package com.github.jberggg.router

import scala.scalajs.js

trait HistoryApiStateDsl[S]{

    def fromHistoryApiState(s: js.Any): S

    def toHistoryApiState(s: S): js.Any
  
}

object HistoryApiStateDsl {

    implicit val defaultImplementation: HistoryApiStateDsl[js.Any] = new HistoryApiStateDsl[js.Any] {
        override def fromHistoryApiState(s: js.Any): js.Any = s
        override def toHistoryApiState(s: js.Any): js.Any = s
    }

}
