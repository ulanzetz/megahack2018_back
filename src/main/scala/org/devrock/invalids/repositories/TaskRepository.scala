package org.devrock.invalids.repositories

import java.sql.Timestamp
import java.time.{LocalDateTime, LocalTime}

import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.traverse._
import cats.syntax.option._
import cats.instances.list._
import doobie.implicits._
import doobie.util.transactor.Transactor.Aux
import org.devrock.invalids.domain.Accounts.AccountInfo
import org.devrock.invalids.domain.Tasks._
import org.devrock.invalids.exceptions.{NoPermission, Sync}

trait TaskRepository[F[_]] {
  implicit val F: Sync[F]

  def list(login: String): F[List[Task]]

  def info(taskId: Int): F[Task]

  def update(taskId: Int, taskUpdate: TaskUpdate): F[Int]

  def add(reporter: String, taskAdd: TaskAdd): F[Int]

  def hasPermission(login: String, taskId: Int): F[Boolean]

  def ifHasPermission[T](login: String, taskId: Int, action: Int => F[T]): F[T] =
    for {
      has <- hasPermission(login, taskId)
      res <- if (has)
        action(taskId)
      else Sync[F].raiseError(new NoPermission("Нет доступа к задаче"))
    } yield res

  def assign(taskId: Int, developer: String): F[Int]

  def getFreeTagged(login: String): F[List[Task]]
}

class TaskRepositoryImpl[F[_]](implicit val F: Sync[F], xa: Aux[F, Unit], accountRepository: AccountRepository[F])
  extends TaskRepository[F] {

  def list(login: String): F[List[Task]] =
    for {
      role <- accountRepository.role(login)
      res <- role match {
        case Some("developer") => list(DeveloperFilter(login))
        case Some("mentor")    => list(ReviewerFilter(login))
        case Some("reporter")  => list(ReporterFilter(login))
        case _                 => Sync[F].pure(Nil)
      }
    } yield res

  private def list(filter: TaskFilter): F[List[Task]] =
    for {
      ids <- (fr"select id from tasks " ++ filter.fr)
        .query[Int]
        .stream
        .compile
        .toList
        .transact(xa)
      res <- ids.traverse(info)
    } yield res

  def getFreeTagged(login: String): F[List[Task]] =
    for {
      ids <- sql"""select id from tasks where developer is null and tag =
             (select tag from developers where login = $login)"""
        .query[Int]
        .stream
        .compile
        .toList
        .transact(xa)
      res <- ids.traverse(info)
    } yield res

  def info(taskId: Int): F[Task] = {
    case class TaskRaw(
        id: Int,
        label: String,
        description: String,
        x: Int,
        imageId: Option[Int],
        developer: Option[String],
        reviewer: Option[String],
        tag: String,
        taskType: String,
        reporter: String,
        price: Int,
        deadLine: Timestamp,
        created: Timestamp
    )

    for {
      raw <- sql"""select id, label, description, x, image_id, developer, reviewer, tag, task_type,
             reporter, price, deadline, created from tasks where id = $taskId"""
        .query[TaskRaw]
        .unique
        .transact(xa)
      developer <- raw.developer
        .map(l => accountRepository.info(l).map(_.some))
        .getOrElse(Sync[F].pure(none[AccountInfo]))
      reviewer <- raw.reviewer
        .map(l => accountRepository.info(l).map(_.some))
        .getOrElse(Sync[F].pure(none[AccountInfo]))
      reporter <- accountRepository.info(raw.reporter)
    } yield
      Task(
        raw.id,
        raw.label,
        raw.description,
        raw.x,
        raw.imageId,
        developer,
        reviewer,
        raw.tag,
        raw.taskType,
        reporter,
        raw.price,
        raw.deadLine,
        raw.created
      )
  }

  def update(taskId: Int, taskUpdate: TaskUpdate): F[Int] =
    (fr"update tasks " ++ taskUpdate.set ++ fr" where id = $taskId").update.run
      .transact(xa)

  def assign(taskId: Int, developer: String): F[Int] =
    sql"update tasks set developer = $developer where id = $taskId".update.run.transact(xa)

  def add(reporter: String, taskAdd: TaskAdd): F[Int] = {
    import taskAdd._
    sql"""insert into tasks (reporter, label, description, deadline, price, task_type, tag)
         values ($reporter, $label, $description, ${Timestamp.valueOf(LocalDateTime.of(deadLine, LocalTime.now))}, $price, $taskType, $tag) returning id"""
      .query[Int]
      .unique
      .transact(xa)
  }

  def hasPermission(login: String, taskId: Int): F[Boolean] =
    sql"""select 1 from tasks where id = $taskId and 
         (developer = $login or reviewer = $login or reporter = $login)"""
      .query[Int]
      .option
      .map(_.isDefined)
      .transact(xa)
}
