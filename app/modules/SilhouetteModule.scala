package modules

import com.google.inject.name.Named
import com.google.inject.{AbstractModule, Provides}
import com.mohiva.play.silhouette.api.actions.{
  SecuredErrorHandler,
  UnsecuredErrorHandler
}
import com.mohiva.play.silhouette.api.crypto._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services._
import com.mohiva.play.silhouette.api.util._
import com.mohiva.play.silhouette.api.{
  Environment,
  EventBus,
  Silhouette,
  SilhouetteProvider
}
import com.mohiva.play.silhouette.crypto.{
  JcaCrypter,
  JcaCrypterSettings,
  JcaSigner,
  JcaSignerSettings
}
import com.mohiva.play.silhouette.impl.authenticators._
import com.mohiva.play.silhouette.impl.providers._
import com.mohiva.play.silhouette.impl.providers.oauth1._
import com.mohiva.play.silhouette.impl.providers.oauth1.secrets.{
  CookieSecretProvider,
  CookieSecretSettings
}
import com.mohiva.play.silhouette.impl.providers.oauth1.services.PlayOAuth1Service
import com.mohiva.play.silhouette.impl.providers.oauth2._
import com.mohiva.play.silhouette.impl.providers.openid.YahooProvider
import com.mohiva.play.silhouette.impl.providers.openid.services.PlayOpenIDService
import com.mohiva.play.silhouette.impl.providers.state.{
  CsrfStateItemHandler,
  CsrfStateSettings
}
import com.mohiva.play.silhouette.impl.services._
import com.mohiva.play.silhouette.impl.util._
import com.mohiva.play.silhouette.persistence.daos.{
  DelegableAuthInfoDAO,
  InMemoryAuthInfoDAO
}
import com.mohiva.play.silhouette.persistence.repositories.DelegableAuthInfoRepository
import com.typesafe.config.Config
import controllers.{
  DefaultRememberMeConfig,
  DefaultSilhouetteControllerComponents,
  RememberMeConfig,
  SilhouetteControllerComponents
}
import models.daos._
import models.services.{UserService, UserServiceImpl}
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import net.ceedubs.ficus.readers.ValueReader
import net.codingwell.scalaguice.ScalaModule

import play.api.Configuration
import play.api.libs.openid.OpenIdClient
import play.api.libs.ws.WSClient
import play.api.mvc.{Cookie, CookieHeaderEncoding}
import utils.auth.{ CustomSecuredErrorHandler, CustomUnsecuredErrorHandler, DefaultEnv }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration

/** The Guice module which wires all Silhouette dependencies.
  */
class SilhouetteModule extends AbstractModule with ScalaModule {

  /** A very nested optional reader, to support these cases: Not set, set None,
    * will use default ('Lax') Set to null, set Some(None), will use 'No
    * Restriction' Set to a string value try to match, Some(Option(string))
    */
  implicit val sameSiteReader: ValueReader[Option[Option[Cookie.SameSite]]] =
    (config: Config, path: String) => {
      if (config.hasPathOrNull(path)) {
        if (config.getIsNull(path))
          Some(None)
        else {
          Some(Cookie.SameSite.parse(config.getString(path)))
        }
      } else {
        None
      }
    }

  /** Configures the module.
    */
  override def configure(): Unit = {
    bind[Silhouette[DefaultEnv]].to[SilhouetteProvider[DefaultEnv]]
    bind[UnsecuredErrorHandler].to[CustomUnsecuredErrorHandler]
    bind[SecuredErrorHandler].to[CustomSecuredErrorHandler]
    bind[UserService].to[UserServiceImpl]
    bind[UserDAO].to[UserDAOImpl]
    bind[CacheLayer].to[PlayCacheLayer]
    bind[IDGenerator].toInstance(new SecureRandomIDGenerator())
    bind[FingerprintGenerator].toInstance(
      new DefaultFingerprintGenerator(false)
    )
    bind[EventBus].toInstance(EventBus())
    bind[Clock].toInstance(Clock())

    // Replace this with the bindings to your concrete DAOs
    bind[DelegableAuthInfoDAO[PasswordInfo]].toInstance(
      new InMemoryAuthInfoDAO[PasswordInfo]
    )
    bind[DelegableAuthInfoDAO[OAuth1Info]].toInstance(
      new InMemoryAuthInfoDAO[OAuth1Info]
    )
    bind[DelegableAuthInfoDAO[OAuth2Info]].toInstance(
      new InMemoryAuthInfoDAO[OAuth2Info]
    )
    bind[DelegableAuthInfoDAO[OpenIDInfo]].toInstance(
      new InMemoryAuthInfoDAO[OpenIDInfo]
    )
  }

