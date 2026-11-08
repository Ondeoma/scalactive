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
      TemplateCompletion.complete(text, index.offset(pos), path, index).map(items => new CompletionList(false, items.asJava))
    } catch {
      case NonFatal(e) =>
        log(s"template completion failed: $e")
        None
    }
  }

  def completion(uri: String, pos: Position): CompletionList = {
    scalaCompletion(uri, pos).orElse(templateCompletion(uri, pos))
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
