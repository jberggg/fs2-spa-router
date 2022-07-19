package com.github.jberggg.router

import cats.syntax.all._
import cats.effect.kernel.{Async, Resource}
import cats.effect.std.Dispatcher
import org.http4s.Uri
import org.http4s.Uri.Path
import fs2.concurrent.SignallingRef
import org.scalajs.dom.window
import scala.scalajs.js
import org.scalajs.dom.PopStateEvent
import Domain._
import cats.NonEmptyParallel

object Service {
  
    def initialize[F[_] : Async : NonEmptyParallel ](initialState: HistoryState): Resource[F,RouterDsl[F]] =  for {
        d <- Dispatcher[F]
        s <- Resource.eval(setupSignal[F](initialState))
        _ <- Resource.make( assembleCallback[F](s,d).pure[F].flatTap(addHashChangeListener[F]) )( removeHashChangeListener[F] )
    } yield RouterDsl.interpreter(s)

    private def setupSignal[F[_] : Async](initialState: HistoryState): F[SignallingRef[F,Tuple2[Path,BrowserHistoryState]]] = 
        Async[F]
        .delay( window.location.href )
        .map( Uri.fromString(_).fold(
            _   => Tuple2(Path.Root,initialState), // TODO: How to deal with parsin errors!?
            uri => Tuple2(uri.path,initialState)
        ))
        .flatTap{ case (p,s) => Async[F].delay(window.history.replaceState(s.toJsObject,"",p.toString)) }
        .flatMap{ case (p,s) => SignallingRef.apply[F,Tuple2[Path,BrowserHistoryState]](Tuple2.apply(p,s.asRight[UnhandledHistoryState])) }        

    private def assembleCallback[F[_] : Async ](s: SignallingRef[F, Tuple2[Path, BrowserHistoryState]], d: Dispatcher[F]) = (
        (e: PopStateEvent) => d.unsafeRunAndForget{
            Async[F].delay(window.location.href)
            .map(
                Uri.fromString(_).fold(
                    _   => Tuple2(Path.Root,e.state), // TODO: How to deal with parsin errors!?
                    uri => Tuple2(uri.path,e.state)
                ) match {
                    case (p: Path, s: HistoryState) => Tuple2.apply(p,s.asRight[UnhandledHistoryState])
                    case (p: Path, unknown) => Tuple2.apply(p,UnhandledHistoryState(unknown).asLeft[HistoryState])
                }
            )
            .flatMap(s.set)
            .void
        }
    ): js.Function1[PopStateEvent,Unit]

    private def addHashChangeListener[ F[_] : Async ](callback: js.Function1[PopStateEvent,Unit]): F[Unit] =  
        Async[F].delay( window.addEventListener(
            `type` = "popstate", 
            listener = callback,
            useCapture = true
        ))

    private def removeHashChangeListener[ F[_] : Async ](originalCallback: js.Function1[PopStateEvent,Unit]): F[Unit] =
        Async[F].delay( window.removeEventListener(
            `type` = "popstate", 
            listener = originalCallback
        ))
}
