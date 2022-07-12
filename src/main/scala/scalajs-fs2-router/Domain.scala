package `scalajs-fs2-router`

import colibri.router.{Root, Path, /}
import java.util.UUID
import scala.util.Try

object Domain {
  
    object Page {

        case object Home extends Page
        final case class RecipeViewer(recipeId: UUID) extends Page
        final case class RecipeEditor(recipeId: UUID) extends Page

        def fromPath(p: Path): Page = p match {
            case Root                           => Page.Home
            // TODO: handle invalid UUIDs
            case Root / "editor" / UUIDVar(id)  => Page.RecipeEditor(id)
            case Root / "recipe" / UUIDVar(id)  => Page.RecipeViewer(id)
        }

        def toPath(p: Page): Path = p match {
            case Page.Home              => Root
            case Page.RecipeEditor(id)  => Root / "editor" / id.toString()
            case Page.RecipeViewer(id)  => Root / "recipe" / id.toString()
        }
    }

    sealed trait Page {

        import Page._

        final def fold[X]( 
            onHome: => X, 
            onRecipeViewer: UUID => X, 
            onRecipeEditor: UUID => X 
        ): X = this match {
            case Home => onHome
            case RecipeViewer(id) => onRecipeViewer(id)
            case RecipeEditor(id) => onRecipeEditor(id)
        }
    }

    object UUIDVar {
        def unapply(str: String): Option[UUID] = {
            Try(
                UUID.fromString(str)
            ).toOption 
        }
    }


}
