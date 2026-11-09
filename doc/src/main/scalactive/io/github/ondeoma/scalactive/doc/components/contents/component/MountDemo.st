<script lang="scala">
case class State(show: RV[Boolean] = RV(true))

def initState() = State()
</script>

<template>
  <div>
    <button @click="s.show := !s.show.v">表示を切り替える</button>
    <ExternalWidget v-if="s.show" />
  </div>
</template>
