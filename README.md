# fs2-spa-router

Simple scalajs router for Single Page web-applications based on *fs2* using 
*http4s-dsl*'s `Path` to calculate and represent routes.


```scala
    "com.github.jberggg" %%% "fs2-spa-router" % "0.1.0-SNAPSHOT"
```

**Note**: CI/CD is not yet in place, you need to checkout the project and build 
it yourself with `sbt publishLocal`.

You can use the `RouterDSL` and `Service` to setup the routing like so:

```scala
import com.github.jberggg.router.{Service => RouterService, RouterDsl}
import org.http4s._, org.http4s.dsl.io._, org.http4s.implicits._
import cats.implicits._
/* ... plus some more imports */

object MyApp {

    def run[F[_] : Monad : Async ]: F[ExitCode] =

        RouterService.setupInfrastructure[F].flatMap{ pathChannel =>    
                    
          implicit val routerDslInterpreter = RouterDsl.interpreter(pathChannel)

          render[F]

        }

    // highly app / framework specific, just for illustration
    private def render[F[_] : Monad : Async : RouterDsl ]: F[Unit] =

        RouterDsl[F]
        .requestedPaths
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