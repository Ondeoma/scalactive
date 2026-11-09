<script lang="scala">
case class State(open: RV[Boolean] = RV(false))

def initState() = State()

// ページを移動したらメニューを閉じる. 片付けるときに監視を解除する(描画のたびに監視が増えないように)
private var closeOnNavigate: Option[WatchInfo] = None

def mounted(s: State): Unit = closeOnNavigate = Some(Router.href.addWatcher(_ => s.open := false))

def unmounted(): Unit = {
  closeOnNavigate.foreach(_.abort())
  closeOnNavigate = None
}
</script>

<template>
  <nav class="sp-menu sp-only-block">
    <a @click="s.open := !s.open.v"><svg data-v-0f6864fa="" xmlns="http://www.w3.org/2000/svg" aria-hidden="true" focusable="false" viewBox="0 0 24 24" class="menu-icon"><path d="M17,11H3c-0.6,0-1-0.4-1-1s0.4-1,1-1h14c0.6,0,1,0.4,1,1S17.6,11,17,11z"></path><path d="M21,7H3C2.4,7,2,6.6,2,6s0.4-1,1-1h18c0.6,0,1,0.4,1,1S21.6,7,21,7z"></path><path d="M21,15H3c-0.6,0-1-0.4-1-1s0.4-1,1-1h18c0.6,0,1,0.4,1,1S21.6,15,21,15z"></path><path d="M17,19H3c-0.6,0-1-0.4-1-1s0.4-1,1-1h14c0.6,0,1,0.4,1,1S17.6,19,17,19z"></path></svg></a>
  </nav>
  <div v-show="s.open" @click="s.open := false" class="sp-side-menu-bg"></div>
  <div v-show="s.open" class="sp-side-menu">
    <SideNavComponent />
  </div>
</template>
