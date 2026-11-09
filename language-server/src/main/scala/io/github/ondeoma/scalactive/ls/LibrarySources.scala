package io.github.ondeoma.scalactive.ls

import coursierapi.{Dependency, Fetch}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths, StandardCopyOption}
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipFile
import scala.collection.JavaConverters._
import scala.meta.pc.{SymbolSearch, SymbolSearchVisitor}
import scala.util.control.NonFatal

/**
 * クラスパスの索引. パッケージ(`a/b/`)ごとに、トップレベルのクラスファイル名と、それを含むエントリ(jarかディレクトリ)を持ちます.
 * 自動importの候補の検索と、ライブラリのシンボルがどのjarにあるかの判定に使います.
 *
 * 内部クラス(`A$B.class`)は除き、objectの `A$.class` と、トップレベルの定義の `Foo$package.class` は残します.
 */
final class ClasspathIndex(classpath: Seq[Path]) {

  import ClasspathIndex._

  /** パッケージ → (クラスファイル名 → エントリ). 最初に使うときに作ります */
  lazy val packages: Map[String, Map[String, Path]] = {
    val b = scala.collection.mutable.LinkedHashMap.empty[String, scala.collection.mutable.LinkedHashMap[String, Path]]
    classpath.foreach { entry =>
      try listClassfiles(entry).foreach { path =>
        val slash = path.lastIndexOf('/')
        val (pkg, file) = (path.substring(0, slash + 1), path.substring(slash + 1))
        if (isToplevel(file)) b.getOrElseUpdate(pkg, scala.collection.mutable.LinkedHashMap.empty).getOrElseUpdate(file, entry)
      } catch {
        case NonFatal(_) => // 読めないエントリは無視する
      }
    }
    b.map { case (k, v) => k -> v.toMap }.toMap
  }

  /** パッケージ `a/b/` のトップレベルの名前 `Top` を含むエントリ */
  def entryOf(pkg: String, top: String): Option[Path] = {
    packages.get(pkg).flatMap(files => List(top + ".class", top + "$.class").flatMap(files.get).headOption)
  }

  /**
   * 名前がqueryに合うクラスを visitor に渡します(presentation compilerの自動importの候補).
   * 多すぎる場合は limit で打ち切り、INCOMPLETE を返します.
   */
  def search(query: String, visitor: SymbolSearchVisitor, limit: Int = 200): SymbolSearch.Result = {
    if (query.isEmpty) SymbolSearch.Result.COMPLETE
    else {
      var count = 0
      var complete = true
      val it = packages.iterator
      while (it.hasNext && complete && !visitor.isCancelled) {
        val (pkg, files) = it.next()
        // A.class と A$.class は同じ名前として1回だけ渡す
        val names = files.keys.toList.map(f => f.stripSuffix(".class").stripSuffix("$")).distinct
          .filterNot(n => n.contains("$") || n == "package")
          .filter(n => matches(query, n))
        if (names.nonEmpty && visitor.shouldVisitPackage(pkg)) {
          names.foreach { n =>
            if (count >= limit) complete = false
            else {
              val file = if (files.contains(n + ".class")) n + ".class" else n + "$.class"
              count += visitor.visitClassfile(pkg, file)
            }
          }
        }
      }
      if (complete) SymbolSearch.Result.COMPLETE else SymbolSearch.Result.INCOMPLETE
    }
  }

}

object ClasspathIndex {

  private def listClassfiles(entry: Path): Iterator[String] = {
    if (Files.isDirectory(entry)) {
      val s = Files.walk(entry)
      try s.iterator().asScala.filter(p => p.toString.endsWith(".class") && Files.isRegularFile(p))
        .map(p => entry.relativize(p).toString.replace(java.io.File.separatorChar, '/')).toList.iterator
      finally s.close()
    } else if (Files.isRegularFile(entry) && entry.toString.endsWith(".jar")) {
      val zip = new ZipFile(entry.toFile)
      try zip.entries().asScala.map(_.getName).filter(n => n.endsWith(".class") && !n.startsWith("META-INF/")).toList.iterator
      finally zip.close()
    } else Iterator.empty
  }

  private[ls] def isToplevel(file: String): Boolean = {
    val name = file.stripSuffix(".class")
    if (name == "module-info" || name == "package-info") false
    else {
      val base = name.stripSuffix("$")
      !base.contains("$") || base.endsWith("$package") && base.indexOf('$') == base.length - "$package".length
    }
  }

