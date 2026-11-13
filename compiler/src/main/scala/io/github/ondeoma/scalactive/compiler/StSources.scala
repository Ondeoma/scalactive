package io.github.ondeoma.scalactive.compiler

import java.nio.file.Path

/**
 * .stファイルのパスから、生成するobjectのパッケージ・名前と出力先を決めます.
 * sbtプラグインと言語サーバーで同じ規則を使うための共通処理です.
 */
object StSources {

  /**
   * @param roots .stを探すディレクトリ(scalactiveSourceDirectories)
   * @return fileを含む最も深いroot. どのrootの下にもない場合はNone.
   */
  def rootOf(roots: Seq[Path], file: Path): Option[Path] = {
    val abs = file.toAbsolutePath.normalize
    roots.map(_.toAbsolutePath.normalize).filter(r => abs.startsWith(r)).sortBy(-_.getNameCount).headOption
  }

  /** fileの内容sourceから、コンパイラへの入力を作ります. ディレクトリ構成がパッケージになります. */
  def input(root: Path, file: Path, source: String): StInput = {
    val rel = root.toAbsolutePath.normalize.relativize(file.toAbsolutePath.normalize)
    val dirs = (0 until rel.getNameCount - 1).map(i => rel.getName(i).toString)
    StInput(
      source = source,
      fileName = rel.toString.replace('\\', '/'),
      objectName = rel.getFileName.toString.stripSuffix(".st"),
      packageName = if (dirs.isEmpty) None else Some(dirs.mkString(".")),
      siblings = siblingsOf(file)
    )
  }

  /** fileと同じディレクトリにある.stのobject名(自分を含む) */
  def siblingsOf(file: Path): Set[String] = {
    val dir = file.toAbsolutePath.normalize.getParent
    if (dir == null || !java.nio.file.Files.isDirectory(dir)) Set.empty
    else {
      val s = java.nio.file.Files.list(dir)
      try {
        import scala.collection.JavaConverters._
        s.iterator.asScala.map(_.getFileName.toString).filter(_.endsWith(".st")).map(_.stripSuffix(".st")).toSet
      } finally s.close()
    }
  }

  /** 生成したScalaソースの出力先 */
  def outputFile(outDir: Path, out: StOutput): Path = {
    val pkgDir = out.packageName.fold(outDir)(p => outDir.resolve(p.replace('.', '/')))
    pkgDir.resolve(out.objectName + ".scala")
  }

}
