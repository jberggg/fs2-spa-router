package com.github.jberggg.router

import fs2.concurrent.SignallingRef
import org.http4s.Uri.Path

abstract class RouterDsl[F[_], S ] {

    def requestedPaths: SignallingRef[F, Tuple2[Path,S]]
    
    def navigate(to: Path, withState: S): F[Unit] = requestedPaths.set(Tuple2(to,withState))

}

object RouterDsl {
    def interpreter[ F[_] , S ](pathSignal: SignallingRef[F, Tuple2[Path,S]]): RouterDsl[F,S] = new RouterDsl[F, S] {
           
        override def requestedPaths: SignallingRef[F, Tuple2[Path,S]] = pathSignal

    }

    def apply[F[_],S](implicit ev: RouterDsl[F,S]): RouterDsl[F,S] = ev
  
}
