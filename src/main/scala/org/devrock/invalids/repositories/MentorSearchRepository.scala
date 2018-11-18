package org.devrock.invalids.repositories

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import cats.effect.IO
import cats.instances.list._
import cats.syntax.traverse._
import io.circe.Json
import io.circe.generic.JsonCodec
import org.devrock.invalids.MentorSearchConfig
import org.devrock.invalids.domain.Accounts.{AccountInfo, Mentor}
import org.devrock.invalids.utils.JsonSupport

class MentorSearchRepository(conf: MentorSearchConfig)(
    implicit val accountRepository: AccountRepository[IO],
    system: ActorSystem,
    materializer: ActorMaterializer
) extends JsonSupport {
  @JsonCodec(decodeOnly = true) case class MentorRaw(mentor: String)

  def mentors(developer: String): IO[List[Mentor]] =
    for {
      response <- IO.fromFuture(IO(Http().singleRequest(HttpRequest(HttpMethods.GET, Uri(conf.url + s"/$developer")))))
      json     <- IO.fromFuture(IO(Unmarshal(response.entity).to[Json](unmarshaller, system.dispatcher, materializer)))
      mentorLogins = json.hcursor.downField("predict").as[List[MentorRaw]].toSeq.toList.flatten.map(_.mentor)
      mentors <- mentorLogins.traverse(accountRepository.maybeMentor)
    } yield mentors.flatMap(_.toList)
}
