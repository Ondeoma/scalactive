package io.github.ondeoma.scalactive.ls

import io.github.ondeoma.scalactive.compiler.{StCompiler, StError, StOutput, StSources}
import org.eclipse.lsp4j._
import org.eclipse.lsp4j.jsonrpc.messages.{Either => LspEither}

import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}
import java.util.concurrent._
import scala.collection.JavaConverters._
import scala.meta.pc.PresentationCompiler
import scala.util.control.NonFatal

/**
 * .stファイルに対する言語機能の本体(LSPの通信からは独立しています).
 *
 * .stを生成コードに変換し、生成コード上でpresentation compilerに問い合わせ、
 * 結果の位置をSourceMapで.st上の位置に戻します.
 */
final class StLanguageService(log: String => Unit) {

  private val scriptFormatter = new ScriptFormatter(log)

  /** テンプレートでHTMLの標準のタグ・属性・属性値も補完するか(initializationOptionsの htmlCompletion) */
  @volatile var htmlCompletion: Boolean = true

  import StLanguageService._

  private val threadFactory: ThreadFactory = (r: Runnable) => {
    val t = new Thread(r, "scalactive-pc")
    t.setDaemon(true)
    t
  }
  private val executor = Executors.newCachedThreadPool(threadFactory)
  private val scheduler = Executors.newSingleThreadScheduledExecutor(threadFactory)

  /** エディタで開いているファイルの内容(URI → テキスト) */
  private val texts = new ConcurrentHashMap[String, String]()

  private final class Project(val config: ProjectConfig,
                              val configModified: Long,
                              val pc: PresentationCompiler,
                              @volatile var compiler: InteractiveCompiler,
                              var stamp: Long) {

    def newCompiler(): InteractiveCompiler = {
      new InteractiveCompiler(PcLoader.classLoader(config.scalaVersion), config.classpath.map(_.toString), config.compilerOptions)
    }

  }

  /** 設定ファイル → プロジェクト */
  private val projects = new ConcurrentHashMap[Path, Either[String, Project]]()

  def open(uri: String, text: String): Unit = texts.put(uri, text)

  def change(uri: String, text: String): Unit = texts.put(uri, text)

  def close(uri: String): Unit = {
    texts.remove(uri)
    prepare(uri).right.foreach(d => d.pc.didClose(d.genUri))
  }

  def shutdown(): Unit = {
    projects.values().asScala.foreach(_.right.foreach(p => p.pc.shutdown()))
    projects.clear()
    executor.shutdownNow()
    scheduler.shutdownNow()
  }

  // ---- 準備 ----

  private def textOf(uri: String, path: Path): String = {
    Option(texts.get(uri)).getOrElse(new String(Files.readAllBytes(path), StandardCharsets.UTF_8))
  }

  private def project(configFile: Path): Either[String, Project] = {
    val modified = Files.getLastModifiedTime(configFile).toMillis
    val result = projects.compute(configFile, (_, old) => old match {
      case null => newProject(configFile, modified)
      case Right(p) if p.configModified != modified =>
        p.pc.shutdown()
        newProject(configFile, modified)
      case Left(_) => newProject(configFile, modified)
      case p => p
    })
    result.right.foreach { p =>
      // sbtが再コンパイルしたら、クラスディレクトリを読み直す
      val stamp = stampOf(p.config)
      if (stamp != p.stamp) {
        p.stamp = stamp
        p.pc.restart()
        p.compiler = p.newCompiler()
      }
    }
    result
  }

  private def stampOf(config: ProjectConfig): Long = {
    val f = config.stampFile
    if (Files.exists(f)) Files.getLastModifiedTime(f).toMillis else 0L
  }

