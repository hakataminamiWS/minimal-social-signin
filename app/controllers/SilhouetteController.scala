package controllers

import javax.inject.Inject
import play.api.mvc._
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.i18n.Langs
import play.api.Logging
import play.api.http.FileMimeTypes

import models.services.UserService
import models.services.AuthTokenService
import utils.auth.DefaultEnv

import com.mohiva.play.silhouette.api.actions.SecuredActionBuilder
import com.mohiva.play.silhouette.api.actions.UnsecuredActionBuilder
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.Clock
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import com.mohiva.play.silhouette.api.services.AvatarService
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.services.AuthenticatorService
import com.mohiva.play.silhouette.api.EventBus

import scala.concurrent.duration.FiniteDuration

abstract class SilhouetteController(
    override protected val controllerComponents: SilhouetteControllerComponents
) extends MessagesAbstractController(controllerComponents)
    with SilhouetteComponents
    with I18nSupport
    with Logging {

  def SecuredAction: SecuredActionBuilder[EnvType, AnyContent] =
    controllerComponents.silhouette.SecuredAction
  def UnsecuredAction: UnsecuredActionBuilder[EnvType, AnyContent] =
    controllerComponents.silhouette.UnsecuredAction

  def userService: UserService = controllerComponents.userService
  def authInfoRepository: AuthInfoRepository =
    controllerComponents.authInfoRepository
  def authTokenService: AuthTokenService = controllerComponents.authTokenService
  def rememberMeConfig: RememberMeConfig = controllerComponents.rememberMeConfig
  def clock: Clock = controllerComponents.clock
  def socialProviderRegistry: SocialProviderRegistry =
    controllerComponents.socialProviderRegistry
  def avatarService: AvatarService = controllerComponents.avatarService

  def silhouette: Silhouette[EnvType] = controllerComponents.silhouette
  def authenticatorService: AuthenticatorService[AuthType] =
    silhouette.env.authenticatorService
  def eventBus: EventBus = silhouette.env.eventBus
}

trait SilhouetteComponents {
  type EnvType = DefaultEnv
  type AuthType = EnvType#A
  type IdentityType = EnvType#I

  def userService: UserService
  def authInfoRepository: AuthInfoRepository
  def authTokenService: AuthTokenService
  def rememberMeConfig: RememberMeConfig
  def clock: Clock
  def socialProviderRegistry: SocialProviderRegistry
  def avatarService: AvatarService

  def silhouette: Silhouette[EnvType]
}

trait SilhouetteControllerComponents
    extends MessagesControllerComponents
    with SilhouetteComponents

final case class DefaultSilhouetteControllerComponents @Inject() (
    silhouette: Silhouette[DefaultEnv],
    userService: UserService,
    authInfoRepository: AuthInfoRepository,
    authTokenService: AuthTokenService,
    rememberMeConfig: RememberMeConfig,
    clock: Clock,
    socialProviderRegistry: SocialProviderRegistry,
    avatarService: AvatarService,
    messagesActionBuilder: MessagesActionBuilder,
    actionBuilder: DefaultActionBuilder,
    parsers: PlayBodyParsers,
    messagesApi: MessagesApi,
    langs: Langs,
    fileMimeTypes: FileMimeTypes,
    executionContext: scala.concurrent.ExecutionContext
) extends SilhouetteControllerComponents

trait RememberMeConfig {
  def expiry: FiniteDuration
  def idleTimeout: Option[FiniteDuration]
  def cookieMaxAge: Option[FiniteDuration]
}

final case class DefaultRememberMeConfig(
    expiry: FiniteDuration,
    idleTimeout: Option[FiniteDuration],
    cookieMaxAge: Option[FiniteDuration]
) extends RememberMeConfig
