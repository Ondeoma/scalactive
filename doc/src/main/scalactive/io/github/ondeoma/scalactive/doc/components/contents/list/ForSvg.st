<script lang="scala">
case class State(values: RV[List[Int]] = RV(List(3, 7, 5)))

def initState() = State()
</script>

<template>
  <button @click="s.values := s.values.v :+ (scala.util.Random.nextInt(9) + 1)">棒を足す</button>
  <svg :viewBox='s"0 0 ${s.values.v.length * 20} 100"' width="240" height="100">
    <g v-for="(v, i) in s.values">
      <rect :x="i * 20 + 2" :y="100 - v * 10" width="16" :height="v * 10" fill="steelblue">
        <title>{{ v }}</title>
      </rect>
    </g>
  </svg>
</template>
