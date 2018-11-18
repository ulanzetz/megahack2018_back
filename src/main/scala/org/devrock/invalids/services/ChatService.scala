package org.devrock.invalids.services

import java.sql.Timestamp

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.{FromStringUnmarshaller, Unmarshaller}
import cats.effect.IO
import org.devrock.invalids.repositories.ChatRepository
import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.SessionOptions.{oneOff, usingHeaders}

import scala.concurrent.Future

trait ChatService extends Service {
  protected implicit val chatRepository: ChatRepository[IO]

  protected implicit val tsUnmarshaller: FromStringUnmarshaller[Timestamp] = Unmarshaller { _ =>
    s => Future.successful(Timestamp.valueOf(s))
  }

  def chatRoutes: Route =
    touchRequiredSession(oneOff, usingHeaders) { session =>
      pathPrefix("chat") {
        (path("add_message" / Segment) & post) { (second) =>
          parameter('text.as[String]) { text =>
            complete(chatRepository.addMessage(session.login, second, text))
          }
        } ~
        (path("history" / Segment) & get) { second =>
          parameters('offset.as[Int], 'limit.as[Int]) { (offset, limit) =>
            complete(chatRepository.history(session.login, second, offset, limit))
          }
        } ~
        (path("read" / Segment) & post) { second =>
          parameter('ts.as[Timestamp]) { ts =>
            complete(chatRepository.read(session.login, second, ts))
          }
        }
      }
    }
}