  private def newProject(configFile: Path, modified: Long): Either[String, Project] = {
    ProjectConfig.load(configFile).right.flatMap { config =>
      try {
        log(s"starting Scala ${config.scalaVersion} presentation compiler for ${config.file}")
        val pc = PcLoader.load(config.scalaVersion)
          .withSearch(new SourceSymbolSearch(config))
          .withExecutorService(executor)
          .withScheduledExecutorService(scheduler)
          .newInstance("scalactive", config.classpath.asJava, config.compilerOptions.asJava)
        val p = new Project(config, modified, pc, null, stampOf(config))
        p.compiler = p.newCompiler()
        Right(p)
      } catch {
        case NonFatal(e) =>
          log(s"failed to start the presentation compiler: $e")
          Left(s"Failed to start the Scala ${config.scalaVersion} presentation compiler: $e")
      }
    }
  }

  /** .stをコンパイルし、presentation compilerに問い合わせる準備をします. */
  def prepare(uri: String): Either[Problem, Doc] = {
    val path = try Paths.get(new URI(uri)).toAbsolutePath.normalize catch {
      case NonFatal(_) => return Left(NoProject(s"unsupported URI: $uri"))
    }
    val text = try textOf(uri, path) catch {
      case NonFatal(e) => return Left(NoProject(s"cannot read $uri: $e"))
    }
    val index = new LineIndex(text)
    ProjectConfig.find(path) match {
      case None =>
        Left(NoProject(
          s"${ProjectConfig.FileName} was not found. Enable ScalactivePlugin in the sbt project and run 'sbt compile' " +
            "to enable completion and type checking."
        ))
      case Some(configFile) =>
        project(configFile) match {
          case Left(msg) => Left(PcFailure(msg))
          case Right(p) =>
            val root = StSources.rootOf(p.config.sourceRoots, path).get
            StCompiler.compile(StSources.input(root, path, text)) match {
              case Left(e) => Left(StFailure(e, index))
              case Right(out) =>
                val genUri = StSources.outputFile(p.config.generatedDirectory, out).toUri
                Right(Doc(uri, text, index, out, genUri, new LineIndex(out.code), p.pc, p.compiler))
            }
        }
    }
  }

  // ---- 位置の変換 ----

  private def toSt(d: Doc, r: Range): Option[Range] = {
    d.out.sourceMap.rangeToSt(d.genIndex.offset(r.getStart), d.genIndex.offset(r.getEnd)).map {
      case (s, e) => d.index.range(s, e)
    }
  }

  private def isGenerated(d: Doc, uri: String): Boolean = {
    try Paths.get(new URI(uri)).toAbsolutePath.normalize == Paths.get(d.genUri).toAbsolutePath.normalize
    catch {
      case NonFatal(_) => false
    }
  }

  /** カーソル位置を生成コード上の位置に変換して問い合わせます. ユーザーのコード以外の位置ではNone. */
  private def at[A](uri: String, pos: Position)(f: (Doc, PositionParams) => A): Option[A] = {
    prepare(uri).right.toOption.flatMap { d =>
      d.out.sourceMap.cursorToGen(d.index.offset(pos)).map { gen =>
        f(d, PositionParams(d.genUri, d.out.code, gen))
      }
    }
  }

  private def await[A](f: CompletableFuture[A]): A = f.get(Timeout, TimeUnit.SECONDS)

  // ---- 言語機能 ----

  def diagnostics(uri: String): List[Diagnostic] = {
    val base = compilerDiagnostics(uri)
    // 子コンポーネントのpropsの検査. 型検査が同じ位置で同じpropについて報告していれば、そちらを残す
    base ++ propsDiagnostics(uri).filterNot { case (d, names) =>
      base.exists(b => b.getRange.getStart == d.getRange.getStart && names.exists(n => b.getMessage.contains(n)))
    }.map(_._1) ++ cssDiagnostics(uri)
  }

