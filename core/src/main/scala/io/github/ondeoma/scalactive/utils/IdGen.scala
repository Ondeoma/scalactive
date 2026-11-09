package io.github.ondeoma.scalactive.utils

/**
 * 監視・イベントハンドラーのid.
 * idは登録先(Reactiveの監視の一覧など)の中で重ならなければよいため、crypto.randomUUIDではなく連番にします.
 * (randomUUIDは1,000行の表を作るときの時間の1割ほどを占めていました)
 */
private[scalactive] object IdGen {

  // Intの範囲を超えても重ならないようDoubleで数える
  private var counter = 0.0

  def next(): String = {
    counter += 1
    counter.toString
  }

}
