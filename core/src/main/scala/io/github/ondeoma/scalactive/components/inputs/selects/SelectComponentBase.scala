package io.github.ondeoma.scalactive.components.inputs.selects

import io.github.ondeoma.scalactive.components.{BaseComponent, ComponentManager}
import io.github.ondeoma.scalactive.models.AddMethod.append
import io.github.ondeoma.scalactive.reactive.{CRV, RV, Reactive}
import org.scalajs.dom.*


trait SelectComponentBase extends BaseComponent {

  type Select = (SelectValue, SelectDisplayName)

  protected def genElement[A](selects: Reactive[List[Select]] | List[Select],
                              rv: RV[A],
                              toSV: A => SelectValue,
                              fromSV: SelectValue => A): GenResult = {
    ComponentManager { implicit cm =>
      val (_for, ssV) = selects match {
        case ss: RV[List[Select]] =>
          (forC(ss), ss.v.map(_._1))
        case ss: CRV[List[Select]] =>
          (forC(ss), ss.v.map(_._1))
        case ss: List[(SelectValue, SelectDisplayName)] =>
          (forC(ss), ss.map(_._1))
        case _ => ???
      }

      // 選択肢変更時に元の値がなければ先頭の値を選択
      val sv = toSV(rv.v)
      val fixedSv =
        if (ssV.contains(sv)) sv
        else ssV.headOption.getOrElse("")
      rv := fromSV(fixedSv)

      val options = _for { implicit (cm, s, _) =>
        val selected = if (s._1 == fixedSv) "selected" else ""
        // language=html
        s"""<option value="${s._1}" $selected>${s._2}</option>"""
      }
      // language=html
      s"""<select ${ev(EventType.input, _.ifSelect(ele => rv := fromSV(ele.value)))}>
         |    $options
         |</select>""".stripMargin
    }
  }

}
