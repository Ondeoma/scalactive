<script lang="scala">
case class State(color: RV[String] = RV("#ff6347"),
                 size: RV[Int] = RV(16),
                 bold: RV[Boolean] = RV(false))

def initState() = State()
</script>

<template>
  <p>
    <input type="color" v-model="s.color">
    <input type="number" v-model="s.size" style="width: 4em">px
    <label><input type="checkbox" v-model="s.bold"> 太字</label>
  </p>
  <!-- 文字列: style属性全体になります -->
  <p :style='s.color.mapC(c => "color: " + c)'>文字列で指定</p>
  <!-- オブジェクト記法(Map): プロパティごとに設定され、静的なstyle属性と併用できます -->
  <p style="border: 1px solid #999; padding: 4px" :style='Map("color" -> s.color, "fontSize" -> s.size.mapC(n => n.toString + "px"), "fontWeight" -> s.bold.mapC(b => if (b) "bold" else ""))'>Mapで指定</p>
</template>