  /** <style scoped> で定義したのに使われていないクラス(エディタでは薄く表示されます) */
  private def cssDiagnostics(uri: String): List[Diagnostic] = {
    try {
      val text = textOf(uri, pathOf(uri))
      val index = new LineIndex(text)
      CssClasses.unused(text).map { d =>
        val diagnostic = new Diagnostic(index.range(d.offset - 1, d.offset + d.name.length),
          s"CSS class '${d.name}' is not used in the template", DiagnosticSeverity.Hint, Source)
        diagnostic.setTags(java.util.Collections.singletonList(DiagnosticTag.Unnecessary))
        diagnostic
      }
    } catch {
      case NonFatal(_) => Nil
    }
  }

  private def propsDiagnostics(uri: String): List[(Diagnostic, List[String])] = {
    try {
      val path = Paths.get(new URI(uri)).toAbsolutePath.normalize
      val text = textOf(uri, path)
      val index = new LineIndex(text)
      PropsCheck.check(text, path).map { p =>
        (new Diagnostic(index.range(p.start, p.end), p.message, DiagnosticSeverity.Warning, Source), p.names)
      }
    } catch {
      case NonFatal(_) => Nil
    }
  }

  /**
   * 推論した型のインレイヒント(scriptの型を書いていない val / def、v-forの変数、ラムダの引数など).
   * 生成コード全体について問い合わせ、ユーザーが書いたコードの位置に対応するものだけを、rangeの中に絞って返します.
   * (生成コードを指す位置やテキストの編集は含めません)
   */
  def inlayHints(uri: String, range: Range): List[InlayHint] = {
    prepare(uri) match {
      case Right(d) =>
        val (from, to) = (d.index.offset(range.getStart), d.index.offset(range.getEnd))
        val hints = await(d.pc.inlayHints(HintParams(d.genUri, d.out.code, 0, d.out.code.length))).asScala.toList
        val fromPc = hints.flatMap { h =>
          d.out.sourceMap.cursorToSt(d.genIndex.offset(h.getPosition)).filter(o => o >= from && o <= to).map { st =>
            val label =
              if (h.getLabel.isLeft) h.getLabel.getLeft
              else h.getLabel.getRight.asScala.map(_.getValue).mkString
            val hint = new InlayHint(d.index.position(st), LspEither.forLeft[String, java.util.List[InlayHintLabelPart]](label))
            hint.setKind(h.getKind)
            hint.setPaddingLeft(h.getPaddingLeft)
            hint.setPaddingRight(h.getPaddingRight)
            hint
          }
        }
        (fromPc ++ templateVariableHints(d, from, to))
          .groupBy(h => (h.getPosition.getLine, h.getPosition.getCharacter)).values.map(_.head).toList
          .sortBy(h => (h.getPosition.getLine, h.getPosition.getCharacter))
      case Left(_) => Nil
    }
  }

  /**
   * テンプレートで導入する変数(v-for の変数、スコープ付きスロットの #name="(a, b)" の変数)の型のヒント.
   * 生成コードではラムダの引数になり、presentation compilerがヒントを出さないため、変数の位置のホバーから型を求めます.
   */
  private def templateVariableHints(d: Doc, from: Int, to: Int): List[InlayHint] = {
    import io.github.ondeoma.scalactive.compiler.{Element, HtmlNode, HtmlParser, SfcParser, SourceText}
    val Ident = "[A-Za-z_][A-Za-z0-9_]*".r
    val src = new SourceText(d.text)
    val vars: List[(String, Int)] = try {
      SfcParser.parse(src).template.toList.flatMap { t =>
        def walk(ns: List[HtmlNode]): List[(String, Int)] = ns.flatMap {
          case e: Element =>
            val own = e.attrs.flatMap { a =>
              a.value.toList.flatMap { v =>
                val pattern =
                  if (a.name == "v-for") Some(v.indexOf(" in ")).filter(_ >= 0).map(v.substring(0, _))
                  else if (a.name.startsWith("#") || a.name.startsWith("v-slot")) Some(v)
                  else None
                pattern.toList.flatMap { p =>
                  Ident.findAllMatchIn(p).toList.collect {
                    // 型を書いた変数(x: Int)と _ は除く
                    case m if m.matched != "_" && !p.substring(m.end).trim.startsWith(":") => (m.matched, a.valueOffset + m.start)
                  }
                }
              }
            }
            own ++ walk(e.children)
          case _ => Nil
        }
        walk(HtmlParser.parse(src, t.contentStart, t.contentEnd))
      }
    } catch {
      case NonFatal(_) => Nil
    }
    vars.filter { case (name, at) => at + name.length >= from && at <= to }.flatMap { case (name, at) =>
      d.out.sourceMap.toGen(at).flatMap { gen =>
        val h = try await(d.pc.hover(PositionParams(d.genUri, d.out.code, gen + 1))) catch {
          case NonFatal(_) => java.util.Optional.empty[scala.meta.pc.HoverSignature]()
        }
        val signature = if (h.isPresent) Option(h.get.signature().orElse(null)) else None
        signature.flatMap { sig =>
          val i = sig.indexOf(": ")
          if (i < 0) None
          else {
            val hint = new InlayHint(d.index.position(at + name.length),
              LspEither.forLeft[String, java.util.List[InlayHintLabelPart]](": " + sig.substring(i + 2).trim))
            hint.setKind(InlayHintKind.Type)
            Some(hint)
          }
        }
      }
    }
  }

