package com.github.jberggg.router

import fs2.concurrent.SignallingRef
import org.http4s.Uri.Path
import scala.scalajs.js

trait RouterDsl[F[_]] {

    def requestedPaths: SignallingRef[F, Tuple2[Path,js.Any]]

    def navigate(to: Path, withState: js.Any): F[Unit] = requestedPaths.set(Tuple2(to,withState))

}

object RouterDsl {
    def interpreter[ F[_] ](pathSignal: SignallingRef[F, Tuple2[Path,js.Any]]): RouterDsl[F] = new RouterDsl[F] {
           
        override def requestedPaths: SignallingRef[F, Tuple2[Path,js.Any]] = pathSignal

    }

    def apply[F[_]](implicit ev: RouterDsl[F]): RouterDsl[F] = ev
  
}
