package com.github.jberggg.router

import cats.syntax.all._
import cats.effect.Async
import org.scalajs.dom.window
import fs2.concurrent.Channel
import fs2.Stream
import org.http4s.Uri.Path

import Domain._

trait RouterDsl[F[_]] {

    def requestedPaths: Stream[F, Path]

    def navigate(to: Path): F[Unit]

}

object RouterDsl {
    def interpreter[ F[_] : Async ](pathChannel: Channel[F, Path]): RouterDsl[F] = new RouterDsl[F] {
           
        override def requestedPaths: Stream[F,Path] = pathChannel.streamAndRegisterEventListener
            
        override def navigate(to: Path): F[Unit] =
            Async[F].delay( window.location.hash = (to.renderString) ) *>
            pathChannel.send(to).void

    }

    def apply[F[_]](implicit ev: RouterDsl[F]): RouterDsl[F] = ev
  
}