  /** 開いているファイルの現在の内容(開いていなければディスクの内容) */
  def currentText(uri: String): String = textOf(uri, pathOf(uri))

  /** コンポーネントの抽出のコマンド名 */
  val ExtractComponentCommand = "scalactive.extractComponent"

  /** テンプレートで完結したHTMLを選んでいれば、「コンポーネントの抽出」のコードアクション(実行はコマンドで行う) */
  def extractComponentAction(uri: String, range: Range): Option[CodeAction] = {
    try {
      val text = textOf(uri, pathOf(uri))
      val index = new LineIndex(text)
      ComponentExtractor.selection(text, index.offset(range.getStart), index.offset(range.getEnd)).map { _ =>
        val action = new CodeAction("Extract component")
        action.setKind(CodeActionKind.RefactorExtract)
        action.setCommand(new Command("Extract component", ExtractComponentCommand, java.util.Arrays.asList[AnyRef](
          uri, Int.box(range.getStart.getLine), Int.box(range.getStart.getCharacter), Int.box(range.getEnd.getLine), Int.box(range.getEnd.getCharacter))))
        action
      }
    } catch {
      case NonFatal(_) => None
    }
  }

  /**
   * コンポーネントの抽出. 名前の解決はpresentation compilerに問い合わせます(定義の位置で、範囲の外の値・scriptのメンバー・そのままを分ける).
   */
  def extractComponent(uri: String, range: Range): Either[String, ComponentExtractor.Result] = {
    val path = pathOf(uri)
    val text = textOf(uri, path)
    val index = new LineIndex(text)
    val (start, end) = (index.offset(range.getStart), index.offset(range.getEnd))
    val doc = prepare(uri).right.toOption
    val sfc = io.github.ondeoma.scalactive.compiler.SfcParser.parse(new io.github.ondeoma.scalactive.compiler.SourceText(text))
    val inScript = (o: Int) => sfc.script.exists(b => o >= b.contentStart && o < b.contentEnd)
    val (selStart, selEnd) = ComponentExtractor.selection(text, start, end).getOrElse((start, end))
    def resolve(id: ComponentExtractor.Ident): ComponentExtractor.Resolution = {
      doc.flatMap { d =>
        d.out.sourceMap.toGen(id.offset).map { gen =>
          val params = PositionParams(d.genUri, d.out.code, gen + 1)
          val locations = try await(d.pc.definition(params)).locations().asScala.toList catch {
            case NonFatal(_) => Nil
          }
          def typeOf: String = {
            val h = try await(d.pc.hover(params)) catch {
              case NonFatal(_) => java.util.Optional.empty[scala.meta.pc.HoverSignature]()
            }
            val sig = if (h.isPresent) Option(h.get.signature().orElse(null)) else None
            sig.flatMap(x => Some(x.indexOf(": ")).filter(_ >= 0).map(i => x.substring(i + 2).trim)).getOrElse("Any")
          }
          locations.headOption match {
            case Some(loc) if isGenerated(d, loc.getUri) =>
              d.out.sourceMap.toSt(d.genIndex.offset(loc.getRange.getStart)) match {
                case Some(st) if st >= selStart && st < selEnd => ComponentExtractor.Keep
                case Some(st) if inScript(st) => ComponentExtractor.ScriptMember
                case Some(_) => ComponentExtractor.External(typeOf) // テンプレートの、範囲の外の変数
                case None =>
                  // 生成コードの引数(s・props)
                  if (id.name == "s" || id.name == "props") ComponentExtractor.External(typeOf) else ComponentExtractor.Keep
              }
            case _ => ComponentExtractor.Keep
          }
        }
      }.getOrElse(ComponentExtractor.Keep)
    }
    ComponentExtractor.extract(text, path, start, end, resolve)
  }

