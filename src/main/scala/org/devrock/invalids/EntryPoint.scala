package org.devrock.invalids

import cats.effect.IO
import doobie.util.transactor.Transactor
import doobie.util.transactor.Transactor.Aux
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.devrock.invalids.repositories._

import scala.concurrent.ExecutionContextExecutor

object EntryPoint {
  implicit val system: ActorSystem                        = ActorSystem()
  implicit val materializer: ActorMaterializer            = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  def main(args: Array[String]): Unit = {
    val config = new ApiConfigFactory("application.conf").load()
    implicit val xa: Aux[IO, Unit] = Transactor.fromDriverManager[IO](
      config.database.driver,
      config.database.url,
      config.database.user,
      config.database.password
    )

    implicit val accountRepository = new AccountRepositoryImpl[IO]
    implicit val taskRepository = new TaskRepositoryImpl[IO]
    implicit val chatRepository = new ChatRepositoryImpl[IO]
    implicit val mentorSearchRepository = new MentorSearchRepository(config.mentorSearch)

    new Server(config.server).start()
  }
}
