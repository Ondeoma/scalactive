package io.github.ondeoma.scalactive.components.fors

import cats.syntax.all.*
import io.github.ondeoma.scalactive.components.{BaseComponent, ComponentManager}
import org.scalajs.dom.*
import io.github.ondeoma.scalactive.utils.DomUtil.*
import io.github.ondeoma.scalactive.utils.TypeAlias.*
import io.github.ondeoma.scalactive.controllers.HtmlElementsComponentController
import io.github.ondeoma.scalactive.models.AddMethod

object ForGeneralComponent extends BaseComponent {

  def apply[A, B](root: HTMLElement,
                  am: AddMethod,
                  values: A,
                  toList: A => List[B],
                  watch: HtmlElementsComponentController => WatchInfos)
                 (genHtml: (ComponentManager, B, IDX) => HTML): HtmlElementsComponentController = {
    HtmlElementsComponentController { c =>
      for {
        e_cs <- toList(values).zipWithIndex.traverse { (rv, i) => ComponentManager(genHtml(_, rv, i)) }
        eles = e_cs.flatMap(_._1)
        children = e_cs.flatMap(_._2)
        tmpRs = e_cs.flatMap(_._3)
        _ <- addNodes(root)(am, eles *).toRight(addNodesErrorMessage)
      } yield {
        c.elements = eles
        c.children = children
        c.watchInfos = watch(c)
        c.tmpReactives = tmpRs
        c
      }
    }
  }

}
 
