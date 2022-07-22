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
        d <- Dispatcher[F]
        s <- Resource.eval(setupSignal[F](initialState))
        _ <- registerEventListener[F](d,s)
        _ <- setupStatePusher[F](s)
    } yield RouterDsl.interpreter(s)

    def setupSignal[F[_] : Async ](initialState: js.Any): F[SignallingRef[F,Tuple2[Path,js.Any]]] = 
        Async[F]
        .delay( window.location.href )
        .map( Uri.unsafeFromString )
        .map( u => Tuple2(u.path, initialState) )
        .flatTap{ case (p,s) => Async[F].delay(window.history.replaceState(s.toJsObject,"",p.toString)) }
        .flatMap{ case (p,s) => SignallingRef.apply[F,Tuple2[Path,js.Any]](Tuple2.apply(p,s)) }

    def setupStatePusher[ F[_] : Async ](s: SignallingRef[F,Tuple2[Path,js.Any]]): Resource[F,Unit] = 
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
