package io.github.ondeoma.scalactive.ls

import java.nio.file.Paths

class LibraryVersionSuite extends munit.FunSuite {

  private def cp(names: String*) = names.map(n => Paths.get("/home/u/.cache/coursier/v1/https/repo1.maven.org/maven2", n))

  test("classpathのjarの名前からScalactiveの版を読む") {
    assertEquals(LibraryVersion.of(cp("scala3-library_sjs1_3-3.3.7.jar", "scalactive_sjs1_3-0.5.0.jar")), Some("0.5.0"))
    assertEquals(LibraryVersion.of(cp("scalactive_sjs1_3-0.6.0-SNAPSHOT.jar")), Some("0.6.0-SNAPSHOT"))
    // コンパイラ(scalactive-compiler)などは対象外. ソースから使っている場合は分からない
    assertEquals(LibraryVersion.of(cp("scalactive-compiler_2.12-0.4.4.jar", "classes")), None)
  }

  test("必要な版より古い場合だけ警告する") {
    assert(LibraryVersion.older("0.5.0", "0.6.0"))
    assert(LibraryVersion.older("0.4.10", "0.6.0"))
    assert(!LibraryVersion.older("0.6.0", "0.6.0"))
    assert(!LibraryVersion.older("0.6.0-SNAPSHOT", "0.6.0"))
    assert(!LibraryVersion.older("0.10.0", "0.6.0"))
    assert(!LibraryVersion.older("1.0", "0.6.0"))
    assert(LibraryVersion.warning(cp("scalactive_sjs1_3-0.5.0.jar")).exists(_.contains("0.6.0 or later")))
    assertEquals(LibraryVersion.warning(cp("scalactive_sjs1_3-0.6.0.jar")), None)
    assertEquals(LibraryVersion.warning(cp("classes")), None)
  }

}
