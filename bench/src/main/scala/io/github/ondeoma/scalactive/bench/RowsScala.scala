package io.github.ondeoma.scalactive.bench

import io.github.ondeoma.scalactive.components.*

/**
 * Scala API(forEachC/clsIf/evClick)で書いた表. RowsSt.stと同じDOMを作ります.
 */
object RowsScala extends StatelessComponent {

  def genHtml(implicit cm: ComponentManager): HTML = {
    // 添字を使わないのでforEachC(.stのv-forも添字の変数がなければforEachCになる)
    val rows = forEachC(Store.rows) { implicit (cm, row) =>
      val id = row.v.id
      // language=html
      s"""<tr ${clsIf(Store.selected.mapC(_ == id), "danger")}>
         |<td class="id">$id</td>
         |<td class="label"><a ${evClick(Store.selected := id)}>${%(TextComponent(row)(_.label))}</a></td>
         |<td><a class="remove" ${evClick(Store.rows.rm(Store.rows.v.indexWhere(_.id == id)))}>x</a></td>
         |</tr>""".stripMargin
    }
    // language=html
    s"""<table class="rows"><tbody id="tbody">$rows</tbody></table>"""
  }

}