  /**
   * クイックフィックス(子コンポーネントのタグの未知のprop・スロットの候補、足りない必須のpropの書き足し).
   * rangeに重なる問題のものを返します.
   */
  def codeActions(uri: String, range: Range): List[CodeAction] = {
    try {
      val path = pathOf(uri)
      val text = textOf(uri, path)
      val index = new LineIndex(text)
      val (from, to) = (index.offset(range.getStart), index.offset(range.getEnd))
      PropsCheck.check(text, path).filter(p => p.start <= to && from <= p.end).flatMap { p =>
        p.fixes.map { f =>
          val action = new CodeAction(f.title)
          action.setKind(CodeActionKind.QuickFix)
          val diagnostic = new Diagnostic(index.range(p.start, p.end), p.message, DiagnosticSeverity.Warning, Source)
          action.setDiagnostics(java.util.Collections.singletonList(diagnostic))
          action.setEdit(new WorkspaceEdit(java.util.Collections.singletonMap(uri,
            java.util.Collections.singletonList(new TextEdit(index.range(f.start, f.end), f.newText)))))
          if (p.fixes.size == 1 || p.fixes.headOption.contains(f)) action.setIsPreferred(true)
          action
        }
      }
    } catch {
      case NonFatal(_) => Nil
    }
  }

  private def compilerDiagnostics(uri: String): List[Diagnostic] = {
    def at0(msg: String, severity: DiagnosticSeverity) = {
      val d = new Diagnostic(new Range(new Position(0, 0), new Position(0, 0)), msg, severity, Source)
      List(d)
    }
    prepare(uri) match {
      case Left(NoProject(msg)) => at0(msg, DiagnosticSeverity.Information)
      case Left(PcFailure(msg)) => at0(msg, DiagnosticSeverity.Warning)
      case Left(StFailure(e, index)) =>
        List(new Diagnostic(index.range(e.offset, e.offset + 1), e.message, DiagnosticSeverity.Error, Source))
      case Right(d) =>
        d.compiler.diagnostics(d.genUri, d.out.code).map { c =>
          val (s, e) = d.out.sourceMap.nearestRangeToSt(c.start, c.end).getOrElse((0, 0))
          val severity = c.level match {
            case 2 => DiagnosticSeverity.Error
            case 1 => DiagnosticSeverity.Warning
            case _ => DiagnosticSeverity.Information
          }
          new Diagnostic(d.index.range(s, e), c.message, severity, Source)
        }
    }
  }