  /** Provides the HTTP layer implementation.
    *
    * @param client
    *   Play's WS client.
    * @return
    *   The HTTP layer implementation.
    */
  @Provides
  def provideHTTPLayer(client: WSClient): HTTPLayer = new PlayHTTPLayer(client)

  /** Provides the Silhouette environment.
    *
    * @param userService
    *   The user service implementation.
    * @param authenticatorService
    *   The authentication service implementation.
    * @param eventBus
    *   The event bus instance.
    * @return
    *   The Silhouette environment.
    */
  @Provides
  def provideEnvironment(
      userService: UserService,
      authenticatorService: AuthenticatorService[CookieAuthenticator],
      eventBus: EventBus
  ): Environment[DefaultEnv] = {

    Environment[DefaultEnv](
      userService,
      authenticatorService,
      Seq(),
      eventBus
    )
  }

  /** Provides the social provider registry.
    *
    * @param googleProvider
    *   The Google provider implementation.
    * @return
    *   The Silhouette environment.
    */
  @Provides
  def provideSocialProviderRegistry(
      googleProvider: GoogleProvider
  ): SocialProviderRegistry = {

    SocialProviderRegistry(
      Seq(
        googleProvider
      )
    )
  }

  /** Provides the signer for the OAuth1 token secret provider.
    *
    * @param configuration
    *   The Play configuration.
    * @return
    *   The signer for the OAuth1 token secret provider.
    */
  @Provides @Named("oauth1-token-secret-signer")
  def provideOAuth1TokenSecretSigner(configuration: Configuration): Signer = {
    val config = configuration.underlying.as[JcaSignerSettings](
      "silhouette.oauth1TokenSecretProvider.signer"
    )

    new JcaSigner(config)
  }

  /** Provides the crypter for the OAuth1 token secret provider.
    *
    * @param configuration
    *   The Play configuration.
    * @return
    *   The crypter for the OAuth1 token secret provider.
    */
  @Provides @Named("oauth1-token-secret-crypter")
  def provideOAuth1TokenSecretCrypter(configuration: Configuration): Crypter = {
    val config = configuration.underlying.as[JcaCrypterSettings](
      "silhouette.oauth1TokenSecretProvider.crypter"
    )

    new JcaCrypter(config)
  }

  /** Provides the signer for the CSRF state item handler.
    *
    * @param configuration
    *   The Play configuration.
    * @return
    *   The signer for the CSRF state item handler.
    */
  @Provides @Named("csrf-state-item-signer")
  def provideCSRFStateItemSigner(configuration: Configuration): Signer = {
    val config = configuration.underlying.as[JcaSignerSettings](
      "silhouette.csrfStateItemHandler.signer"
    )

    new JcaSigner(config)
  }

  /** Provides the signer for the social state handler.
    *
    * @param configuration
    *   The Play configuration.
    * @return
    *   The signer for the social state handler.
    */
  @Provides @Named("social-state-signer")
  def provideSocialStateSigner(configuration: Configuration): Signer = {
    val config = configuration.underlying.as[JcaSignerSettings](
      "silhouette.socialStateHandler.signer"
    )

    new JcaSigner(config)
  }

  /** Provides the signer for the authenticator.
    *
    * @param configuration
    *   The Play configuration.
    * @return
    *   The signer for the authenticator.
    */
  @Provides @Named("authenticator-signer")
  def provideAuthenticatorSigner(configuration: Configuration): Signer = {
    val config = configuration.underlying.as[JcaSignerSettings](
      "silhouette.authenticator.signer"
    )

    new JcaSigner(config)
  }

  /** Provides the crypter for the authenticator.
    *
    * @param configuration
    *   The Play configuration.
    * @return
    *   The crypter for the authenticator.
    */
  @Provides @Named("authenticator-crypter")
  def provideAuthenticatorCrypter(configuration: Configuration): Crypter = {
    val config = configuration.underlying.as[JcaCrypterSettings](
      "silhouette.authenticator.crypter"
    )

    new JcaCrypter(config)
  }

  /** Provides the auth info repository.
    *
    * @param oauth2InfoDAO
    *   The implementation of the delegable OAuth2 auth info DAO.
    * @return
    *   The auth info repository instance.
    */
  @Provides
  def provideAuthInfoRepository(
      oauth2InfoDAO: DelegableAuthInfoDAO[OAuth2Info]
  ): AuthInfoRepository = {

    new DelegableAuthInfoRepository(
      oauth2InfoDAO
    )
  }

