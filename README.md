# fs2-spa-router

Scalajs router for Single Page web-applications based on *fs2* using 
*http4s-dsl*'s `Uri` and `Path` to calculate and represent routes.

```scala
"com.github.jberggg" %%% "fs2-spa-router" % "0.1.0-SNAPSHOT"
```

**Note**: CI/CD is not yet in place, you need to checkout the project and build 
it yourself with `sbt publishLocal`.

## How it works

The router is built upon the [*History API*](https://developer.mozilla.org/en-US/docs/Web/API/History) 
which allows to route on regular looking paths rather then a path section delimited
by a `#`. 

When navigating from within the SPA app, the path and state is pushed to the *History API*. 
When the user presses the back button, the router will receive the previous path and state
from the *History API* which then can be used  to reconstruct the view the user wants to navigate
back to. The library makes no assumption about the state pushed to and received from the *History Api*. 
Thus it can be anything you want [as long as](https://developer.mozilla.org/en-US/docs/Web/API/History_API/Working_with_the_History_API):

> The state object can be anything that can be serialized.

## How to use

You can use the `RouterService` to setup the routing like so:

```scala
import org.http4s._, org.http4s.dsl.io._, org.http4s.implicits._
import cats.syntax.all._
import com.github.jberggg.router.{RouterService, RouterDsl}
import com.github.jberggg.router.syntax._
/* ... plus some more imports */

object MyApp {

    trait MyAppState
    object EmptyState extends MyAppState
    case class CreateFormState(name: String, address: String) extends MyAppState
    // and so on...

    def run[F[_] : Monad : Async ]: F[ExitCode] =

        RouterService.createSignalAndResources[F](EmptyState.toJsObject).use{ pathSignal =>    
                    
          // ... or us the Signal directly by consuming it with `discrete` 
          // and add new path and state with `set`
          implicit val routerDslInterpreter = RouterDsl(pathSignal)

          render[F]

        }

    // highly app / framework specific, just for illustration
    private def render[F[_] : Monad : Async : RouterDsl ]: F[Unit] =

        RouterDsl[F]
        .requestedPaths
        .discrete
        .map{ case (path,state) =>

            path match {

                case Root               =>  // render HomeScreen
                case Root / "about"     =>  // render about page
                case Root / "profile"   =>  // render user profile
                case path               =>  // render warning that this is a unknown path

            }

        }

}

```

### Outwatch

If you want to use the router with [Outwatch](https://github.com/outwatch/outwatch) you
need to ensure, that you lift the resources into the same stream as the one that produces
your dynamic content. You would first create the signal with `createPathSignal` and then -
at the start of the content stream - you would lift `createRouterResources` into
the stream with `Stream.resource` followed by the content stream itself.
This ensures that the event handler are not prematurely terminated.

```scala
RouterService.createPathSignal[F](EmptyState.toJsObject).flatMap( signal =>

    implicit val routerDslInterpreter = RouterDsl.interpreter(s)

    val requestedPaths: Stream[F,Tuple2[Path,js.Any]] = for {
            _ <- Stream.resource(RouterService.createHistoryApiResources[F](s))
            r <- routerDslInterpreter.requestedPaths.discrete
    } yield r

    // and then use the requestedPaths stream where you want to render
    // the dynamic content of your app
)
```
