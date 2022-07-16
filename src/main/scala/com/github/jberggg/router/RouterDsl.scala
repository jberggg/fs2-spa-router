package com.github.jberggg.router

import cats.effect.Async
import fs2.Stream
import fs2.concurrent.SignallingRef
import org.http4s.Uri.Path
import org.scalajs.dom.window

import Domain._

trait RouterDsl[F[_]] {

    def requestedPaths: Stream[F, Path]

    def navigate(to: Path): F[Unit]

}

object RouterDsl {
    def interpreter[ F[_] : Async ](pathSignal: SignallingRef[F, Path]): RouterDsl[F] = new RouterDsl[F] {
           
        override def requestedPaths: Stream[F,Path] = pathSignal.streamAndRegisterEventListener
            
        override def navigate(to: Path): F[Unit] = Async[F].delay( window.location.hash = (to.renderString) ) 

    }

    def apply[F[_]](implicit ev: RouterDsl[F]): RouterDsl[F] = ev
  
}
