package org.devrock.invalids.repositories

import cats.syntax.option._
import cats.syntax.applicative._
import cats.syntax.functor._
import cats.syntax.applicativeError._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor.Aux
import org.devrock.invalids.domain.Accounts._
import org.devrock.invalids.exceptions.Sync

trait AccountRepository[F[_]] {
  def register(registerInfo: RegisterFormat): F[Int]

  def login(authFormat: AuthFormat): F[Option[String]]

  def info(login: String): F[AccountInfo]

  def maybeMentor(login: String): F[Option[Mentor]]

  def role(login: String): F[Option[String]]

  def developer(login: String): F[Option[Developer]]

  def exists(login: String): F[Boolean]

  def setMentor(developer: String, mentor: String): F[Int]
}

class AccountRepositoryImpl[F[_]: Sync](implicit xa: Aux[F, Unit]) extends AccountRepository[F] {

  def register(registerFormat: RegisterFormat): F[Int] = {
    import registerFormat._
    sql"""insert into accounts (login, password, name, surname, email)
         values (${registerFormat.login}, $password, $name, $surname, $email)
         on conflict do nothing""".update.run
      .transact(xa)
  }

  def login(loginFormat: AuthFormat): F[Option[String]] =
    sql"select login from accounts where email = ${loginFormat.email} and password = ${loginFormat.password}"
      .query[String]
      .option
      .transact(xa)

  def info(login: String): F[AccountInfo] =
    AccountRepository.info(login).transact(xa)

  def role(login: String): F[Option[String]] =
    sql"""select case when (select true from developers where login = $login) then 'developer'
         when (select true from mentors where login = $login) then 'mentor'
         when (select true from reporters where login = $login) then 'reporter'
         else NULL END""".query[String].option.transact(xa).attempt.map(_.toOption.flatten)

  def developer(login: String): F[Option[Developer]] =
    (for {
      maybeRaw <- sql"select tag, mentor from developers where login = $login".query[(String, Option[String])].option
      res <- maybeRaw
        .fold(none[Developer].pure[ConnectionIO]) { raw =>
          for {
            account <- AccountRepository.info(login)
            mentor  <- raw._2.fold(none[Mentor].pure[ConnectionIO])(AccountRepository.maybeMentor)
          } yield Developer(account, raw._1, mentor).some
        }
    } yield res).transact(xa)

  def exists(login: String): F[Boolean] =
    sql"select 1 from accounts where login = $login".query[Int].option.map(_.isDefined).transact(xa)

  def maybeMentor(login: String): F[Option[Mentor]] =
    AccountRepository.maybeMentor(login).transact(xa)

  def setMentor(developer: String, mentor: String): F[Int] =
    sql"update developers set mentor = $mentor where login = $developer".update.run.transact(xa)
}

object AccountRepository {

  def maybeInfo(login: String): ConnectionIO[Option[AccountInfo]] =
    sql"select login, name, surname from accounts where login = $login"
      .query[AccountInfo]
      .option

  def info(login: String): ConnectionIO[AccountInfo] =
    sql"select login, name, surname from accounts where login = $login"
      .query[AccountInfo]
      .unique

  def maybeMentor(login: String): ConnectionIO[Option[Mentor]] =
    for {
      account <- AccountRepository.maybeInfo(login)
      res <- account.fold(none[Mentor].pure[ConnectionIO])(
        a =>
          sql"select tag, description from mentors where login = $login".query[(String, String)].unique.map { t =>
            Mentor(a, t._1, t._2).some
          }
      )
    } yield res
}
