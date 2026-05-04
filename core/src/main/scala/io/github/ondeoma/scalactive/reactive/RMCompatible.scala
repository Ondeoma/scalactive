package io.github.ondeoma.scalactive.reactive

trait RMCompatible[A, M <: ReactiveModel[A, M]] {
  def toReactiveModel(a: A): M
}

object RMCompatible {

  def toRM[A, M <: ReactiveModel[A, M]](a: A)
                                       (using rmc: RMCompatible[A, M]): M = {
    rmc.toReactiveModel(a)
  }

  def toRMs[A, M <: ReactiveModel[A, M]](as: List[A])
                                        (using rmc: RMCompatible[A, M]): List[M] = {
    as.map(toRM)
  }

  def toListRM[A, M <: ReactiveModel[A, M]](as: List[A])
                                           (using rmc: RMCompatible[A, M]): ListRM[A, M] = {
    ListRM(toRMs(as))
  }

  object ext {

    extension [A, M <: ReactiveModel[A, M]](a: A)
                                           (using rmc: RMCompatible[A, M]) {
      def toRM: M = {
        RMCompatible.toRM(a)
      }
    }

    extension [A, M <: ReactiveModel[A, M]](as: List[A])
                                           (using rmc: RMCompatible[A, M]) {
      def toRMs: List[M] = {
        as.map(toRM)
      }

      def toListRM: ListRM[A, M] = {
        ListRM(toRMs)
      }
    }

  }

}


