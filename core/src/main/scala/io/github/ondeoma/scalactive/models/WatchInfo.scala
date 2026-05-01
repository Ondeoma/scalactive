package io.github.ondeoma.scalactive.models

import io.github.ondeoma.scalactive.utils.TypeAlias.ID
import io.github.ondeoma.scalactive.reactive.Reactive

case class WatchInfo(value: Reactive[?],
                     watchId: ID) {
  def abort(): Unit = {
    value.rmWatcher(watchId)
  }
}
