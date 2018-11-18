package org.devrock.invalids.utils

import akka.http.scaladsl.marshalling.Marshaller

trait CatsEffectSupport {
  import cats.effect.IO

  implicit def ioMarshaller[A, B](implicit m: Marshaller[A, B]): Marshaller[IO[A], B] =
    Marshaller { implicit ec => a =>
      a.unsafeToFuture.flatMap(m(_))
    }
}
