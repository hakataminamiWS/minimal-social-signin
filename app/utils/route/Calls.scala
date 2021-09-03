package utils.route

import play.api.mvc.Call

/** Defines some common redirect calls used in authentication flow.
  */
object Calls {

  /** @return The URL to redirect to when an authentication succeeds. */
  def authenticated: Call =
    controllers.routes.ApplicationController.authenticated

  /** @return The URL to redirect to when an authentication fails. */
  def notAuthenticated: Call = controllers.routes.ApplicationController.notAuthenticated

}
