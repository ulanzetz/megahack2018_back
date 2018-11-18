package org.devrock.invalids.domain
import java.sql.Timestamp
import java.time.LocalDate

import io.circe.{Decoder, Encoder}
import io.circe.syntax._

object Encoders {
  implicit val encodeLocalDate: Encoder[Timestamp] =
    _.toString.asJson

  implicit val decodeTs: Decoder[Timestamp] =
    _.as[String].map(Timestamp.valueOf)

  implicit val decodeLocalDate: Decoder[LocalDate] =
    _.as[String].map(LocalDate.parse)
}