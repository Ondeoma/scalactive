package io.github.ondeoma.scalactive.reactive

import io.github.ondeoma.scalactive.reactive.*

import scala.deriving.Mirror
import scala.quoted.*

object ReactiveModelMacro {

  private val basePackage = "io.github.ondeoma.scalactive.reactive"
  private val RMCompatible = s"${basePackage}.RMCompatible"
  private val ListRV = s"${basePackage}.ListRV"

  val collectReactivesPF: PartialFunction[Any, Reactive[?]] = {
    case r: Reactive[?] => r
  }

  val collectReactiveModelsPF: PartialFunction[Any, ReactiveModel[?, ?]] = {
    case rm: ReactiveModel[?, ?] => rm
  }

  inline def collectReactives[A <: Product](a: A)
                                           (using m: Mirror.ProductOf[A]): List[Reactive[?]] = {
    Tuple.fromProductTyped(a).toList.collect(collectReactivesPF)
  }

  inline def collectReactiveModels[A <: Product](a: A)
                                                (using m: Mirror.ProductOf[A]): List[ReactiveModel[?, ?]] = {
    Tuple.fromProductTyped(a).toList.collect(collectReactiveModelsPF)
  }

  inline def genToOrigin[O <: Product, RM <: ReactiveModel[O, RM]](inline rm: RM): O = {
    ${ genToOriginImpl[O, RM]('rm) }
  }

  private def genToOriginImpl[O: Type, RM: Type](rm: Expr[RM])
                                                (using Quotes): Expr[O] = {
    import quotes.reflect.*

    val rmType: TypeRepr = TypeRepr.of[RM].dealias
    val rmFields: List[Symbol] = rmType.typeSymbol.caseFields
    val orgType: TypeRepr = TypeRepr.of[O].dealias
    val orgFields: List[Symbol] = orgType.typeSymbol.caseFields

    val argsForConstructor: List[Term] = orgFields.map { orgField =>
      val name = orgField.name
      rmFields.find(_.name == name) match {
        case Some(rmField) =>
          val rmFType: TypeRepr = rmType.memberType(rmField)
          val orgFieldType: TypeRepr = orgType.memberType(orgField)
          val (isRV, isRM, isListRV, isListRM) = checkReactiveType(rmFType)
          val rmFTerm = Select.unique(rm.asTerm, name)

          if (isRV || isListRM || isListRV) {
            Select.unique(rmFTerm, "v")
          } else if (isRM) {
            Select.unique(rmFTerm, "toOrigin")
          } else rmFTerm

        case None =>
          report.errorAndAbort(
            s"'${TypeRepr.of[RM].show} type does not have '${orgField.name}."
          )
      }
    }
    val ctor = Select.overloaded(New(TypeTree.of[O]), "<init>", Nil, argsForConstructor)
    val expr = ctor.asExprOf[O]
    // report.info(expr.show)
    expr
  }

  /**
   * ReactiveModelのフィールドにListRM, ListRVが存在しない場合は利用可能
   */
  inline def genReload[O <: Product, RM <: ReactiveModel[O, RM]](inline rm: RM,
                                                                 inline o: O): Unit = {
    ${ genReloadImpl[O, RM]('rm, 'o) }
  }

  private def genReloadImpl[O: Type, RM: Type](rm: Expr[RM],
                                               o: Expr[O])
                                              (using Quotes): Expr[Unit] = {
    import quotes.reflect.*

    val rmType: TypeRepr = TypeRepr.of[RM].dealias
    val rmFields: List[Symbol] = rmType.typeSymbol.caseFields
    val orgType: TypeRepr = TypeRepr.of[O].dealias
    val orgFields: List[Symbol] = orgType.typeSymbol.caseFields
    val rmCompatible: Ref = Ref(Symbol.requiredModule(RMCompatible))

    val exprs: List[Expr[Unit]] = rmFields.flatMap { rmField =>
      val name = rmField.name
      orgFields.find(_.name == name).map { orgField =>
        val rmFType: TypeRepr = rmType.memberType(rmField)
        val orgFieldType: TypeRepr = orgType.memberType(orgField)
        val orgFieldSelect: Select = Select.unique(o.asTerm, name)
        val rmFTerm = Select.unique(rm.asTerm, name)
        val oFTerm = Select.unique(o.asTerm, name)
        val (isRV, isRM, isListRV, isListRM) = checkReactiveType(rmFType)

        if (isRM) {
          Select.overloaded(rmFTerm, "reload", Nil, List(oFTerm)).asExprOf[Unit]
        } else if (isListRM) {
          val orgTypeInList = orgFieldType.dealias.typeArgs.head // List[ここ]
          val rnTypeInListRM2 = rmFType.dealias.typeArgs(1) // ListRM[?, ここ]
          val toRMsT = Select.overloaded(rmCompatible, "toRMs", List(orgTypeInList, rnTypeInListRM2), List(orgFieldSelect))
          Apply(Select.unique(rmFTerm, ":="), List(toRMsT)).asExprOf[Unit]
        } else if (isListRV) {
          Apply(Select.unique(rmFTerm, "::="), List(oFTerm)).asExprOf[Unit]
        } else if (isRV) {
          Apply(Select.unique(rmFTerm, ":="), List(oFTerm)).asExprOf[Unit]
        } else {
          '{ () }
        }
      }
    }

    val expr = Expr.block(exprs, '{ () })
    // report.info(expr.show)
    expr
  }

  inline def genToReactiveModel[O <: Product, RM <: ReactiveModel[O, RM]](inline o: O): RM = {
    ${ genToReactiveModelImpl[O, RM]('o) }
  }

  private def genToReactiveModelImpl[O: Type, RM: Type](o: Expr[O])
                                                       (using Quotes): Expr[RM] = {
    import quotes.reflect.*

    val rmType: TypeRepr = TypeRepr.of[RM].dealias
    val rmFields: List[Symbol] = rmType.typeSymbol.caseFields
    val orgType: TypeRepr = TypeRepr.of[O].dealias
    val orgFields: List[Symbol] = orgType.typeSymbol.caseFields
    val rmCompatible: Ref = Ref(Symbol.requiredModule(RMCompatible))
    val listRV: Ref = Ref(Symbol.requiredModule(ListRV))

    val argsForConstructor: List[Term] = rmFields.flatMap { rmField =>
      val name = rmField.name
      orgFields.find(_.name == name).map { orgField =>
        val rmFType: TypeRepr = rmType.memberType(rmField)
        val orgFieldType: TypeRepr = orgType.memberType(orgField)
        val orgFieldSelect: Select = Select.unique(o.asTerm, name)
        val (isRV, isRM, isListRV, isListRM) = checkReactiveType(rmFType)

        if (isRM) {
          Select.overloaded(rmCompatible, "toRM", List(orgFieldType, rmFType), List(orgFieldSelect))
        } else if (isListRM) {
          val orgTypeInList = orgFieldType.dealias.typeArgs.head // List[ここ]
          val rnTypeInListRM2 = rmFType.dealias.typeArgs(1) // ListRM[?, ここ]
          Select.overloaded(rmCompatible, "toListRM", List(orgTypeInList, rnTypeInListRM2), List(orgFieldSelect))
        } else if (isListRV) {
          val orgTypeInList = orgFieldType.dealias.typeArgs.head // List[ここ]
          Select.overloaded(listRV, "toListRV", List(orgTypeInList), List(orgFieldSelect))
        } else if (isRV) {
          Select.overloaded(New(TypeTree.of[RV]), "<init>", List(orgFieldType), List(orgFieldSelect))
        } else {
          report.errorAndAbort(s"'${TypeRepr.of[RM].show} can't have non reactive fields.")
        }

      }
    }
    val ctor = Select.overloaded(New(TypeTree.of[RM]), "<init>", Nil, argsForConstructor)
    val expr = ctor.asExprOf[RM]
    // report.info(expr.show)
    expr
  }

  private type IsRV = Boolean
  private type IsRM = Boolean
  private type IsListRV = Boolean
  private type IsListRM = Boolean

  private def checkReactiveType(typeRepl: Any)
                               (using Quotes): (IsRV, IsRM, IsListRV, IsListRM) = {
    import quotes.reflect.*
    val t = typeRepl.asInstanceOf[TypeRepr]
    val s = t.typeSymbol
    val isRV = s == TypeRepr.of[RV[?]].typeSymbol
    val isRM = t <:< TypeRepr.of[ReactiveModel[?, ?]]
    val isListRV = s == TypeRepr.of[ListRV[?]].typeSymbol
    val isListRM = s == TypeRepr.of[ListRM[?, ?]].typeSymbol
    (isRV, isRM, isListRV, isListRM)
  }

}

 
