<script lang="scala">
case class State(name: RV[String] = RV("Taro"),
                 age: RV[Int] = RV(20),
                 memo: RV[String] = RV(""),
                 agree: RV[Boolean] = RV(false),
                 color: RV[String] = RV("red"),
                 tags: RV[List[String]] = RV(List("scala")))

def initState() = State()
</script>

<template>
  <p><input type="text" v-model="s.name"> → {{ s.name }}</p>
  <p><input type="number" v-model="s.age" style="width: 5em"> → 来年は{{ s.age.mapC(_ + 1) }}歳</p>
  <p><textarea v-model="s.memo"></textarea> → {{ s.memo }}</p>
  <p><label><input type="checkbox" v-model="s.agree"> 同意する</label> → {{ s.agree }}</p>
  <p>
    <label><input type="radio" name="model-color" value="red" v-model="s.color"> red</label>
    <label><input type="radio" name="model-color" value="blue" v-model="s.color"> blue</label>
    <select v-model="s.color">
      <option value="red">red</option>
      <option value="blue">blue</option>
    </select>
    → {{ s.color }}
  </p>
  <p>
    <label><input type="checkbox" value="scala" v-model="s.tags"> scala</label>
    <label><input type="checkbox" value="js" v-model="s.tags"> js</label>
    <label><input type="checkbox" value="css" v-model="s.tags"> css</label>
    → {{ s.tags.mapC(_.mkString(", ")) }}
  </p>
</template>
