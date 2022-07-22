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

object Service {
  
    def createRouterDsl[F[_] : Async ](initialState: js.Any): Resource[F,RouterDsl[F]] = for {
        s <- Resource.eval(createPathSignal[F](initialState))
        _ <- createRouterResources[F](s)
    } yield RouterDsl.interpreter(s)

    def createPathSignal[F[_] : Async ](initialState: js.Any): F[SignallingRef[F,Tuple2[Path,js.Any]]] = 
        Async[F]
        .delay( window.location.href )
        .map( Uri.unsafeFromString )
        .map( u => Tuple2(u.path, initialState) )
        .flatTap{ case (p,s) => Async[F].delay(window.history.replaceState(s.toJsObject,"",p.toString)) } // set initial state in History API
        .flatMap{ SignallingRef.apply[F,Tuple2[Path,js.Any]] }

    def createRouterResources[F[_] : Async ](signal: SignallingRef[F,Tuple2[Path,js.Any]]): Resource[F,Unit] = for {
        d <- Dispatcher[F]
        _ <- registerEventListener[F](d,signal)
        _ <- createStatePusher[F](signal)
    } yield ()

    def createStatePusher[ F[_] : Async ](s: SignallingRef[F,Tuple2[Path,js.Any]]): Resource[F,Unit] = 
        s.discrete
        .evalMap{ case (p,st) => Async[F].delay( window.history.pushState( st.toJsObject, "", p.toString ) ) }
        .compile
        .drain
        .background
        .void

    def registerEventListener[F[_] : Async](d: Dispatcher[F], s: SignallingRef[F,Tuple2[Path,js.Any]]): Resource[F,Unit] = 
        Resource
        .make( assembleCallback[F](s,d).pure[F] )( removeHashChangeListener[F] )
        .evalMap( addHashChangeListener[F] )

    private def assembleCallback[F[_] : Async ](s: SignallingRef[F, Tuple2[Path, js.Any]], d: Dispatcher[F]) = (
        (e: PopStateEvent) => d.unsafeRunAndForget{
            Async[F].delay(window.location.href)
            .map( Uri.unsafeFromString )
            .map( u => Tuple2(u.path,e.state) )
            .flatMap{
                case t@(_, _: Object) => s.set(t)
                case (_,_) => Async[F].delay( println(s"Unexpected history state encountered..."))
            }
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
