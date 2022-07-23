package com.github.jberggg.router

import scala.scalajs.js.{Object => JsObject}

package object syntax {

    implicit class ObjectSyntax(self: Object){
        def toJsObject: JsObject = JsObject.apply(self)
    }

}
