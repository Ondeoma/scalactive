// .stからScalaソースを生成するだけのテスト(Scala.jsでのコンパイルは行わない)
scalaVersion := "3.3.7"
enablePlugins(ScalactivePlugin)

def outDir = Def.setting((Compile / sourceManaged).value / "scalactive")

TaskKey[Unit]("checkHello") := {
  val files = (Compile / scalactiveGenerate).value
  val hello = files.find(_.getName == "Hello.scala").getOrElse(sys.error(s"Hello.scala not generated: $files"))
  val code = IO.read(hello)
  assert(hello.getParentFile.getName == "app", hello)
  assert(code.contains("package app\n"), code)
  assert(code.contains("object Hello extends StatefulComponent[_root_.app.Hello.State]"), code)
  assert(code.contains("__st.text(/* Hello.st:6 */ s.n)"), code)
}

// 生成タスクの戻り値と、出力ディレクトリ上の.scalaファイルの数を確認する(削除した.stの生成物が残らないこと)
InputKey[Unit]("checkCount") := {
  val expected = complete.DefaultParsers.spaceDelimited("<n>").parsed.head.toInt
  val files = (Compile / scalactiveGenerate).value
  val onDisk = (outDir.value ** "*.scala").get
  assert(files.size == expected, s"expected $expected generated files but got $files")
  assert(onDisk.size == expected, s"expected $expected files on disk but got $onDisk")
}

TaskKey[Unit]("checkLspConfig") := {
  val file = (Compile / scalactiveLspConfig).value
  val json = IO.read(file)
  assert(file == target.value / "scalactive-lsp.json", file)
  assert(json.contains("\"scalaVersion\": \"3.3.7\""), json)
  assert(json.contains((Compile / classDirectory).value.getAbsolutePath), json)
  assert(json.contains((sourceDirectory.value / "main" / "scalactive").getAbsolutePath), json)
}

// 組み込み(RouterLink)と同じ名前の.stは、同じディレクトリの部品から完全修飾名で参照する. <Link>は空要素の<link>と扱わない
TaskKey[Unit]("checkSiblings") := {
  val files = (Compile / scalactiveGenerate).value
  val code = IO.read(files.find(_.getName == "Nav.scala").getOrElse(sys.error(s"Nav.scala not generated: $files")))
  assert(code.contains("_root_.app.RouterLink(to = \"/\""), code)
  assert(code.contains("_root_.app.Link(to = \"/a\", slot = __st.slotOf"), code)
  val own = IO.read(files.find(_.getName == "RouterLink.scala").get)
  assert(own.contains("extends StatelessPropsComponent[_root_.app.RouterLink.Props]"), own)
}