  /** Provides the authenticator service.
    *
    * @param signer
    *   The signer implementation.
    * @param crypter
    *   The crypter implementation.
    * @param cookieHeaderEncoding
    *   Logic for encoding and decoding `Cookie` and `Set-Cookie` headers.
    * @param fingerprintGenerator
    *   The fingerprint generator implementation.
    * @param idGenerator
    *   The ID generator implementation.
    * @param configuration
    *   The Play configuration.
    * @param clock
    *   The clock instance.
    * @return
    *   The authenticator service.
    */
  @Provides
  def provideAuthenticatorService(
      @Named("authenticator-signer") signer: Signer,
      @Named("authenticator-crypter") crypter: Crypter,
      cookieHeaderEncoding: CookieHeaderEncoding,
      fingerprintGenerator: FingerprintGenerator,
      idGenerator: IDGenerator,
      configuration: Configuration,
      clock: Clock
  ): AuthenticatorService[CookieAuthenticator] = {

    val config = configuration.underlying.as[CookieAuthenticatorSettings](
      "silhouette.authenticator"
    )
    val authenticatorEncoder = new CrypterAuthenticatorEncoder(crypter)

    new CookieAuthenticatorService(
      config,
      None,
      signer,
      cookieHeaderEncoding,
      authenticatorEncoder,
      fingerprintGenerator,
      idGenerator,
      clock
    )
  }

  /** Provides the avatar service.
    *
    * @param httpLayer
    *   The HTTP layer implementation.
    * @return
    *   The avatar service implementation.
    */
  @Provides
  def provideAvatarService(httpLayer: HTTPLayer): AvatarService =
    new GravatarService(httpLayer)

  /** Provides the OAuth1 token secret provider.
    *
    * @param signer
    *   The signer implementation.
    * @param crypter
    *   The crypter implementation.
    * @param configuration
    *   The Play configuration.
    * @param clock
    *   The clock instance.
    * @return
    *   The OAuth1 token secret provider implementation.
    */
  @Provides
  def provideOAuth1TokenSecretProvider(
      @Named("oauth1-token-secret-signer") signer: Signer,
      @Named("oauth1-token-secret-crypter") crypter: Crypter,
      configuration: Configuration,
      clock: Clock
  ): OAuth1TokenSecretProvider = {

    val settings = configuration.underlying.as[CookieSecretSettings](
      "silhouette.oauth1TokenSecretProvider"
    )
    new CookieSecretProvider(settings, signer, crypter, clock)
  }

  /** Provides the CSRF state item handler.
    *
    * @param idGenerator
    *   The ID generator implementation.
    * @param signer
    *   The signer implementation.
    * @param configuration
    *   The Play configuration.
    * @return
    *   The CSRF state item implementation.
    */
  @Provides
  def provideCsrfStateItemHandler(
      idGenerator: IDGenerator,
      @Named("csrf-state-item-signer") signer: Signer,
      configuration: Configuration
  ): CsrfStateItemHandler = {
    val settings = configuration.underlying.as[CsrfStateSettings](
      "silhouette.csrfStateItemHandler"
    )
    new CsrfStateItemHandler(settings, idGenerator, signer)
  }

  /** Provides the social state handler.
    *
    * @param signer
    *   The signer implementation.
    * @return
    *   The social state handler implementation.
    */
  @Provides
  def provideSocialStateHandler(
      @Named("social-state-signer") signer: Signer,
      csrfStateItemHandler: CsrfStateItemHandler
  ): SocialStateHandler = {

    new DefaultSocialStateHandler(Set(csrfStateItemHandler), signer)
  }

  /** Provides the Google provider.
    *
    * @param httpLayer
    *   The HTTP layer implementation.
    * @param socialStateHandler
    *   The social state handler implementation.
    * @param configuration
    *   The Play configuration.
    * @return
    *   The Google provider.
    */
  @Provides
  def provideGoogleProvider(
      httpLayer: HTTPLayer,
      socialStateHandler: SocialStateHandler,
      configuration: Configuration
  ): GoogleProvider = {

    new GoogleProvider(
      httpLayer,
      socialStateHandler,
      configuration.underlying.as[OAuth2Settings]("silhouette.google")
    )
  }

  /** Provides the remember me configuration.
    *
    * @param configuration
    *   The Play configuration.
    * @return
    *   The remember me config.
    */
  @Provides
  def providesRememberMeConfig(
      configuration: Configuration
  ): RememberMeConfig = {
    val c = configuration.underlying
    DefaultRememberMeConfig(
      expiry = c.as[FiniteDuration](
        "silhouette.authenticator.rememberMe.authenticatorExpiry"
      ),
      idleTimeout = c.getAs[FiniteDuration](
        "silhouette.authenticator.rememberMe.authenticatorIdleTimeout"
      ),
      cookieMaxAge = c.getAs[FiniteDuration](
        "silhouette.authenticator.rememberMe.cookieMaxAge"
      )
    )
  }

  @Provides
  def providesSilhouetteComponents(
      components: DefaultSilhouetteControllerComponents
  ): SilhouetteControllerComponents = {
    components
  }
}
