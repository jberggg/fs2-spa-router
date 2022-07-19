package com.github.jberggg.router

import scala.scalajs.js.{Object => JsObject}

object Domain {

    type BrowserHistoryState = Either[UnhandledHistoryState,HistoryState]

    trait HistoryState

    final case class UnhandledHistoryState(state: Any)

    implicit class ObjectSyntax(self: Object){
        def toJsObject: JsObject = JsObject.apply(self)
    }
  
}
