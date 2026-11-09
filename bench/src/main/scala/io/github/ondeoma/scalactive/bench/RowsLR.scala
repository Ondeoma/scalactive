package io.github.ondeoma.scalactive.bench

import io.github.ondeoma.scalactive.components.*

/**
 * RowsScalaのReactiveList版. RVListとReactiveListの性能を比べるために使います.
 * 行は値(Row)なので、ラベルはそのまま埋め込みます(行の値が変われば、その行が作り直されます).
 */
object RowsLR extends StatelessComponent {

  def genHtml(implicit cm: ComponentManager): HTML = {
    val rows = forEachC(Store.lrows) { implicit (cm, row) =>
      val id = row.id
      // language=html
      s"""<tr ${clsIf(Store.selected.mapC(_ == id), "danger")}>
         |<td class="id">$id</td>
         |<td class="label"><a ${evClick(Store.selected := id)}>${row.label}</a></td>
         |<td><a class="remove" ${evClick(Store.lrows.rm(Store.lrows.v.indexWhere(_.id == id)))}>x</a></td>
         |</tr>""".stripMargin
    }
    // language=html
    s"""<table class="rows"><tbody id="tbody">$rows</tbody></table>"""
  }

}
