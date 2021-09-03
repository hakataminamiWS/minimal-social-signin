package jobs

import akka.actor.{ ActorRef, ActorSystem }
import com.google.inject.Inject
import com.google.inject.name.Named
import com.typesafe.akka.extension.quartz.QuartzSchedulerExtension

// Akka Typed Actors sample.
// import com.typesafe.akka.extension.quartz.QuartzSchedulerExtension
// import akka.actor.typed.ActorSystem
// import akka.actor.typed.ActorRef

/** Schedules the jobs.
  */
class Scheduler @Inject() (
    _system: ActorSystem,
    @Named("auth-token-cleaner") authTokenCleaner: ActorRef
) {

  QuartzSchedulerExtension(_system).schedule(
    "AuthTokenCleaner",
    authTokenCleaner,
    AuthTokenCleaner.Clean
  )

  authTokenCleaner ! AuthTokenCleaner.Clean
}
