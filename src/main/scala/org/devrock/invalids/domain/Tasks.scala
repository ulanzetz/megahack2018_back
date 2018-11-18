package org.devrock.invalids.domain

import java.sql.Timestamp
import java.time.{LocalDate, LocalDateTime}

import io.circe.generic.JsonCodec
import org.devrock.invalids.domain.Accounts._

object Tasks {
  import Encoders._

  sealed abstract class TaskFilter(login: String, field: String) {
    import doobie.Fragment

    def fr: Fragment =
      Fragment.const(s"where $field = '$login'")
  }

  final case class DeveloperFilter(login: String) extends TaskFilter(login, "developer")

  final case class ReviewerFilter(login: String) extends TaskFilter(login, "reviewer")

  final case class ReporterFilter(login: String) extends TaskFilter(login, "reporter")

  @JsonCodec(encodeOnly = true) case class Task(
      id: Int,
      label: String,
      description: String,
      x: Int,
      imageId: Option[Int],
      developer: Option[AccountInfo],
      reviewer: Option[AccountInfo],
      tag: String,
      taskType: String,
      reporter: AccountInfo,
      price: Int,
      deadLine: Timestamp,
      created: Timestamp
  )

  @JsonCodec(decodeOnly = true) final case class TaskUpdate(
      developer: Option[String] = None,
      reviewer: Option[String] = None,
      x: Option[Int] =  None,
      imageId: Option[Int] = None
  ) {
    import doobie.Fragment

    require(x.forall(_ > 0))
    require(imageId.forall(_ > 0))

    def set: Fragment = {
      val fields =
        (developer.fold("")(v => s"developer = '$v'") ::
        reviewer.fold("")(v => s"reviewer = '$v'") ::
        x.fold("")(v => s"x = '$v'") ::
        imageId.fold("")(v => s"image_id = '$v'") :: Nil)
          .filterNot(_.isEmpty)
          .mkString(", ")
      Fragment.const(s"set $fields")
    }
  }

  @JsonCodec(decodeOnly = true) final case class TaskAdd(
      label: String,
      description: String,
      deadLine: LocalDate,
      price: Int,
      taskType: String,
      tag: String
  ) {
    require(!label.isEmpty, "Заголовок задания должен быть непустым")
    require(!description.isEmpty, "Описание не должно быть пустым")
    require(price >= 0, "Цена должна быть неотрицательн")
    require(deadLine.isAfter(LocalDate.now), "Дедлайн должен быть позже сегодняшней дать")
    require(!taskType.isEmpty)
    require(!tag.isEmpty)
  }
}
