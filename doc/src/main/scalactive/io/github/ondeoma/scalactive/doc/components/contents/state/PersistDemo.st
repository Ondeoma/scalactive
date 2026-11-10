<template>
  <div>
    <p>名前(localStorage. 他のタブにもすぐ反映): <input type="text" v-model="PrefsStore.name"></p>
    <p>下書き(sessionStorage. このタブだけ): <input type="text" v-model="DraftStore.draft"></p>
    <ul>
      <li v-for="memo in PrefsStore.memos">{{ memo.mapC(_.text) }}</li>
    </ul>
    <button @click='PrefsStore.memos.add(Memo("メモ " + (PrefsStore.memos.v.size + 1)))'>メモを追加</button>
    <button @click="PrefsStore.clear(); DraftStore.clear()">保存した値を消す</button>
  </div>
</template>
