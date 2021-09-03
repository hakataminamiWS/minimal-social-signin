package controllers

import com.google.inject.Inject
import play.api.mvc._
import play.api.i18n.Messages

import com.mohiva.play.silhouette.impl.providers.SocialProvider
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfileBuilder

import com.mohiva.play.silhouette.api.LoginEvent
import com.mohiva.play.silhouette.api.exceptions.ProviderException

import utils.route.Calls

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.mohiva.play.silhouette.api.Authenticator.Implicits._

/** The social auth controller.
  */
class SocialAuthController @Inject() (
    scc: SilhouetteControllerComponents
)(implicit ex: ExecutionContext)
    extends SilhouetteController(scc) {

  /** Authenticates a user against a social provider.
    *
    * @param provider
    *   The ID of the provider to authenticate against.
    * @return
    *   The result to display.
    */
  def authenticate(provider: String) = Action.async {
    implicit request: Request[AnyContent] =>
      (socialProviderRegistry.get[SocialProvider](provider) match {
        case Some(p: SocialProvider with CommonSocialProfileBuilder) =>
          p.authenticate().flatMap {
            case Left(result) => Future.successful(result)
            case Right(authInfo) =>
              for {
                profile <- p.retrieveProfile(authInfo)
                user <- userService.save(profile)
                authInfo <- authInfoRepository.save(profile.loginInfo, authInfo)
                authenticator <- authenticatorService.create(profile.loginInfo)
                aut = authenticator.copy(
                  expirationDateTime = clock.now + scc.rememberMeConfig.expiry,
                  idleTimeout = scc.rememberMeConfig.idleTimeout,
                  cookieMaxAge = scc.rememberMeConfig.cookieMaxAge
                )
                value <- authenticatorService.init(aut)
                result <- authenticatorService
                  .embed(
                    value,
                    Redirect(Calls.authenticated)
                  )
              } yield {
                eventBus.publish(LoginEvent(user, request))
                result
              }
          }
        case _ =>
          Future.failed(
            new ProviderException(
              s"Cannot authenticate with unexpected social provider $provider"
            )
          )
      }).recover { case e: ProviderException =>
        logger.error("Unexpected provider error", e)
        Redirect(Calls.notAuthenticated).flashing(
          "error" -> Messages("could.not.authenticate")
        )
      }
  }
}
