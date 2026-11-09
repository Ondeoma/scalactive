<script lang="scala">
case class Todo(id: Int, title: String)

case class State(todos: RV[List[Todo]] = RV(List(Todo(1, "買い物"), Todo(2, "掃除"), Todo(3, "洗濯"))))

def initState() = State()
</script>

<template>
  <button @click="s.todos := s.todos.v.reverse">逆順にする</button>
  <ul>
    <li v-for="t in s.todos" :key="t.id">
      {{ t.id }}: {{ t.title }}
      <input type="text" placeholder="入力した値は行と一緒に動きます">
    </li>
  </ul>
</template>
