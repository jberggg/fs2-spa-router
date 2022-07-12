package `scalajs-fs2-router`

import cats.implicits._
import cats.effect.Async
import Domain.Page
import org.scalajs.dom.window
import cats.Monad
import fs2.concurrent.Channel
import fs2.Stream
import colibri.router.Path

trait RouterDsl[F[_]] {

    def requestedPageStream: Stream[F, Page]

    def navigate(to: Page): F[Unit]

}

object RouterDsl {

    def interpreter[ F[_] : Async ](pathChannel: Channel[F, Path]): RouterDsl[F] = new RouterDsl[F] {
           
        override def requestedPageStream: Stream[F,Page] = 
            pathChannel
            .streamAndRegisterEventListener
            .map( Domain.Page.fromPath(_) )
            
        override def navigate(to: Page): F[Unit] = for {
            _ <- Async[F].delay( window.location.hash = (Page.toPath(to).pathString) )
            _ <- pathChannel.send(Page.toPath(to))
        } yield ()

    }

    implicit class PathChannelSyntax[F[_] : Monad : Async ](c: Channel[F, Path]){

        def streamAndRegisterEventListener: Stream[F, Path] = Service.registerEventHandler[F](c)

    }

    def apply[F[_]](implicit ev: RouterDsl[F]): RouterDsl[F] = ev
  
}
