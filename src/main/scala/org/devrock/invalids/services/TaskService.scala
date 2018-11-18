package org.devrock.invalids.services

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cats.effect.IO
import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.SessionOptions.{oneOff, usingHeaders}
import org.devrock.invalids.domain.Tasks._
import org.devrock.invalids.repositories.TaskRepository

trait TaskService extends Service {
  protected implicit val taskRepository: TaskRepository[IO]

  def taskRoutes: Route =
    touchRequiredSession(oneOff, usingHeaders) { session =>
      pathPrefix("task") {
        path("list") {
          complete(taskRepository.list(session.login))
        } ~
        (path("info" / IntNumber) & get) { taskId =>
          complete(taskRepository.ifHasPermission(session.login, taskId, taskRepository.info))
        } ~
        (path("update" / IntNumber) & post) { taskId =>
          entity(as[TaskUpdate]) { taskUpdate =>
            complete(taskRepository.ifHasPermission(session.login, taskId, taskRepository.update(_, taskUpdate)))
          }
        } ~
        (path("accept" / IntNumber) & post) { taskId =>
          complete(taskRepository.assign(taskId, session.login))
        } ~
        (path("add") & post) {
          entity(as[TaskAdd]) { taskAdd =>
            complete(taskRepository.add(session.login, taskAdd))
          }
        } ~
        (path("free") & get) {
          complete(taskRepository.getFreeTagged(session.login))
        }
      }
    }
}
