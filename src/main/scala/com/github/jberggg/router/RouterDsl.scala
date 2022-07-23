package com.github.jberggg.router

import fs2.concurrent.SignallingRef
import org.http4s.Uri.Path
import Domain._

trait RouterDsl[F[_]] {

    def requestedPaths: SignallingRef[F, Tuple2[Path,HistoryApiState]]

    def navigate(to: Path, withState: HistoryApiState): F[Unit] = requestedPaths.set(Tuple2(to,withState))

}

object RouterDsl {
    def interpreter[ F[_] ](pathSignal: SignallingRef[F, Tuple2[Path,HistoryApiState]]): RouterDsl[F] = new RouterDsl[F] {
           
        override def requestedPaths: SignallingRef[F, Tuple2[Path,HistoryApiState]] = pathSignal

    }

    def apply[F[_]](implicit ev: RouterDsl[F]): RouterDsl[F] = ev
  
}
