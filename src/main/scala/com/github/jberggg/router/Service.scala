package com.github.jberggg.router

import cats.implicits._
import org.http4s.Uri.Path

import org.scalajs.dom.window
import cats.effect.std.Dispatcher
import org.scalajs.dom.Event
import fs2.concurrent.Channel
import fs2._
import cats.effect.kernel.{Async, Resource}

object Service {
  
    def setupInfrastructure[F[_] : Async ]: F[Channel[F, Path]] = for {
        channel     <- Channel.unbounded[F,Path]
        location    <- getCurrentLocation[F]
        _           <- channel.send(location)
    } yield channel

    private[router] def registerEventHandler[ F[_] : Async ](c: Channel[F, Path]): Stream[F, Path] = for {
        dispatcher <- Stream.resource(Dispatcher[F])
        callback   =  (_: Event) => dispatcher.unsafeRunAndForget( getCurrentLocation[F].flatMap( l => c.send(l) ) )
        _          <- Stream.resource(Resource.make(addHashChangeListener[F](callback))(_ => removeHashChangeListener[F](callback)))
        paths      <- c.stream
    } yield paths

    private def getCurrentLocation[F[_] : Async]: F[Path] = Async[F].delay(Path.unsafeFromString(window.location.hash.tail))

    private def addHashChangeListener[ F[_] : Async ](callback: Event => Unit): F[Unit] =  
        Async[F].delay( window.addEventListener(
            `type` = "hashchange", 
            listener = callback,
            useCapture = true
        ))

    private def removeHashChangeListener[ F[_] : Async ](originalCallback: Event => Unit): F[Unit] =
        Async[F].delay( window.removeEventListener(
            `type` = "hashchange", 
            listener = originalCallback
        ))
}
