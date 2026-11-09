<script lang="scala">
case class Term(word: String, desc: String)

val terms = List(Term("RV", "書き換えられるリアクティブな値"),
                 Term("CRV", "他の値から計算されるリアクティブな値"))
</script>

<template>
  <dl>
    <template v-for="t in terms">
      <dt>{{ t.word }}</dt>
      <dd>{{ t.desc }}</dd>
    </template>
  </dl>
</template>
