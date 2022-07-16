package com.github.jberggg.router

import cats.effect.kernel.Async
import cats.Monad
import fs2.Stream
import fs2.concurrent.SignallingRef
import org.http4s.Uri.Path

object Domain {

    implicit class PathChannelSyntax[F[_] : Monad : Async ](c: SignallingRef[F, Path]){

        def streamAndRegisterEventListener: Stream[F, Path] = Service.registerEventHandlerAndToStream[F](c)

    }
  
}
