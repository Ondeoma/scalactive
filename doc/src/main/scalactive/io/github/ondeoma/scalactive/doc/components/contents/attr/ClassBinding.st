<script lang="scala">
case class State(active: RV[Boolean] = RV(true),
                 size: RV[String] = RV("small"))

def initState() = State()
</script>

<template>
  <p>
    <label><input type="checkbox" v-model="s.active"> active</label>
    <select v-model="s.size">
      <option value="small">small</option>
      <option value="large">large</option>
    </select>
  </p>
  <!-- 文字列(空白区切りで複数指定できます) -->
  <p :class="s.size">文字列: sizeの値がクラスになります.</p>
  <!-- クラス名と条件の組 -->
  <p :class='"active" -> s.active'>組: activeの時だけ緑色になります.</p>
  <!-- オブジェクト記法(Map) -->
  <p :class='Map("active" -> s.active, "underline" -> true)'>Map: 下線は常に付きます.</p>
  <!-- 配列記法(文字列と組の混在). 静的なclass属性と併用できます -->
  <p class="box" :class='List("bold", "active" -> s.active)'>List: 枠と太字は常に付きます.</p>
</template>

<style scoped>
.active { color: seagreen; }
.underline { text-decoration: underline; }
.bold { font-weight: bold; }
.small { font-size: 12px; }
.large { font-size: 18px; }
.box { border: 1px solid #999; padding: 4px; }
</style>
