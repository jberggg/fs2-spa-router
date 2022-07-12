package com.github.jberggg.router

import cats.implicits._
import cats.effect.Async
import org.scalajs.dom.window
import cats.Monad
import fs2.concurrent.Channel
import fs2.Stream
import org.http4s.Uri.Path

trait RouterDsl[F[_]] {

    def requestedPageStream: Stream[F, Path]

    def navigate(to: Path): F[Unit]

}

object RouterDsl {

    def interpreter[ F[_] : Async ](pathChannel: Channel[F, Path]): RouterDsl[F] = new RouterDsl[F] {
           
        override def requestedPageStream: Stream[F,Path] = 
            pathChannel
            .streamAndRegisterEventListener
            
        override def navigate(to: Path): F[Unit] = for {
            _ <- Async[F].delay( window.location.hash = (to.renderString) )
            _ <- pathChannel.send(to)
        } yield ()

    }

    implicit class PathChannelSyntax[F[_] : Monad : Async ](c: Channel[F, Path]){

        def streamAndRegisterEventListener: Stream[F, Path] = Service.registerEventHandler[F](c)

    }

    def apply[F[_]](implicit ev: RouterDsl[F]): RouterDsl[F] = ev
  
}
