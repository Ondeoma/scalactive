package io.github.ondeoma.scalactive.ls

import org.eclipse.lsp4j._
import org.eclipse.lsp4j.jsonrpc.messages.{Either => LspEither}
import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services._

import java.io.PrintStream
import java.util.concurrent._
import scala.collection.JavaConverters._
import scala.util.control.NonFatal

/**
 * LSPの通信部分. 処理はStLanguageServiceに委譲します.
 */
final class ScalactiveLanguageServer extends LanguageServer with LanguageClientAware {

  @volatile private var client: Option[LanguageClient] = None

  private def log(msg: String): Unit = {
    System.err.println(s"[scalactive] $msg")
    client.foreach(_.logMessage(new MessageParams(MessageType.Log, msg)))
  }

  private val service = new StLanguageService(log)

  // プロジェクトのScalactiveが古い場合などは、ログだけでなく画面にも出す
  service.warn = { msg =>
    log(msg)
    client.foreach(_.showMessage(new MessageParams(MessageType.Warning, msg)))
  }

  private val requests = Executors.newCachedThreadPool((r: Runnable) => {
    val t = new Thread(r, "scalactive-ls")
    t.setDaemon(true)
    t
  })
  private val debounce = Executors.newSingleThreadScheduledExecutor((r: Runnable) => {
    val t = new Thread(r, "scalactive-diagnostics")
    t.setDaemon(true)
    t
  })
  private val pendingDiagnostics = new ConcurrentHashMap[String, ScheduledFuture[_]]()

  override def connect(c: LanguageClient): Unit = client = Some(c)

  private def async[A](default: => A)(f: => A): CompletableFuture[A] = {
    CompletableFuture.supplyAsync(() => {
      try f
      catch {
        case NonFatal(e) =>
          log(s"request failed: $e\n${e.getStackTrace.take(20).mkString("\n")}")
          default
      }
    }, requests)
  }

  /** 入力が落ち着いてから診断を送ります. */
  private def scheduleDiagnostics(uri: String, delayMillis: Long): Unit = {
    val f = debounce.schedule(new Runnable {
      override def run(): Unit = requests.execute(() => publishDiagnostics(uri))
    }, delayMillis, TimeUnit.MILLISECONDS)
    Option(pendingDiagnostics.put(uri, f)).foreach(_.cancel(false))
  }

  private def publishDiagnostics(uri: String): Unit = {
    try {
      val ds = service.diagnostics(uri)
      client.foreach(_.publishDiagnostics(new PublishDiagnosticsParams(uri, ds.asJava)))
    } catch {
      case NonFatal(e) => log(s"diagnostics failed for $uri: $e")
    }
  }

  override def initialize(params: InitializeParams): CompletableFuture[InitializeResult] = {
    // IntelliJはテンプレートをHTMLとして扱い、HTMLの補完を自分で出すため {"htmlCompletion": false} を渡す.
    // また、定義の上での定義ジャンプで使っている場所を出すため {"usagesOnDeclaration": true} を渡す
    params.getInitializationOptions match {
      case o: com.google.gson.JsonObject =>
        def bool(name: String): Option[Boolean] =
          Option(o.get(name)).filter(_.isJsonPrimitive).map(_.getAsBoolean)
        bool("htmlCompletion").foreach(service.htmlCompletion = _)
        bool("usagesOnDeclaration").foreach(service.usagesOnDeclaration = _)
      case _ =>
    }
    val caps = new ServerCapabilities()
    caps.setTextDocumentSync(TextDocumentSyncKind.Full)
    // "." はScalaの部分、"<" "@" "#" "\"" はテンプレートの部分(タグ名・イベント・スロット名・<slot name>)の補完のきっかけ
    caps.setCompletionProvider(new CompletionOptions(true, List(".", "<", "@", "#", "\"").asJava))
    caps.setHoverProvider(true)
    caps.setDefinitionProvider(true)
    caps.setDocumentHighlightProvider(true)
    caps.setDocumentFormattingProvider(true)
    caps.setReferencesProvider(true)
    caps.setInlayHintProvider(true)
    caps.setExecuteCommandProvider(new ExecuteCommandOptions(java.util.Collections.singletonList(service.ExtractComponentCommand)))
    caps.setCodeActionProvider(new CodeActionOptions(java.util.Arrays.asList(CodeActionKind.QuickFix, CodeActionKind.RefactorExtract)))
    caps.setRenameProvider(LspEither.forRight[java.lang.Boolean, RenameOptions](new RenameOptions(true)))
    caps.setSignatureHelpProvider(new SignatureHelpOptions(List("(", ",").asJava))
    CompletableFuture.completedFuture(new InitializeResult(caps, new ServerInfo("scalactive-language-server", BuildInfo.version)))
  }

