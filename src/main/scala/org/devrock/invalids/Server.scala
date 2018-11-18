package org.devrock.invalids

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.server._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server._
import cats.effect.IO
import com.softwaremill.session._
import org.devrock.invalids.domain.Accounts.UserSession
import org.devrock.invalids.exceptions.{ApiException, NoPermission}
import org.devrock.invalids.repositories.{AccountRepository, ChatRepository, MentorSearchRepository, TaskRepository}
import org.devrock.invalids.services._

import scala.concurrent.ExecutionContext

class Server(conf: ServerConfig)(
    implicit system: ActorSystem,
    protected implicit val accountRepository: AccountRepository[IO],
    protected implicit val taskRepository: TaskRepository[IO],
    protected implicit val chatRepository: ChatRepository[IO],
    protected implicit val mentorSearchRepository: MentorSearchRepository,
    protected implicit val executionContext: ExecutionContext
) extends HttpApp with AccountService with TaskService with ChatService {
  def start(): Unit = startServer(conf.host, conf.port, system)

  implicit val sessionManager: SessionManager[UserSession] =
    new SessionManager[UserSession](SessionConfig.fromConfig())

  private def addAccessControlHeaders(): Directive0 =
    respondWithHeaders(
      `Access-Control-Allow-Origin`.*,
      `Access-Control-Allow-Credentials`(true),
      `Access-Control-Allow-Headers`("Authorization", "Content-Type", "X-Requested-With")
    )

  private val apiExceptionHandler = ExceptionHandler {
    case e: NoPermission =>
      complete(StatusCodes.Forbidden, e.getMessage)
    case e: ApiException =>
      complete(StatusCodes.BadRequest, e.getMessage)
  }

  private def preflightRequestHandler: Route = options {
    complete(HttpResponse(StatusCodes.OK).withHeaders(`Access-Control-Allow-Methods`(OPTIONS, POST, PUT, GET, DELETE)))
  }

  protected def routes: Route = addAccessControlHeaders() {
    preflightRequestHandler ~ handleExceptions(apiExceptionHandler) {
      authRoutes ~ taskRoutes ~ chatRoutes
    }
  }
}
