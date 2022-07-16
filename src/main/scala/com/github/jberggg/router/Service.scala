package com.github.jberggg.router

import cats.syntax.all._
import cats.effect.kernel.{Async, Resource}
import cats.effect.std.Dispatcher
import org.http4s.Uri.Path
import fs2._
import fs2.concurrent.SignallingRef
import org.scalajs.dom.window
import org.scalajs.dom.HashChangeEvent
import scala.scalajs.js
import cats.Applicative

object Service {
  
    def setupInfrastructure[F[_] : Async ]: F[SignallingRef[F, Path]] = 
        Async[F].delay( window.location.hash.tail )
        .map( location => if(location ==="") "/" else location  )
        .flatMap( location => SignallingRef[F,Path]( Path.unsafeFromString(location) ) )

    private[router] def registerEventHandlerAndToStream[ F[_] : Async ](s: SignallingRef[F, Path]): Stream[F, Path] = for {
        dispatcher <- Stream.resource(Dispatcher[F])
        callback   = assembleCallback[F](s,dispatcher)
        _          <- Stream.resource( Resource.make( addHashChangeListener(callback) )( _ => removeHashChangeListener[F](callback) ) )
        paths      <- s.discrete
    } yield paths

    private def extractLocationHash(path: String): Option[String] = path.split("#").toList match {
        case _ :: afterHash :: Nil => afterHash.some
        case _ if path.endsWith("#") => "/".some
        case _ => None
    }

    private def assembleCallback[F[_] : Applicative ](s: SignallingRef[F, Path], d: Dispatcher[F]) = (
        (e: HashChangeEvent) => d.unsafeRunAndForget( 
            extractLocationHash(e.newURL)
            .map( locationHash => s.set(Path.unsafeFromString(locationHash)) )
            .getOrElse( ().pure[F] )
        )
    ): js.Function1[HashChangeEvent,Unit]

    private def addHashChangeListener[ F[_] : Async ](callback: js.Function1[HashChangeEvent,Unit]): F[Unit] =  
        Async[F].delay( window.addEventListener(
            `type` = "hashchange", 
            listener = callback,
            useCapture = true
        ))

    private def removeHashChangeListener[ F[_] : Async ](originalCallback: js.Function1[HashChangeEvent,Unit]): F[Unit] =
        Async[F].delay( window.removeEventListener(
            `type` = "hashchange", 
            listener = originalCallback
        ))
}
