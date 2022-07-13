package com.github.jberggg.router

import cats.effect.kernel.Async
import cats.Monad
import fs2.concurrent.Channel
import fs2.Stream
import org.http4s.Uri.Path

object Domain {

    implicit class PathChannelSyntax[F[_] : Monad : Async ](c: Channel[F, Path]){

        def streamAndRegisterEventListener: Stream[F, Path] = Service.registerEventHandlerAndToStream[F](c)

    }
  
}
