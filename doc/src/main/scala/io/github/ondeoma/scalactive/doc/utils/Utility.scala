package io.github.ondeoma.scalactive.doc.utils

import org.scalajs.dom.document.location

object Utility {

  def esc(c: String): String = {
    c.replaceAll("<", "&lt;")
      .replaceAll("<", "&gt;")
  }

  def goToAnchor(): Unit = {
    location.hash = location.hash
  }
  
}
