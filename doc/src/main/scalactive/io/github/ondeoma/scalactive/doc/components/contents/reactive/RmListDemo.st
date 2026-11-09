<script lang="scala">
import io.github.ondeoma.scalactive.reactive.ReactiveModelMacro.*

case class Human(name: String,
                 age: Int)

case class HumanRM(name: RV[String],
                   age: RV[Int]) extends ReactiveModel[Human, HumanRM] {

  val reactives = collectReactives(this)
  val reactiveModels = collectReactiveModels(this)
  def reload(s: O) = genReload(this, s)
  def toOrigin = genToOrigin(this)

}

object HumanRM {
  given RMCompatible[Human, HumanRM] = genToReactiveModel[Human, HumanRM](_)
}

case class State(humans: RMList[Human, HumanRM] = RMList(List(Human("Taro", 10), Human("Hanako", 20))))

def initState() = State()
</script>

<template>
  <ul>
    <!-- RMListの場合、hはHumanRMになります -->
    <li v-for="h in s.humans">
      <input type="text" v-model="h.name"> {{ h.age }}歳
      <button @click="h.age := h.age.v + 1">+1歳</button>
    </li>
  </ul>
  <button @click='s.humans.add(Human("Bob", 30))'>Bobを追加</button>
  <p>s.humans.v = {{ s.humans }}</p>
</template>
