package org.devrock.invalids.services

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cats.effect.IO
import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.SessionOptions._
import org.devrock.invalids.domain.Accounts._
import org.devrock.invalids.repositories.{AccountRepository, MentorSearchRepository}

trait AccountService extends Service {
  protected implicit val accountRepository: AccountRepository[IO]
  protected implicit val mentorSearchRepository: MentorSearchRepository

  protected val authRoutes: Route =
    pathPrefix("account") {
      (path("login") & post) {
        entity(as[AuthFormat]) { authFormat =>
          onSuccess(accountRepository.login(authFormat).unsafeToFuture) {
            case Some(login) => setSession(oneOff, usingHeaders, UserSession(login)) {
              complete(HttpResponse(StatusCodes.OK, entity = sessionManager.clientSessionManager.createHeader(UserSession(login)).value))
            }
            case _ => complete(HttpResponse(StatusCodes.Unauthorized))
          }
        }
      } ~
      (path("info") & get) {
        touchRequiredSession(oneOff, usingHeaders) { session =>
          complete(accountRepository.info(session.login))
        }
      } ~
      (path("register") & post) {
        entity(as[RegisterFormat]) { registerFormat =>
          complete(accountRepository.register(registerFormat).unsafeToFuture)
        }
      } ~
      touchRequiredSession(oneOff, usingHeaders) { session =>
        (path("role") & get) {
          complete(accountRepository.role(session.login))
        } ~
        (path("developer") & get) {
          complete(accountRepository.developer(session.login))
        } ~
        (path("exists" / Segment) & get) { login =>
          complete(accountRepository.exists(login))
        } ~
        (path("mentors") & get) {
          complete(mentorSearchRepository.mentors(session.login))
        } ~
        (path("set_mentor" / Segment) & post) { mentor =>
          complete(accountRepository.setMentor(session.login, mentor))
        }
      }
    }
}
