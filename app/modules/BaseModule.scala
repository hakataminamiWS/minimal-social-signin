package modules

import com.google.inject.AbstractModule
import models.daos.{AuthTokenDAO, AuthTokenDAOImpl}
import models.services.{AuthTokenService, AuthTokenServiceImpl}

/** The base Guice module.
  */
class BaseModule extends AbstractModule {

  /** Configures the module.
    */
  override def configure(): Unit = {
    bind(classOf[AuthTokenDAO]).to(classOf[AuthTokenDAOImpl])
    bind(classOf[AuthTokenService]).to(classOf[AuthTokenServiceImpl])
  }
}