  /**
   * テンプレートのHTMLの部分(タグ名・属性名・スロット名)の補完. コンパイルできない書きかけの.stでも動きます.
   */
  private def templateCompletion(uri: String, pos: Position): Option[CompletionList] = {
    try {
      val path = Paths.get(new URI(uri)).toAbsolutePath.normalize
      val text = textOf(uri, path)
      val index = new LineIndex(text)
      TemplateCompletion.complete(text, index.offset(pos), path, index, htmlCompletion).map(items => new CompletionList(false, items.asJava))
    } catch {
      case NonFatal(e) =>
        log(s"template completion failed: $e")
        None
    }
  }

  def completion(uri: String, pos: Position): CompletionList = {
    // タグ名・属性名ではテンプレートの補完を優先する(コンパイルできる.stでは、子コンポーネントのタグ名が
    // 生成コードのScalaの式に対応付けられているため、先にScalaの補完を試すとScalaの候補になってしまう)
    templateCompletion(uri, pos).orElse(scalaCompletion(uri, pos))
      .getOrElse(new CompletionList(false, java.util.Collections.emptyList()))
  }

  private def scalaCompletion(uri: String, pos: Position): Option[CompletionList] = {
    at(uri, pos) { (d, params) =>
      val list = await(d.pc.complete(params))
      list.getItems.asScala.foreach { item =>
        Option(item.getTextEdit).foreach { te =>
          val mapped: Option[LspEither[TextEdit, InsertReplaceEdit]] =
            if (te.isLeft) toSt(d, te.getLeft.getRange).map(r => LspEither.forLeft(new TextEdit(r, te.getLeft.getNewText)))
            else for {
              ins <- toSt(d, te.getRight.getInsert)
              rep <- toSt(d, te.getRight.getReplace)
            } yield LspEither.forRight(new InsertReplaceEdit(te.getRight.getNewText, ins, rep))
          mapped match {
            case Some(m) => item.setTextEdit(m)
            case None =>
              item.setTextEdit(null)
              item.setInsertText(if (te.isLeft) te.getLeft.getNewText else te.getRight.getNewText)
          }
        }
        Option(item.getAdditionalTextEdits).foreach { edits =>
          item.setAdditionalTextEdits(edits.asScala.flatMap(e => toSt(d, e.getRange).map(r => new TextEdit(r, e.getNewText))).asJava)
        }
      }
      list
    }
  }

  def hover(uri: String, pos: Position): Option[Hover] = {
    // 子コンポーネントやHTMLのタグ名・属性名ではテンプレートのホバー(Propsの一覧・説明)を優先する
    templateHover(uri, pos).orElse(scalaHover(uri, pos))
  }

  private def templateHover(uri: String, pos: Position): Option[Hover] = {
    try {
      val path = Paths.get(new URI(uri)).toAbsolutePath.normalize
      val text = textOf(uri, path)
      TemplateNavigation.hover(text, new LineIndex(text).offset(pos), path)
    } catch {
      case NonFatal(_) => None
    }
  }

  private def scalaHover(uri: String, pos: Position): Option[Hover] = {
    at(uri, pos) { (d, params) =>
      val h = await(d.pc.hover(params))
      if (h.isPresent) {
        val hover = h.get.toLsp()
        hover.setRange(Option(hover.getRange).flatMap(toSt(d, _)).orNull)
        Some(hover)
      } else None
    }.flatten
  }

  def signatureHelp(uri: String, pos: Position): Option[SignatureHelp] = {
    at(uri, pos)((d, params) => Option(await(d.pc.signatureHelp(params)))).flatten
  }

  def definition(uri: String, pos: Position): List[Location] = {
    templateDefinition(uri, pos).map(List(_)).getOrElse(scalaDefinition(uri, pos))
  }

  /** 子コンポーネントのタグ名・属性名からの定義ジャンプ(コンパイルできない書きかけの.stでも動きます) */
  private def templateDefinition(uri: String, pos: Position): Option[Location] = {
    try {
      val path = Paths.get(new URI(uri)).toAbsolutePath.normalize
      val text = textOf(uri, path)
      TemplateNavigation.definition(text, new LineIndex(text).offset(pos), path)
    } catch {
      case NonFatal(_) => None
    }
  }

