package io.github.ondeoma.scalactive.ls

import org.eclipse.lsp4j.Location

import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern
import scala.collection.JavaConverters._
import scala.meta.pc.{ParentSymbols, SymbolDocumentation, SymbolSearch, SymbolSearchVisitor}
import scala.util.control.NonFatal

/**
 * presentation compilerから呼ばれる、ファイル外のシンボルの検索.
 *
 *  - 定義ジャンプ: プロジェクトのソースディレクトリから、パッケージに対応するディレクトリの .scala / .st を探し、
 *    なければクラスパスのjarのソース(LibrarySources)から探して、名前の定義らしい箇所を返します.
 *  - ドキュメント: 見つけた定義の直前の `/** */` をMarkdownにして返します(ホバー・補完の説明).
 *  - 自動importの候補: クラスパスの索引(ClasspathIndex)から、名前が合うクラスを返します.
 */
final class SourceSymbolSearch(config: ProjectConfig, index: ClasspathIndex) extends SymbolSearch {

  def this(config: ProjectConfig) = this(config, new ClasspathIndex(config.classpath))

  import SourceSymbolSearch._

  private val docs = new ConcurrentHashMap[String, Optional[SymbolDocumentation]]()

  override def documentation(symbol: String, parents: ParentSymbols): Optional[SymbolDocumentation] = {
    docs.computeIfAbsent(symbol, s => {
      try {
        val own = docstringOf(s)
        val found = own.orElse(Option(parents).iterator.flatMap(_.parents().asScala).map(docstringOf).collectFirst { case Some(d) => d })
        found.map(d => Doc(s, parse(s).map(_._2.last).getOrElse(s), d): SymbolDocumentation) match {
          case Some(d) => Optional.of(d)
          case None => Optional.empty[SymbolDocumentation]()
        }
      } catch {
        case NonFatal(_) => Optional.empty[SymbolDocumentation]()
      }
    })
  }

  private def docstringOf(symbol: String): Option[String] = {
    locate(symbol).flatMap(f => DocComments.before(f.text, f.offset))
  }

  override def definitionSourceToplevels(symbol: String, sourceUri: URI): java.util.List[String] = java.util.Collections.emptyList()

  override def search(query: String, buildTargetIdentifier: String, visitor: SymbolSearchVisitor): SymbolSearch.Result = {
    try index.search(query, visitor)
    catch {
      case NonFatal(_) => SymbolSearch.Result.COMPLETE
    }
  }

  override def searchMethods(query: String, buildTargetIdentifier: String, visitor: SymbolSearchVisitor): SymbolSearch.Result =
    SymbolSearch.Result.COMPLETE

  override def definition(symbol: String, source: URI): java.util.List[Location] = {
    try locate(symbol).map(_.location).toList.asJava
    catch {
      case NonFatal(_) => java.util.Collections.emptyList()
    }
  }

  /** プロジェクトのソース、なければライブラリのソースから定義を探します */
  def locate(symbol: String): Option[Found] = {
    find(config.sourceDirectories, symbol).orElse(locateInLibrary(symbol))
  }

  private def locateInLibrary(symbol: String): Option[Found] = {
    parse(symbol).flatMap { case (pkg, names) =>
      index.entryOf(pkg.map(_ + "/").mkString, names.head)
        .flatMap(LibrarySources.sourceRoot)
        .flatMap(root => find(Seq(root), symbol, walk = true))
    }
  }

}

object SourceSymbolSearch {

  /** 見つけた定義. offsetは名前の位置 */
  final case class Found(file: Path, text: String, offset: Int, length: Int) {
    def location: Location = {
      val index = new LineIndex(text)
      new Location(file.toUri.toString, index.range(offset, offset + length))
    }
  }

  private final case class Doc(symbol: String, displayName: String, docstring: String) extends SymbolDocumentation {
    override def defaultValue(): String = ""
    override def typeParameters(): java.util.List[SymbolDocumentation] = java.util.Collections.emptyList()
    override def parameters(): java.util.List[SymbolDocumentation] = java.util.Collections.emptyList()
  }

  /**
   * SemanticDB形式のシンボル(例: `a/b/Foo.`, `a/b/Foo#bar().`, `a/b/Foo.Props#title.`)を、
   * パッケージのパスと、トップレベルから順の名前に分けます.
   */
  def parse(symbol: String): Option[(List[String], List[String])] = {
    if (symbol.startsWith("local") || !symbol.contains("/")) None
    else {
      val slash = symbol.lastIndexOf('/')
      val pkg = symbol.substring(0, slash).split('/').toList.filter(_.nonEmpty)
      val rest = symbol.substring(slash + 1)
        .replaceAll("""\([^)]*\)""", "")
        .replaceAll("""\[[^\]]*\]""", "")
      val names = rest.split("[.#]").toList.map(_.stripPrefix("`").stripSuffix("`")).filter(_.nonEmpty)
      if (names.isEmpty) None else Some((pkg, names))
    }
  }

  private val Extensions = List(".st", ".scala", ".java")

  /**
   * ソースディレクトリから定義を探します. トップレベルと同名のファイル(トップレベルの定義 `Foo$package` は Foo.scala)を優先し、
   * なければ同じパッケージの他のファイル、walkがtrueならディレクトリ全体から同名のファイルも探します.
   */
  def find(sourceDirectories: Seq[Path], symbol: String, walk: Boolean = false): Option[Found] = {
    parse(symbol).flatMap { case (pkg, names) =>
      val top = names.head.stripSuffix("$package")
      val target = names.last
      val dirs = sourceDirectories.map(d => pkg.foldLeft(d)(_ resolve _)).filter(Files.isDirectory(_))
      val preferred = dirs.flatMap(d => Extensions.map(e => d.resolve(top + e))).filter(Files.isRegularFile(_))
      lazy val others = dirs.flatMap { d =>
        val s = Files.list(d)
        try s.iterator().asScala.filter(f => (f.toString.endsWith(".scala") || f.toString.endsWith(".java")) && Files.isRegularFile(f))
          .toList.sortBy(_.toString)
        finally s.close()
      }.filterNot(preferred.contains)
      lazy val walked =
        if (!walk) Nil
        else sourceDirectories.filter(Files.isDirectory(_)).flatMap { d =>
          val s = Files.walk(d)
          try s.iterator().asScala.filter(f => Extensions.exists(e => f.getFileName.toString == top + e)).toList
          finally s.close()
        }.filterNot(f => preferred.contains(f) || others.contains(f))
      (preferred.iterator ++ others.iterator ++ walked.iterator).flatMap { f =>
        val text = new String(Files.readAllBytes(f), StandardCharsets.UTF_8)
        val isSt = f.toString.endsWith(".st")
        val found = definitionOffset(text, target, f.toString.endsWith(".java")).map(o => Found(f, text, o, target.length)).orElse {
          // .stのobject自体は、ファイルの先頭を定義とする
          if (isSt && names.size == 1) Some(Found(f, text, 0, 0)) else None
        }
        found
      }.toStream.headOption
    }
  }

  private def definitionOffset(text: String, name: String, java: Boolean): Option[Int] = {
    val q = Pattern.quote(name)
    val patterns =
      if (java) List(
        Pattern.compile(s"""\\b(?:class|interface|enum|record|@interface)\\s+($q)\\b"""),
        // メソッド・フィールド(修飾子と型の後ろの名前)
        Pattern.compile(s"""(?m)^\\s*(?:@\\w+\\s+)*(?:(?:public|protected|private|static|final|abstract|default|synchronized|native|transient|volatile)\\s+)*[\\w<>\\[\\]?,. ]+\\s+($q)\\s*[(=;]""")
      )
      else List(
        Pattern.compile(s"""\\b(?:class|object|trait|enum|type|def|val|var|given)\\s+($q)\\b"""),
        // case classの引数など(`name: Type`)
        Pattern.compile(s"""[(,]\\s*(?:(?:val|var)\\s+)?($q)\\s*:""")
      )
    patterns.iterator.map(_.matcher(text)).collectFirst { case m if m.find() => m.start(1) }
  }

}
