package org.devrock.invalids

import cats.MonadError

package object exceptions {
  type Sync[F[_]] = MonadError[F, Throwable]

  object Sync {
    def apply[F[_]: Sync]: Sync[F] =
      implicitly[Sync[F]]
  }

  class ApiException(message: String) extends Exception

  class NoPermission(message: String) extends ApiException(message)
}
