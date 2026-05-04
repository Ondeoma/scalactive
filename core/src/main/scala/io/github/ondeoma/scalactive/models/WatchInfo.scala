package io.github.ondeoma.scalactive.models

import io.github.ondeoma.scalactive.reactive.Reactive
import io.github.ondeoma.scalactive.utils.TypeAlias.ID

case class WatchInfo(value: Reactive[?],
                     watchId: ID) {
  def abort(): Unit = {
    value.rmWatcher(watchId)
  }
}
