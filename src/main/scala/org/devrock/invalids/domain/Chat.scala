package org.devrock.invalids.domain

import java.sql.Timestamp

import io.circe.generic.JsonCodec
import org.devrock.invalids.domain.Accounts.AccountInfo

object Chat {
  import Encoders._

  @JsonCodec(encodeOnly = true) case class ChatMessage(sender: AccountInfo, ts: Timestamp, text: String, subjectRead: Boolean)
}
