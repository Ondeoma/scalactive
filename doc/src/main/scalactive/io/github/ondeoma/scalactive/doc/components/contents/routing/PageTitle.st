<script lang="scala">
case class Props(title: String)

// 表示している間、画面(タブ)のタイトルにします
def mounted(props: Props): Unit = pageTitle(props.title)
</script>

<template>
  <h2>{{ props.title }}</h2>
</template>
