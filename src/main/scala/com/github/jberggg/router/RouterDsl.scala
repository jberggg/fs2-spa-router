package com.github.jberggg.router

import cats.syntax.all._
import cats.effect.Async
import fs2.concurrent.SignallingRef
import org.http4s.Uri.Path
import org.scalajs.dom.window

import Domain._
import cats.NonEmptyParallel

trait RouterDsl[F[_]] {

    def requestedPaths: SignallingRef[F, Tuple2[Path,BrowserHistoryState]]

    def  navigate(to: Path, withState: HistoryState): F[Unit]

}

object RouterDsl {
    def interpreter[ F[_] : Async : NonEmptyParallel ](pathSignal: SignallingRef[F, Tuple2[Path,BrowserHistoryState]]): RouterDsl[F] = new RouterDsl[F] {
           
        override def requestedPaths: SignallingRef[F, Tuple2[Path,BrowserHistoryState]] = pathSignal
            
        override def navigate(to: Path, withState: HistoryState): F[Unit] = 
            Async[F].delay( window.history.pushState( withState.toJsObject, "", to.toString ) ) &>
            pathSignal.set(to,withState.asRight).void

    }

    def apply[F[_]](implicit ev: RouterDsl[F]): RouterDsl[F] = ev
  
}