  /** 前方一致(大文字・小文字を区別しない)か、キャメルケースの区切りごとの前方一致(`HTE` → `HTMLElement`, `RvL` → `RVList`) */
  private[ls] def matches(query: String, name: String): Boolean = {
    name.regionMatches(true, 0, query, 0, query.length) || {
      val humps = humpStarts(name)
      def go(q: Int, h: Int): Boolean = {
        if (q == query.length) true
        else if (h >= humps.length) false
        else {
          val start = humps(h)
          val end = if (h + 1 < humps.length) humps(h + 1) else name.length
          // このこぶの先頭からk文字をqueryに対応させ、次のこぶへ進む
          (1 to (end - start)).exists { k =>
            q + k <= query.length && name.regionMatches(true, start, query, q, k) && go(q + k, h + 1)
          }
        }
      }
      query.nonEmpty && query.charAt(0).toLower == name.charAt(0).toLower && go(0, 0)
    }
  }

  /** キャメルケースの区切りの位置(先頭、大文字の始まり、数字・_ の後) */
  private def humpStarts(name: String): Array[Int] = {
    val b = Array.newBuilder[Int]
    var i = 0
    while (i < name.length) {
      val c = name.charAt(i)
      val prev = if (i == 0) ' ' else name.charAt(i - 1)
      if (i == 0 || c.isUpper && (!prev.isUpper || i + 1 < name.length && name.charAt(i + 1).isLower) || prev == '_' || prev.isDigit && !c.isDigit) b += i
      i += 1
    }
    b.result()
  }

}

/**
 * ライブラリ(jar)のソース. jarの隣の `-sources.jar`、ivyの `srcs/`、またはcoursierのキャッシュのパスから座標を読んで取得したソースjarを、
 * `~/.cache/scalactive/sources/` に展開して返します. 結果(見つからなかったことも)はjarごとに覚えます.
 */
object LibrarySources {

  @volatile var log: String => Unit = (_: String) => ()

  /** 展開先. テストなどで変える場合は環境変数 SCALACTIVE_SOURCES_CACHE で指定します */
  def cacheDir: Path = sys.env.get("SCALACTIVE_SOURCES_CACHE").filter(_.nonEmpty).map(Paths.get(_))
    .getOrElse(Paths.get(System.getProperty("user.home"), ".cache", "scalactive", "sources"))

  private val roots = new ConcurrentHashMap[Path, Option[Path]]()

  /** jarのソースを展開したディレクトリ. ディレクトリのエントリ(プロジェクトのクラス)やソースのないjarはNone */
  def sourceRoot(entry: Path): Option[Path] = {
    if (!entry.toString.endsWith(".jar")) None
    else roots.computeIfAbsent(entry, e => {
      try sourcesJar(e).map(extract)
      catch {
        case NonFatal(ex) =>
          log(s"cannot get the sources of $e: $ex")
          None
      }
    })
  }

  private def sourcesJar(jar: Path): Option[Path] = {
    val base = jar.getFileName.toString.stripSuffix(".jar")
    val sibling = jar.resolveSibling(base + "-sources.jar")
    // ivyのローカルリポジトリ: .../jars/x.jar に対して .../srcs/x-sources.jar
    val ivy = Option(jar.getParent).filter(_.getFileName.toString == "jars").map(_.resolveSibling("srcs").resolve(base + "-sources.jar"))
    (Some(sibling) ++ ivy).find(Files.isRegularFile(_)).orElse(fetch(jar))
  }

  /** coursierのキャッシュのパス(…/maven2/<group>/<artifact>/<version>/<artifact>-<version>.jar)から座標を読み、ソースjarを取得します */
  private[ls] def coordinates(jar: Path): Option[(String, String, String)] = {
    val segs = jar.iterator().asScala.map(_.toString).toVector
    val marker = segs.lastIndexOf("maven2")
    val n = segs.length
    if (marker < 0 || n - marker < 5) None
    else {
      val (version, artifact) = (segs(n - 2), segs(n - 3))
      if (segs(n - 1) != s"$artifact-$version.jar") None
      else Some((segs.slice(marker + 1, n - 3).mkString("."), artifact, version))
    }
  }

  private def fetch(jar: Path): Option[Path] = {
    coordinates(jar).flatMap { case (g, a, v) =>
      log(s"fetching the sources of $g:$a:$v")
      Fetch.create()
        .addDependencies(Dependency.of(g, a, v).withTransitive(false))
        .withClassifiers(java.util.Collections.singleton("sources"))
        .fetch().asScala.map(_.toPath).find(_.getFileName.toString.endsWith("-sources.jar"))
    }
  }

  private def extract(sources: Path): Path = {
    val digest = MessageDigest.getInstance("SHA-1")
      .digest(s"${sources.toAbsolutePath}:${Files.size(sources)}:${Files.getLastModifiedTime(sources).toMillis}".getBytes(StandardCharsets.UTF_8))
    val id = sources.getFileName.toString.stripSuffix(".jar") + "-" + digest.take(6).map(b => f"${b & 0xff}%02x").mkString
    val dir = cacheDir.resolve(id)
    val done = dir.resolve(".done")
    if (!Files.exists(done)) {
      val zip = new ZipFile(sources.toFile)
      try zip.entries().asScala.filterNot(_.isDirectory).foreach { e =>
        val out = dir.resolve(e.getName).normalize
        if (out.startsWith(dir)) {
          Files.createDirectories(out.getParent)
          val in = zip.getInputStream(e)
          try Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING)
          finally in.close()
        }
      } finally zip.close()
      Files.write(done, Array.emptyByteArray)
    }
    dir
  }

}

