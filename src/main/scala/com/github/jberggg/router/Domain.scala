package com.github.jberggg.router

import scala.scalajs.js.{Object => JsObject, Any => JsAny}

object Domain {

    type HistoryApiState = JsAny

    implicit class ObjectSyntax(self: Object){
        def toJsObject: JsObject = JsObject.apply(self)
    }
  
}
