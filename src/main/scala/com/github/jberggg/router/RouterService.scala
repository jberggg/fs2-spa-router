package com.github.jberggg.router

import cats.syntax.all._
import cats.effect.syntax.all._
import cats.effect.kernel.{Async, Resource}
import cats.effect.std.Dispatcher
import org.http4s.Uri
import org.http4s.Uri.Path
import fs2.concurrent.SignallingRef
import org.scalajs.dom.window
import scala.scalajs.js
import org.scalajs.dom.PopStateEvent
import Domain._

object RouterService {
  
    def createSignalAndResources[F[_] : Async ](initialState: HistoryApiState): Resource[F,SignallingRef[F,Tuple2[Path,HistoryApiState]]] = for {
        s <- Resource.eval(createPathSignal[F](initialState))
        _ <- createHistoryApiResources[F](s)
    } yield s

    def createHistoryApiResources[F[_] : Async ](signal: SignallingRef[F,Tuple2[Path,HistoryApiState]]): Resource[F,Unit] = for {
        d <- Dispatcher[F]
        _ <- createHistoryPopEventListener[F](d,signal)
        _ <- createHistoryStatePusher[F](signal)
    } yield ()

    def createPathSignal[F[_] : Async ](initialState: HistoryApiState): F[SignallingRef[F,Tuple2[Path,HistoryApiState]]] = 
        Async[F]
        .delay( window.location.href )
        .map( Uri.unsafeFromString )
        .map( u => Tuple2(u.path, initialState) )
        .flatTap{ case (p,s) => Async[F].delay(window.history.replaceState(s,"",p.toString)) } // set initial state in History API
        .flatMap{ SignallingRef.apply[F,Tuple2[Path,HistoryApiState]] }

    def createHistoryStatePusher[ F[_] : Async ](s: SignallingRef[F,Tuple2[Path,HistoryApiState]]): Resource[F,Unit] = 
        s.discrete
        .evalMap{ case (p,st) => Async[F].delay( window.history.pushState( st, "", p.toString ) ) }
        .compile
        .drain
        .background
        .void

    def createHistoryPopEventListener[F[_] : Async](d: Dispatcher[F], s: SignallingRef[F,Tuple2[Path,HistoryApiState]]): Resource[F,Unit] = 
        Resource
        .make( assembleCallback[F](s,d).pure[F] )( removePopStateEventListener[F] )
        .evalMap( addPopStateEventListener[F] )

    private def assembleCallback[F[_] : Async ](s: SignallingRef[F, Tuple2[Path,HistoryApiState]], d: Dispatcher[F]) = ((e: PopStateEvent) => 
        d.unsafeRunAndForget{
            Async[F].delay(window.location.href)
            .map( Uri.unsafeFromString )
            .map( u => Tuple2(u.path,e.state) )
            .flatMap{ s.set }
            .void
        }): js.Function1[PopStateEvent,Unit]

    private def addPopStateEventListener[ F[_] : Async ](callback: js.Function1[PopStateEvent,Unit]): F[Unit] =  
        Async[F].delay( window.addEventListener(
            `type` = "popstate", 
            listener = callback,
            useCapture = true
        ))

    private def removePopStateEventListener[ F[_] : Async ](originalCallback: js.Function1[PopStateEvent,Unit]): F[Unit] =
        Async[F].delay( window.removeEventListener(
            `type` = "popstate", 
            listener = originalCallback
        ))
}
