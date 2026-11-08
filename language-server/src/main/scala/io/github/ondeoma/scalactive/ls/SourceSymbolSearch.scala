package io.github.ondeoma.scalactive.ls

import org.eclipse.lsp4j.Location

import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util.Optional
import java.util.regex.Pattern
import scala.collection.JavaConverters._
import scala.meta.pc.{ParentSymbols, SymbolDocumentation, SymbolSearch, SymbolSearchVisitor}

/**
 * presentation compilerから呼ばれる、ファイル外のシンボルの検索.
 *
 * 索引は持たず、定義ジャンプのときだけ、プロジェクトのソースディレクトリから
 * パッケージに対応するディレクトリの .scala / .st を探して、名前の定義らしい箇所を返します.
 * ライブラリ(jar)内のシンボルや、ドキュメント・自動importの候補は提供しません.
 */
final class SourceSymbolSearch(config: ProjectConfig) extends SymbolSearch {

  override def documentation(symbol: String, parents: ParentSymbols): Optional[SymbolDocumentation] = Optional.empty()

  override def definitionSourceToplevels(symbol: String, sourceUri: URI): java.util.List[String] = java.util.Collections.emptyList()

  override def search(query: String, buildTargetIdentifier: String, visitor: SymbolSearchVisitor): SymbolSearch.Result =
    SymbolSearch.Result.COMPLETE

  override def searchMethods(query: String, buildTargetIdentifier: String, visitor: SymbolSearchVisitor): SymbolSearch.Result =
    SymbolSearch.Result.COMPLETE

  override def definition(symbol: String, source: URI): java.util.List[Location] = {
    try SourceSymbolSearch.find(config.sourceDirectories, symbol).toList.asJava
    catch {
      case _: Exception => java.util.Collections.emptyList()
    }
  }

}

object SourceSymbolSearch {

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

  def find(sourceDirectories: Seq[Path], symbol: String): Option[Location] = {
    parse(symbol).flatMap { case (pkg, names) =>
      val top = names.head
      val target = names.last
      val dirs = sourceDirectories.map(d => pkg.foldLeft(d)(_ resolve _)).filter(Files.isDirectory(_))
      // トップレベルと同名のファイルを優先し、なければ同じパッケージの他のファイルも探す
      val preferred = dirs.flatMap(d => List(d.resolve(top + ".st"), d.resolve(top + ".scala"))).filter(Files.isRegularFile(_))
      val others = dirs.flatMap { d =>
        val s = Files.list(d)
        try s.iterator().asScala.filter(f => f.toString.endsWith(".scala") && Files.isRegularFile(f)).toList.sortBy(_.toString)
        finally s.close()
      }.filterNot(preferred.contains)
      (preferred ++ others).iterator.flatMap { f =>
        val text = new String(Files.readAllBytes(f), StandardCharsets.UTF_8)
        val isSt = f.toString.endsWith(".st")
        val found = definitionOffset(text, target).orElse {
          // .stのobject自体は、ファイルの先頭を定義とする
          if (isSt && names.size == 1) Some(0) else None
        }
        found.map { offset =>
          val index = new LineIndex(text)
          new Location(f.toUri.toString, index.range(offset, offset + (if (offset == 0 && isSt) 0 else target.length)))
        }
      }.toStream.headOption
    }
  }

  private def definitionOffset(text: String, name: String): Option[Int] = {
    val q = Pattern.quote(name)
    val keyword = Pattern.compile(s"""\\b(?:class|object|trait|enum|type|def|val|var|given)\\s+($q)\\b""")
    // case classの引数など(`name: Type`)
    val param = Pattern.compile(s"""[(,]\\s*(?:(?:val|var)\\s+)?($q)\\s*:""")
    List(keyword, param).iterator.map(_.matcher(text)).collectFirst { case m if m.find() => m.start(1) }
  }

}
