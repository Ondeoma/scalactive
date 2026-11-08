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
    val caps = new ServerCapabilities()
    caps.setTextDocumentSync(TextDocumentSyncKind.Full)
    // "." はScalaの部分、"<" "@" "#" "\"" はテンプレートの部分(タグ名・イベント・スロット名・<slot name>)の補完のきっかけ
    caps.setCompletionProvider(new CompletionOptions(false, List(".", "<", "@", "#", "\"").asJava))
    caps.setHoverProvider(true)
    caps.setDefinitionProvider(true)
    caps.setDocumentHighlightProvider(true)
    caps.setDocumentFormattingProvider(true)
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

    override def documentHighlight(params: DocumentHighlightParams): CompletableFuture[java.util.List[_ <: DocumentHighlight]] = {
      async[java.util.List[_ <: DocumentHighlight]](java.util.Collections.emptyList()) {
        service.documentHighlight(params.getTextDocument.getUri, params.getPosition).asJava
      }
    }

  }

  override val getWorkspaceService: WorkspaceService = new WorkspaceService {
    override def didChangeConfiguration(params: DidChangeConfigurationParams): Unit = ()

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
