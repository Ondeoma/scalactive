<template>
  <div class="card"><p class="title"><em>hi</em></p></div>
</template>

<style lang="scss" scoped>
@use "vars";
@use "theme/colors";

.card {
  padding: vars.$pad;
  &:hover { opacity: 0.5; }
  .title { color: colors.$main; }
}
</style>

<style lang="sass">
em
  font-weight: bold
</style>
