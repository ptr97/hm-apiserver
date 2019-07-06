package com.pwos.api.domain.places

import cats.data.EitherT
import com.pwos.api.domain.{PlaceAlreadyExistsError, PlaceNotFoundError}


trait PlaceValidationAlgebra[F[_]] {
  def doesNotExists(place: Place): EitherT[F, PlaceAlreadyExistsError, Unit]

  def exists(place: Place): EitherT[F, PlaceNotFoundError.type, Unit]
}
