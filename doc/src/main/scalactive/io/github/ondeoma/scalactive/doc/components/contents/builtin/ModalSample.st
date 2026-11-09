<script lang="scala">
case class State(open: RV[Boolean] = RV(false))

def initState() = State()
</script>

<template>
  <div>
    <button @click="s.open := true">モーダルを開く</button>
    <Teleport v-if="s.open" to="body">
      <div class="modal" @click.self="s.open := false">
        <div class="modal-body">
          <p>bodyの末尾に描画されています.</p>
          <button @click="s.open := false">閉じる</button>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.modal { position: fixed; inset: 0; background: rgba(0, 0, 0, 0.4); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-body { background: #fff; color: #222; padding: 16px; border-radius: 6px; }
</style>
