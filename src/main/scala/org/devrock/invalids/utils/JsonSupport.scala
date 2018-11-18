package org.devrock.invalids.utils

import java.net.URLDecoder

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import io.circe.parser._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}

import scala.concurrent.Future

trait JsonSupport {
  implicit final def marshaller[A: Encoder]: ToEntityMarshaller[A] = {
    Marshaller.withFixedContentType(`application/json`) { obj =>
      HttpEntity(`application/json`, obj.asJson.spaces4)
    }
  }

  implicit final def unmarshaller[A: Decoder]: FromEntityUnmarshaller[A] =
    Unmarshaller.firstOf(
      Unmarshaller.stringUnmarshaller
        .forContentTypes(`application/json`)
        .flatMap { _ => _ => body =>
          decode[A](body).fold(Future.failed, Future.successful)
        },
      Unmarshaller.stringUnmarshaller
        .forContentTypes(`application/x-www-form-urlencoded`)
        .flatMap { _ => _ => body =>
        {
          val regex = "^[^=]+=(.*)".r
          URLDecoder.decode(body, "UTF-8") match {
            case regex(json) =>
              decode[A](json).fold(Future.failed, Future.successful)
          }
        }
        }
    )
}
