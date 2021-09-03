package modules

import jobs.{AuthTokenCleaner, Scheduler}
import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

/** The job module.
  */
class JobModule extends AbstractModule with AkkaGuiceSupport {

  /** Configures the module.
    */
  override def configure() = {
    bindActor[AuthTokenCleaner]("auth-token-cleaner")
    bind(classOf[Scheduler]).asEagerSingleton()
  }
}
