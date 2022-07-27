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

object RouterService {
  
    def createSignalAndResources[F[_] : Async, S ](initialState: S)(implicit hasd: HistoryApiStateDsl[S]): Resource[F,SignallingRef[F,Tuple2[Path,S]]] = for {
        s <- Resource.eval(createPathSignal[F,S](initialState))
        _ <- createHistoryApiResources[F,S](s)
    } yield s

    def createHistoryApiResources[F[_] : Async, S ](signal: SignallingRef[F,Tuple2[Path,S]])(implicit hasd: HistoryApiStateDsl[S]): Resource[F,Unit] = for {
        d <- Dispatcher[F]
        _ <- createHistoryPopEventListener[F,S](d,signal)
        _ <- createHistoryStatePusher[F,S](signal)
    } yield ()

    def createPathSignal[F[_] : Async, S ](initialState: S)(implicit hasd: HistoryApiStateDsl[S]): F[SignallingRef[F,Tuple2[Path,S]]] = 
        Async[F]
        .delay( window.location.href )
        .map( Uri.unsafeFromString )
        .map( u => Tuple2(u.path, initialState) )
        .flatTap{ case (p,s) => Async[F].delay(window.history.replaceState(hasd.toHistoryApiState(s),"",p.toString)) } // set initial state in History API
        .flatMap{ SignallingRef.apply[F,Tuple2[Path,S]] _ }

    def createHistoryStatePusher[ F[_] : Async, S ](s: SignallingRef[F,Tuple2[Path,S]])(implicit hasd: HistoryApiStateDsl[S]): Resource[F,Unit] = 
        s.discrete
        .evalMap{ case (p,st) => Async[F].delay( window.history.pushState( hasd.toHistoryApiState(st), "", p.toString ) ) }
        .compile
        .drain
        .background
        .void

    def createHistoryPopEventListener[F[_] : Async, S](d: Dispatcher[F], s: SignallingRef[F,Tuple2[Path,S]])(implicit hasd: HistoryApiStateDsl[S]): Resource[F,Unit] = 
        Resource
        .make( assembleCallback[F,S](s,d).pure[F] )( removePopStateEventListener[F] )
        .evalMap( addPopStateEventListener[F] )

    private def assembleCallback[F[_] : Async, S ](s: SignallingRef[F, Tuple2[Path,S]], d: Dispatcher[F])(implicit hasd: HistoryApiStateDsl[S]) = ((e: PopStateEvent) => 
        d.unsafeRunAndForget{
            Async[F].delay(window.location.href)
            .map( Uri.unsafeFromString )
            .map( u => Tuple2(u.path,hasd.fromHistoryApiState(e.state)) )
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