/** ソースの `/** */` のコメント(scaladoc・javadoc)を取り出し、Markdownにします */
object DocComments {

  /** offset(定義の名前の位置)の行の直前にあるドキュメントコメント. 間のアノテーションの行は飛ばします */
  def before(text: String, offset: Int): Option[String] = {
    var lineStart = text.lastIndexOf('\n', math.max(0, offset - 1)) + 1
    var result: Option[String] = None
    var continue = true
    while (continue && lineStart > 0) {
      val prevEnd = lineStart - 1
      val prevStart = text.lastIndexOf('\n', prevEnd - 1) + 1
      val line = text.substring(prevStart, prevEnd).trim
      if (line.isEmpty || line.startsWith("@")) lineStart = prevStart
      else {
        continue = false
        if (line.endsWith("*/")) {
          val end = text.lastIndexOf("*/", prevEnd)
          val start = text.lastIndexOf("/**", end)
          if (start >= 0 && !text.substring(start + 3, end).contains("*/")) result = Some(text.substring(start, end + 2))
        }
      }
    }
    result.map(toMarkdown).filter(_.trim.nonEmpty)
  }

  private val Tag = """^@(\w+)\s*(.*)$""".r

  def toMarkdown(comment: String): String = {
    val body = comment.stripPrefix("/**").stripSuffix("*/")
    val lines = body.split("\n", -1).toList.map(l => l.replaceFirst("""^\s*\*(?!/) ?""", "")).map(_.replaceAll("""\s+$""", ""))
    // 本文とタグ(@param など)に分ける
    val (main, tags) = lines.span(l => !l.trim.startsWith("@"))
    val tagBlocks = tags.foldLeft(List.empty[List[String]]) { (acc, l) =>
      if (l.trim.startsWith("@")) List(l.trim) :: acc
      else acc match {
        case h :: t => (h :+ l.trim) :: t
        case Nil => Nil
      }
    }.reverse.map(_.mkString(" ").trim)
    val text = inline(codeBlocks(main).mkString("\n")).trim
    val grouped = tagBlocks.flatMap {
      case Tag(name, rest) => Some(name -> rest)
      case _ => None
    }
    def section(title: String, names: Set[String], named: Boolean): Option[String] = {
      val items = grouped.filter(t => names.contains(t._1)).map { case (_, rest) =>
        if (named) {
          val (n, d) = rest.span(!_.isWhitespace)
          s"- `$n`: ${inline(d.trim)}"
        } else s"- ${inline(rest)}"
      }
      if (items.isEmpty) None else Some(s"**$title**\n\n" + items.mkString("\n"))
    }
    (List(text).filter(_.nonEmpty) ++ List(
      section("Type parameters", Set("tparam"), named = true),
      section("Parameters", Set("param"), named = true),
      section("Returns", Set("return"), named = false),
      section("Throws", Set("throws", "exception"), named = true),
      section("Note", Set("note"), named = false),
      section("Example", Set("example"), named = false),
      section("See also", Set("see"), named = false)
    ).flatten).mkString("\n\n")
  }

  /** scaladocの {{{ }}} と、javadocの <pre> をコードブロックにします */
  private def codeBlocks(lines: List[String]): List[String] = {
    lines.flatMap { l =>
      val t = l.trim
      if (t == "{{{") List("```scala")
      else if (t == "}}}") List("```")
      else if (t.startsWith("{{{") && t.endsWith("}}}") && t.length > 6) List("`" + t.drop(3).dropRight(3).trim + "`")
      else List(l.replace("<pre>", "\n```\n").replace("</pre>", "\n```\n"))
    }
  }

  /** 1行の中の書き方: [[a.b.C]] / {@link C} / {@code x} とHTMLのタグ */
  private def inline(s: String): String = {
    s.replaceAll("""\[\[(\S+?)\s+([^\]]+)\]\]""", "$2")
      .replaceAll("""\[\[([^\]]+)\]\]""", "`$1`")
      .replaceAll("""\{@(?:link|linkplain)\s+([^}\s]+)(?:\s+([^}]+))?\}""", "`$1`")
      .replaceAll("""\{@(?:code|literal)\s+([^}]*)\}""", "`$1`")
      .replaceAll("""(?i)<p>""", "\n\n")
      .replaceAll("""(?i)</?(?:p|b|i|em|strong|code|tt|ul|ol|li|br|a|blockquote|div|span|h\d)(?:\s[^>]*)?/?>""", "")
      .replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
  }

}
