package com.pwos.api.domain.places

import cats.data.EitherT
import com.pwos.api.domain.PlaceAlreadyExistsError
import com.pwos.api.domain.PlaceNotFoundError


trait PlaceValidationAlgebra[F[_]] {

  def doesNotExists(place: Place): EitherT[F, PlaceAlreadyExistsError, Unit]

  def exists(placeId: Option[Long]): EitherT[F, PlaceNotFoundError.type, Unit]

}
