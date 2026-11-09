<script lang="scala">
case class State(text: RV[String] = RV("TEXT"),
                 num: RV[Int] = RV(1),
                 memo: RV[String] = RV(""),
                 agree: RV[Boolean] = RV(false),
                 tags: RV[List[String]] = RV(List()),
                 color: RV[String] = RV("red"),
                 size: RV[Int] = RV(2),
                 files: RV[List[File]] = RV(List()))

def initState() = State()

// ファイルはv-modelではなく、changeイベントで受け取る
def onFiles(s: State, e: Event): Unit = {
  s.files := e.target.asInstanceOf[HTMLInputElement].files.toList
}
</script>

<template>
  <div>
    <p><input type="text" v-model.lazy="s.text"> → {{ s.text }}</p>
    <p><input type="number" v-model="s.num"> → {{ s.num }}</p>
    <p><textarea v-model="s.memo"></textarea> → {{ s.memo }}</p>
    <p><label><input type="checkbox" v-model="s.agree"> 同意する</label> → {{ s.agree }}</p>
    <p>
      <label><input type="checkbox" value="A" v-model="s.tags"> A</label>
      <label><input type="checkbox" value="B" v-model="s.tags"> B</label>
      → {{ s.tags.mapC(_.mkString(", ")) }}
    </p>
    <p>
      <label><input type="radio" name="input-color" value="red" v-model="s.color"> red</label>
      <label><input type="radio" name="input-color" value="blue" v-model="s.color"> blue</label>
      → {{ s.color }}
    </p>
    <p>
      <select v-model="s.size">
        <option value="1">One</option>
        <option value="2">Two</option>
      </select>
      → {{ s.size }}
    </p>
    <p><input type="file" multiple @change="onFiles(s, event)"> → {{ s.files.mapC(_.map(_.name).mkString(", ")) }}</p>
  </div>
</template>
