package io.github.ondeoma.scalactive.components.fors

import cats.syntax.all.*
import io.github.ondeoma.scalactive.components.BaseComponent
import org.scalajs.dom.*

object ForGeneralComponent extends BaseComponent {

  def apply[A, B](root: HTMLElement,
                  am: AddMethod,
                  values: A,
                  toList: A => List[B],
                  watch: NodesComponentController => WatchInfos)
                 (genHtml: (CM, B, IDX) => HTML): NodesComponentController = {
    NodesComponentController { c =>
      for {
        e_cs <- toList(values).zipWithIndex.traverse { (rv, i) => ComponentManager(genHtml(_, rv, i)) }
        ns = e_cs.flatMap(_._1)
        children = e_cs.flatMap(_._2)
        tmpRs = e_cs.flatMap(_._3)
        _ <- addNodes(c.parent.getOrElse(root))(am, ns *).toRight(addNodesErrorMessage)
      } yield {
        c.nodes = ns
        c.children = children
        c.watchInfos = watch(c)
        c.tmpReactives = tmpRs
        c 
      }
    }
  }

}
 