  private def scalaDefinition(uri: String, pos: Position): List[Location] = {
    at(uri, pos) { (d, params) =>
      await(d.pc.definition(params)).locations().asScala.toList.flatMap { loc =>
        if (isGenerated(d, loc.getUri)) toSt(d, loc.getRange).map(r => new Location(uri, r))
        else Some(loc)
      }
    }.getOrElse(Nil)
  }

  // ---- 参照の検索・名前の変更 ----

  private def pathOf(uri: String): Path = Paths.get(new URI(uri)).toAbsolutePath.normalize

  /** 開いている.stは編集中の内容、それ以外はファイルの内容 */
  private def fileText(p: Path): String = {
    val uri = p.toUri.toString
    Option(texts.get(uri)).orElse(texts.asScala.collectFirst {
      case (u, t) if scala.util.Try(pathOf(u)).toOption.contains(p) => t
    }).getOrElse(new String(Files.readAllBytes(p), StandardCharsets.UTF_8))
  }

  private def uriOf(p: Path): String = {
    texts.asScala.keys.find(u => scala.util.Try(pathOf(u)).toOption.contains(p)).getOrElse(p.toUri.toString)
  }

  /** 定義した.stの中のScalaの参照(presentation compilerのハイライト) */
  private def scalaRefsOf(t: PropRefs.Target): List[PropRefs.Occurrence] = {
    val text = fileText(t.componentFile)
    val index = new LineIndex(text)
    documentHighlight(uriOf(t.componentFile), index.position(t.declOffset)).map { h =>
      PropRefs.Occurrence(t.componentFile, index.offset(h.getRange.getStart), index.offset(h.getRange.getEnd), PropRefs.Plain)
    }
  }

  private def propOccurrences(t: PropRefs.Target): List[PropRefs.Occurrence] = {
    val all = PropRefs.occurrences(t, fileText) ++ scalaRefsOf(t)
    all.groupBy(o => (o.file, o.start)).values.map(_.head).toList.sortBy(o => (o.file.toString, o.start))
  }

  private def toLocation(o: PropRefs.Occurrence): Location = {
    val index = new LineIndex(fileText(o.file))
    new Location(uriOf(o.file), index.range(o.start, o.end))
  }

  /**
   * 参照の検索. 子コンポーネントのprop(定義・属性・スロット)とコンポーネントのタグは同じパッケージの.stから、
   * それ以外のScalaの名前はこの.stの中から探します.
   */
  def references(uri: String, pos: Position, includeDeclaration: Boolean): List[Location] = {
    try {
      val path = pathOf(uri)
      val text = textOf(uri, path)
      val offset = new LineIndex(text).offset(pos)
      PropRefs.target(text, offset, path, fileText) match {
        case Some(t) =>
          propOccurrences(t).filter(o => includeDeclaration || !(o.file == t.componentFile && o.start == t.declOffset)).map(toLocation)
        case None =>
          val tag = TemplateNavigation.wordAt(text, offset).filter { case (s, _) =>
            (s >= 1 && text.charAt(s - 1) == '<') || (s >= 2 && text.startsWith("</", s - 2))
          }.map { case (s, e) => text.substring(s, e) }
          tag.filter(_.headOption.exists(_.isUpper)).flatMap(c => TemplateNavigation.componentFile(c, path).map(c -> _)) match {
            case Some((c, cf)) => PropRefs.componentOccurrences(c, cf.getParent, fileText).map(toLocation)
            case None => documentHighlight(uri, pos).map(h => new Location(uri, h.getRange))
          }
      }
    } catch {
      case NonFatal(e) =>
        log(s"references failed: $e")
        Nil
    }
  }

