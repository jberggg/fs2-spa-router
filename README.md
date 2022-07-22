# fs2-spa-router

Simple scalajs router for Single Page web-applications based on *fs2* using 
*http4s-dsl*'s `Path` to calculate and represent routes.

```scala
"com.github.jberggg" %%% "fs2-spa-router" % "0.1.0-SNAPSHOT"
```

**Note**: CI/CD is not yet in place, you need to checkout the project and build 
it yourself with `sbt publishLocal`.

## How it works

The router is built upon the `History API` which allows to route on regular
looking paths rather then a path section delimited by a `#`. When navigating
from within the SPA app, the path and optional state is pushed to the `History API`.
When the user presses the back button, the router will receive the previous path
and state which then can be used to reconstruct the view.

## How to use

You can use the `RouterDSL` and `RouterService` to setup the routing like so:

```scala
import org.http4s._, org.http4s.dsl.io._, org.http4s.implicits._
import cats.syntax.all._
import com.github.jberggg.router.{RouterService, RouterDsl}
/* ... plus some more imports */

object MyApp {

    def run[F[_] : Monad : Async ]: F[ExitCode] =

        RouterService.createRouterResourcesStandalone[F].use{ pathSignal =>    
                    
          // or us the Signal directly by consuming it with `discrete` 
          // and add new path and state with `set`
          implicit val routerDslInterpreter = RouterDsl(pathSignal)

          render[F]

        }

    // highly app / framework specific, just for illustration
    private def render[F[_] : Monad : Async : RouterDsl ]: F[Unit] =

        RouterDsl[F]
        .requestedPaths
        .discrete
        .map( path =>

            path match {

                case Root               =>  // render HomeScreen
                case Root / "about"     =>  // render about page
                case Root / "profile"   =>  // render user profile
                case path               =>  // render warning that this is a unknown path

            }

        )

}

```

### Outwatch

If you want to use the router with [Outwatch](https://github.com/outwatch/outwatch) you
need to ensure, that you lift the resources into the same stream as the one that produces
your dynamic content. So you would first create the signal with `createPathSignal` and then
at the start of the content stream you will need to lift `createRouterResources` into
the stream with `Stream.resource`. This ensures that the event handler are not prematurely
terminated. A code example will follow...