  override def shutdown(): CompletableFuture[AnyRef] = {
    service.shutdown()
    CompletableFuture.completedFuture(null)
  }

  override def exit(): Unit = System.exit(0)

  override val getTextDocumentService: TextDocumentService = new TextDocumentService {

    override def didOpen(params: DidOpenTextDocumentParams): Unit = {
      service.open(params.getTextDocument.getUri, params.getTextDocument.getText)
      scheduleDiagnostics(params.getTextDocument.getUri, 0)
    }

    override def didChange(params: DidChangeTextDocumentParams): Unit = {
      // 全文同期(TextDocumentSyncKind.Full)なので、最後の変更が全文です
      params.getContentChanges.asScala.lastOption.foreach { c =>
        service.change(params.getTextDocument.getUri, c.getText)
        scheduleDiagnostics(params.getTextDocument.getUri, 300)
      }
    }

    override def didClose(params: DidCloseTextDocumentParams): Unit = {
      val uri = params.getTextDocument.getUri
      Option(pendingDiagnostics.remove(uri)).foreach(_.cancel(false))
      service.close(uri)
      client.foreach(_.publishDiagnostics(new PublishDiagnosticsParams(uri, java.util.Collections.emptyList())))
    }

    override def didSave(params: DidSaveTextDocumentParams): Unit = scheduleDiagnostics(params.getTextDocument.getUri, 0)

    override def completion(params: CompletionParams): CompletableFuture[LspEither[java.util.List[CompletionItem], CompletionList]] = {
      async(LspEither.forRight[java.util.List[CompletionItem], CompletionList](new CompletionList())) {
        LspEither.forRight(service.completion(params.getTextDocument.getUri, params.getPosition))
      }
    }

    override def resolveCompletionItem(item: CompletionItem): CompletableFuture[CompletionItem] = {
      async(item)(service.resolveCompletionItem(item))
    }

    override def hover(params: HoverParams): CompletableFuture[Hover] = {
      async(null: Hover)(service.hover(params.getTextDocument.getUri, params.getPosition).orNull)
    }

    override def signatureHelp(params: SignatureHelpParams): CompletableFuture[SignatureHelp] = {
      async(null: SignatureHelp)(service.signatureHelp(params.getTextDocument.getUri, params.getPosition).orNull)
    }

    override def definition(params: DefinitionParams)
    : CompletableFuture[LspEither[java.util.List[_ <: Location], java.util.List[_ <: LocationLink]]] = {
      type R = LspEither[java.util.List[_ <: Location], java.util.List[_ <: LocationLink]]
      async[R](LspEither.forLeft(java.util.Collections.emptyList[Location]())) {
        LspEither.forLeft(service.definition(params.getTextDocument.getUri, params.getPosition).asJava)
      }
    }

    override def formatting(params: DocumentFormattingParams): CompletableFuture[java.util.List[_ <: TextEdit]] = {
      async[java.util.List[_ <: TextEdit]](java.util.Collections.emptyList()) {
        service.formatting(params.getTextDocument.getUri).asJava
      }
    }

    override def references(params: ReferenceParams): CompletableFuture[java.util.List[_ <: Location]] = {
      async[java.util.List[_ <: Location]](java.util.Collections.emptyList()) {
        val include = Option(params.getContext).forall(_.isIncludeDeclaration)
        service.references(params.getTextDocument.getUri, params.getPosition, include).asJava
      }
    }

    override def prepareRename(params: PrepareRenameParams)
    : CompletableFuture[org.eclipse.lsp4j.jsonrpc.messages.Either3[Range, PrepareRenameResult, PrepareRenameDefaultBehavior]] = {
      type R = org.eclipse.lsp4j.jsonrpc.messages.Either3[Range, PrepareRenameResult, PrepareRenameDefaultBehavior]
      async[R](null) {
        service.prepareRename(params.getTextDocument.getUri, params.getPosition).map { case (r, name) =>
          org.eclipse.lsp4j.jsonrpc.messages.Either3.forSecond[Range, PrepareRenameResult, PrepareRenameDefaultBehavior](new PrepareRenameResult(r, name))
        }.orNull
      }
    }

    override def rename(params: RenameParams): CompletableFuture[WorkspaceEdit] = {
      CompletableFuture.supplyAsync(() => {
        service.rename(params.getTextDocument.getUri, params.getPosition, params.getNewName) match {
          case Right(changes) => new WorkspaceEdit(changes.map { case (u, es) => u -> es.asJava }.asJava)
          case Left(msg) =>
            throw new org.eclipse.lsp4j.jsonrpc.ResponseErrorException(
              new org.eclipse.lsp4j.jsonrpc.messages.ResponseError(org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode.InvalidParams, msg, null))
        }
      }, requests)
    }

    override def inlayHint(params: InlayHintParams): CompletableFuture[java.util.List[InlayHint]] = {
      async[java.util.List[InlayHint]](java.util.Collections.emptyList()) {
        service.inlayHints(params.getTextDocument.getUri, params.getRange).asJava
      }
    }

    override def codeAction(params: CodeActionParams): CompletableFuture[java.util.List[LspEither[Command, CodeAction]]] = {
      async[java.util.List[LspEither[Command, CodeAction]]](java.util.Collections.emptyList()) {
        val diagnostics = Option(params.getContext).flatMap(c => Option(c.getDiagnostics)).map(_.asScala.toList).getOrElse(Nil)
        (service.codeActions(params.getTextDocument.getUri, params.getRange, diagnostics) ++
          service.extractComponentAction(params.getTextDocument.getUri, params.getRange))
          .map(a => LspEither.forRight[Command, CodeAction](a)).asJava
      }
    }

    override def documentHighlight(params: DocumentHighlightParams): CompletableFuture[java.util.List[_ <: DocumentHighlight]] = {
      async[java.util.List[_ <: DocumentHighlight]](java.util.Collections.emptyList()) {
        service.documentHighlight(params.getTextDocument.getUri, params.getPosition).asJava
      }
    }

  }

