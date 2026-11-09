<script lang="scala">
case class Props(userName: String, count: Reactive[Int] = RV(0))
</script>

<template>
  <p>こんにちは、{{ props.userName }}さん. count = {{ props.count }}</p>
</template>
