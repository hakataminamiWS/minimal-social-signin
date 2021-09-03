package controllers

import com.mohiva.play.silhouette.api.LogoutEvent
import com.mohiva.play.silhouette.api.actions._
import javax.inject.Inject
import play.api.mvc._
import utils.route.Calls

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

/** The basic application controller.
  */
class ApplicationController @Inject() (
    scc: SilhouetteControllerComponents,
    authenticated: views.html.authenticated
)(implicit ex: ExecutionContext, assets: AssetsFinder)
    extends SilhouetteController(scc) {

  /** Handles the authenticated action.
    *
    * @return
    *   The result to display.
    */
  def authenticated: Action[AnyContent] = SecuredAction.async {
    implicit request: SecuredRequest[EnvType, AnyContent] =>
      Future.successful(Ok(authenticated(request.identity)))
  }

  /** Handles the not authenticated action.
    *
    * @return
    *   The result to display.
    */
  def notAuthenticated = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.notAuthenticated())
  }

  /** Handles the Sign Out action.
    *
    * @return
    *   The result to display.
    */
  def signOut = SecuredAction.async {
    implicit request: SecuredRequest[EnvType, AnyContent] =>
      val result = Redirect(Calls.notAuthenticated)
      eventBus.publish(LogoutEvent(request.identity, request))
      authenticatorService.discard(request.authenticator, result)
  }
}