  override val getWorkspaceService: WorkspaceService = new WorkspaceService {
    override def didChangeConfiguration(params: DidChangeConfigurationParams): Unit = ()

    /** コンポーネントの抽出: 新しい.stをディスクに書き、元のファイルの選んだ範囲をタグに置き換える編集をクライアントに頼む */
    override def executeCommand(params: ExecuteCommandParams): CompletableFuture[AnyRef] = {
      CompletableFuture.supplyAsync(() => {
        if (params.getCommand != service.ExtractComponentCommand) null
        else {
          def arg(i: Int): String = params.getArguments.get(i) match {
            case j: com.google.gson.JsonPrimitive => j.getAsString
            case other => String.valueOf(other)
          }
          val uri = arg(0)
          def num(i: Int) = arg(i).toDouble.toInt
          val range = new Range(new Position(num(1), num(2)), new Position(num(3), num(4)))
          service.extractComponent(uri, range) match {
            case Right(r) =>
              java.nio.file.Files.write(r.file, r.content.getBytes(java.nio.charset.StandardCharsets.UTF_8))
              val text = service.currentText(uri)
              val index = new LineIndex(text)
              val edit = new WorkspaceEdit(java.util.Collections.singletonMap(uri,
                java.util.Collections.singletonList(new TextEdit(index.range(r.replaceStart, r.replaceEnd), r.replacement))))
              client.foreach { c =>
                c.applyEdit(new ApplyWorkspaceEditParams(edit, "Extract component"))
                // 作ったファイルを開く(対応していないクライアントでは何もしない)
                try c.showDocument(new ShowDocumentParams(r.file.toUri.toString)) catch {
                  case NonFatal(_) =>
                }
              }
              r.file.toUri.toString
            case Left(msg) =>
              client.foreach(_.showMessage(new MessageParams(MessageType.Warning, msg)))
              null
          }
        }
      }, requests)
    }

    override def didChangeWatchedFiles(params: DidChangeWatchedFilesParams): Unit = ()
  }

}

object Main {

  def main(args: Array[String]): Unit = {
    if (args.contains("--version")) {
      println(BuildInfo.version)
      return
    }
    val in = System.in
    val out = System.out
    // 標準出力はLSPの通信に使うため、それ以外の出力は標準エラーへ
    System.setOut(new PrintStream(System.err, true))
    val server = new ScalactiveLanguageServer
    val launcher = LSPLauncher.createServerLauncher(server, in, out)
    server.connect(launcher.getRemoteProxy)
    launcher.startListening().get()
  }

}
