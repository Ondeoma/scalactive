package io.github.ondeoma.scalactive.compiler

import java.nio.file.{Files, Path}
import java.util.zip.ZipFile
import scala.collection.JavaConverters._

/**
 * `import org.scalajs.dom.*` で使えるようになる名前.
 * .stの生成コードは、この名前を使っている場合だけ dom.* をimportします(-Wunused:importsの警告を出さないため).
 *
 * 型・object・サブパッケージは、利用者のclasspathにあるscalajs-domのjarから読みます(利用者の使う版に合わせるため).
 * パッケージオブジェクトの値・型の別名はjarの一覧に現れないので、[[PackageMembers]] に持ちます.
 */
object DomNames {

  /** scalajs-dom 2.8 の package object dom の値・関数・型の別名 */
  val PackageMembers: Set[String] = Set(
    "console", "css", "document", "fetch", "webcrypto", "window",
    "AlgorithmIdentifier", "BigInteger", "BlobPart", "BodyInit", "BufferSource", "ByteString", "ClientRect", "ClientRectList",
    "ClipboardItemData", "HashAlgorithmIdentifier", "HeadersInit", "IDBKey", "IDBKeyPath", "IDBValue", "KeyAlgorithmIdentifier",
    "NodeListOf", "OpenEndedDictionary", "RequestInfo", "Sequence", "Transferable"
  )

  private val Jar = """scalajs-dom_sjs1_[^-/\\]+-[0-9][^/\\]*\.jar""".r
  private val Entry = """org/scalajs/dom/([A-Za-z_][A-Za-z0-9_]*)(?:\$?\.class|/.*)""".r

  /** classpathのscalajs-domのjarから、dom.* で使える名前を集めます. jarが見つからない場合はNone(常に dom.* をimportする) */
  def fromClasspath(classpath: Seq[Path]): Option[Set[String]] = {
    classpath.find(p => p.getFileName != null && Jar.pattern.matcher(p.getFileName.toString).matches() && Files.isRegularFile(p)).flatMap { jar =>
      try {
        val zip = new ZipFile(jar.toFile)
        try {
          val names = zip.entries.asScala.map(_.getName).collect { case Entry(n) => n }.toSet - "package"
          Some(names ++ PackageMembers)
        } finally zip.close()
      } catch {
        case scala.util.control.NonFatal(_) => None
      }
    }
  }

}
