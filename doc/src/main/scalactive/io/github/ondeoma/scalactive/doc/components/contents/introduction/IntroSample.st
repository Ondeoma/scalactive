<script lang="scala">
// 状態はReactiveな値(RV)で持ちます.
case class State(txt: RV[String] = RV(""))

def initState() = State()
</script>

<template>
  <!-- HTMLをそのまま書くような形です. -->
  <table>
    <tbody>
      <tr>
        <th>入力</th>
        <td><input type="text" v-model="s.txt"></td>
      </tr>
      <tr>
        <th>状態</th>
        <td>{{ s.txt }}</td>
      </tr>
      <tr>
        <th>イベント</th>
        <td><button @click='window.alert(s"入力値は${s.txt.v}です。")'>ボタン</button></td>
      </tr>
    </tbody>
  </table>
</template>
