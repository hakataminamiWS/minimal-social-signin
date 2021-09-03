package controllers

import javax.inject.Inject
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

/** The `Sign In` controller.
  */
class SignInController @Inject() (
    scc: SilhouetteControllerComponents,
    signIn: views.html.signIn
)(implicit ex: ExecutionContext)
    extends SilhouetteController(scc) {

  /** Views the `Sign In` page.
    *
    * @return
    *   The result to display.
    */
  def view = UnsecuredAction.async { implicit request: Request[AnyContent] =>
    Future.successful(Ok(signIn(socialProviderRegistry)))
  }

}
