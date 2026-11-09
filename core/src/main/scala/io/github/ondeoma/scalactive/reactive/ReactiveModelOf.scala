package io.github.ondeoma.scalactive.reactive

import io.github.ondeoma.scalactive.reactive.ReactiveModelMacro.*

/**
 * case classとReactiveModelの相互変換. ReactiveModelOfが使います.
 * 中身はReactiveModelMacroのマクロで作るので、ふつうは自分で書く必要はありません.
 */
trait RMConversion[O, M] {
  def toOrigin(m: M): O
  def reload(m: M, o: O): Unit
  def toReactiveModel(o: O): M
}

object RMConversion {

  def instance[O, M](toO: M => O,
                     rl: (M, O) => Unit,
                     toM: O => M): RMConversion[O, M] = new RMConversion[O, M] {
    def toOrigin(m: M): O = toO(m)
    def reload(m: M, o: O): Unit = rl(m, o)
    def toReactiveModel(o: O): M = toM(o)
  }

  /** フィールドの名前で対応付けて、変換をマクロで作ります */
  inline given derived[O <: Product, M <: ReactiveModel[O, M]]: RMConversion[O, M] = {
    instance[O, M](
      (m: M) => genToOrigin[O, M](m),
      (m: M, o: O) => genReload[O, M](m, o),
      (o: O) => genToReactiveModel[O, M](o),
    )
  }

}

/**
 * 少ない記述で定義できるReactiveModel.
 * {{{
 * case class Human(name: String, age: Int)
 * case class HumanRM(name: RV[String], age: RV[Int]) extends ReactiveModelOf[Human, HumanRM]
 * object HumanRM extends ReactiveModelOf.Companion[Human, HumanRM]
 * }}}
 * reactives・reactiveModels・reload・toOriginと、RMCompatibleのgivenを書く必要はありません.
 * フィールドは元のcase classと同じ名前にし、RV・RVList・ReactiveModel・RMListのどれかにします.
 */
abstract class ReactiveModelOf[Org <: Product, Self <: ReactiveModel[Org, Self]](using conv: RMConversion[Org, Self])
  extends ReactiveModel[Org, Self] {
  self: Self =>

  // case classのフィールドは親クラスの初期化より前に代入されるので、ここで集められます
  val reactives: List[Reactive[?]] = productIterator.collect(collectReactivesPF).toList
  val reactiveModels: List[ReactiveModel[?, ?]] = productIterator.collect(collectReactiveModelsPF).toList

  def reload(org: Org): Unit = conv.reload(self, org)

  def toOrigin: Org = conv.toOrigin(self)

}

object ReactiveModelOf {

  /**
   * ReactiveModelOfのコンパニオンに継承させると、RMCompatibleのgivenを用意します.
   * (givenはモデルの型ごとに具体的な型で置く必要があります.
   * 型パラメータで一般化したgivenでは、RMCompatible.toRM(a)などでモデルの型が推論できないため)
   */
  abstract class Companion[Org <: Product, Self <: ReactiveModelOf[Org, Self]](using conv: RMConversion[Org, Self]) {

    given rmCompatible: RMCompatible[Org, Self] = (o: Org) => conv.toReactiveModel(o)

    /** case classからモデルを作ります */
    def from(org: Org): Self = conv.toReactiveModel(org)

  }

}
