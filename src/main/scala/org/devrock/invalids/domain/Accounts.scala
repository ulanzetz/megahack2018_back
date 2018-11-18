package org.devrock.invalids.domain

import com.softwaremill.session.{SessionSerializer, SingleValueSessionSerializer}
import io.circe.generic.JsonCodec

import scala.util.Success

object Accounts {
  @JsonCodec(encodeOnly = true) final case class UserSession(login: String)

  implicit val sessionSerializer: SessionSerializer[UserSession, String] =
    new SingleValueSessionSerializer[UserSession, String](_.login, login => Success(UserSession(login)))

  @JsonCodec(decodeOnly = true) final case class AuthFormat(email: String, password: String)

  @JsonCodec(decodeOnly = true) final case class RegisterFormat(
      email: String,
      login: String,
      name: String,
      surname: String,
      password: String
  ) {
    require(emailValid(email), "Некорректный e-mail")
    require(password.length > 6, "Пароль должен быть длинее 6 символов")
    require(nameValid(name), "Некорректное имя")
    require(nameValid(surname), "Некорректная фамилия")
  }

  def emailValid(email: String): Boolean =
    """(\w+)@([\w\.]+)""".r.unapplySeq(email).isDefined

  def nameValid(name: String): Boolean =
    """[А-Я][а-я]+""".r.unapplySeq(name).isDefined

  @JsonCodec(encodeOnly = true) final case class AccountInfo(login: String, name: String, surname: String)

  @JsonCodec(encodeOnly = true) final case class Developer(account: AccountInfo, tag: String, mentor: Option[Mentor])

  @JsonCodec(encodeOnly = true) final case class Mentor(accountInfo: AccountInfo, tag: String, description: String)
}