  /** 名前を変えられる位置なら、その範囲と今の名前 */
  def prepareRename(uri: String, pos: Position): Option[(Range, String)] = {
    try {
      val path = pathOf(uri)
      val text = textOf(uri, path)
      val index = new LineIndex(text)
      val offset = index.offset(pos)
      for {
        t <- PropRefs.target(text, offset, path, fileText)
        (s, e) <- TemplateNavigation.wordAt(text, offset)
      } yield (index.range(s, e), t.prop)
    } catch {
      case NonFatal(_) => None
    }
  }

  /** 子コンポーネントのpropの名前の変更. 定義・属性・スロット・定義した.stの中のScalaの参照を書き換えます */
  def rename(uri: String, pos: Position, newName: String): Either[String, Map[String, List[TextEdit]]] = {
    if (!newName.matches("[A-Za-z_][A-Za-z0-9_]*")) Left(s"'$newName' is not a valid prop name")
    else {
      val path = pathOf(uri)
      val text = textOf(uri, path)
      PropRefs.target(text, new LineIndex(text).offset(pos), path, fileText) match {
        case None => Left("only props of .st components can be renamed")
        case Some(t) =>
          val occs = propOccurrences(t)
          val edits = occs.map(o => PropRefs.renamed(o.kind, newName).map(n => (o, n)))
          edits.collectFirst { case Left(e) => e } match {
            case Some(e) => Left(e)
            case None =>
              Right(edits.collect { case Right((o, n)) => o -> n }.groupBy(_._1.file).map { case (f, xs) =>
                val index = new LineIndex(fileText(f))
                uriOf(f) -> xs.map { case (o, n) => new TextEdit(index.range(o.start, o.end), n) }
              })
          }
      }
    }
  }

  /** ドキュメントの整形. 変わる場合は全体を置き換える編集を1つ返します. */
  def formatting(uri: String): List[TextEdit] = {
    try {
      val path = Paths.get(new URI(uri)).toAbsolutePath.normalize
      val text = textOf(uri, path)
      // <script>はscalafmtで整形し、できなければ(構文エラーなど)空白の整理だけを行う
      // テンプレートのScalaの部分({{ }}・属性値)もscalafmtで整形する
      val withTemplate = scriptFormatter.formatTemplate(text, path)
      val formatted = scriptFormatter.format(withTemplate, path) match {
        case Some(t) => io.github.ondeoma.scalactive.compiler.StFormatter.format(t, scriptSpacing = false)
        case None => io.github.ondeoma.scalactive.compiler.StFormatter.format(withTemplate)
      }
      if (formatted == text) Nil
      else {
        val index = new LineIndex(text)
        List(new TextEdit(index.range(0, text.length), formatted))
      }
    } catch {
      case NonFatal(e) =>
        log(s"formatting failed: $e")
        Nil
    }
  }

  def documentHighlight(uri: String, pos: Position): List[DocumentHighlight] = {
    at(uri, pos) { (d, params) =>
      await(d.pc.documentHighlight(params)).asScala.toList.flatMap { h =>
        toSt(d, h.getRange).map(r => new DocumentHighlight(r, h.getKind))
      }
    }.getOrElse(Nil)
  }

}

object StLanguageService {

  val Source = "scalactive"

  /** presentation compilerの応答を待つ上限(秒). 初回はクラスパスの読み込みに時間がかかります. */
  private val Timeout = 60L

  /**
   * @param genUri   生成コードのURI(sbtプラグインの出力先と同じ)
   * @param index    .stの行・桁の変換
   * @param genIndex 生成コードの行・桁の変換
   */
  final case class Doc(uri: String,
                       text: String,
                       index: LineIndex,
                       out: StOutput,
                       genUri: URI,
                       genIndex: LineIndex,
                       pc: PresentationCompiler,
                       compiler: InteractiveCompiler)

  sealed trait Problem

  /** 設定ファイルがない(sbtでコンパイルしていない)など */
  final case class NoProject(message: String) extends Problem

  /** presentation compilerを起動できない */
  final case class PcFailure(message: String) extends Problem

  /** .stの構文エラー */
  final case class StFailure(error: StError, index: LineIndex) extends Problem

}
