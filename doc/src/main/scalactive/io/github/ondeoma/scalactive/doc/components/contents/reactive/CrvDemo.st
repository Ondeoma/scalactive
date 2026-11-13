<script lang="scala">
case class State(price: RV[Int] = RV(100),
                 qty: RV[Int] = RV(2))

def initState() = State()
</script>

<template>
  <p>
    単価 <input type="number" v-model="s.price" style="width: 5em">円
    × <input type="number" v-model="s.qty" style="width: 4em">個
  </p>
  <p>合計: {{ CRV(s.price, s.qty, (p, q) => p * q) }}円</p>
  <p>税込: {{ s.price.v * s.qty.v * 110 / 100 }}円</p>
  <p v-if="s.qty.v >= 10">まとめ買いです!</p>
</template>
