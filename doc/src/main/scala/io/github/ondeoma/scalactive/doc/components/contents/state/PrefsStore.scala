package io.github.ondeoma.scalactive.doc.components.contents.state

import io.github.ondeoma.scalactive.components.ComponentManager.*

case class Memo(text: String, done: Boolean = false) derives StorageCodec

// localStorageに保存し、他のタブでの変更もすぐに反映する
object PrefsStore extends PersistentStore("scalactive-doc.prefs", StorageKind.Local(sync = true)) {
  val name: RV[String] = persist("name", "")
  val memos: RVList[Memo] = persistList("memos", Nil)
}

// sessionStorageに保存する(タブごと. 再読み込みしても残る)
object DraftStore extends PersistentStore("scalactive-doc.draft", StorageKind.Session) {
  val draft: RV[String] = persist("draft", "")
}
