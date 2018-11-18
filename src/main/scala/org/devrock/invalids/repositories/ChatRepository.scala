package org.devrock.invalids.repositories
import java.sql.Timestamp

import cats.instances.list._
import cats.syntax.traverse._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor.Aux
import org.devrock.invalids.domain.Chat.ChatMessage
import org.devrock.invalids.exceptions.Sync

trait ChatRepository[F[_]] {
  def addMessage(first: String, second: String, text: String): F[Int]

  def history(first: String, second: String, offset: Int, limit: Int): F[List[ChatMessage]]

  def read(first: String, second: String, ts: Timestamp): F[Int]
}

class ChatRepositoryImpl[F[_]: Sync](implicit xa: Aux[F, Unit]) extends ChatRepository[F] {

  def addMessage(sender: String, second: String, text: String): F[Int] = {
    val ordered = List(sender, second).sorted
    (for {
      chatId <- chatId(ordered.head, ordered(1))
      add    <- sql"insert into messages (text, sender, chat_id) values ($text, $sender, $chatId)".update.run
    } yield add).transact(xa)
  }

  def history(first: String, second: String, offset: Int, limit: Int): F[List[ChatMessage]] = {
    val ordered = List(first, second).sorted
    (for {
      chatId <- chatId(ordered.head, ordered(1))
      rawList <- sql"""select sender, ts, text, subject_read from messages
             where chat_id = $chatId order by ts desc offset $offset limit $limit"""
        .query[(String, Timestamp, String, Boolean)]
        .stream
        .compile
        .toList
      res <- rawList.traverse[ConnectionIO, ChatMessage] { t =>
        AccountRepository.info(t._1).map(s => ChatMessage(s, t._2, t._3, t._4))
      }
    } yield res).transact(xa)
  }

  def read(first: String, second: String, ts: Timestamp): F[Int] = {
    val ordered = List(first, second).sorted
    (for {
      chatId <- chatId(ordered.head, ordered(1))
      res    <- sql"update messages set subject_read = true where chat_id = $chatId and ts = $ts".update.run
    } yield res).transact(xa)
  }

  private def chatId(first: String, second: String): ConnectionIO[Int] =
    sql"""insert into chats (first, second) values ($first, $second) on conflict (first, second) do update set first = $first
     returning id"""
      .query[Int]
      .unique
}
