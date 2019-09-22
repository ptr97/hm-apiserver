package com.pwos.api.domain.places

import cats.data.EitherT
import com.pwos.api.domain.HelloMountainsError._


trait PlaceValidationAlgebra[F[_]] {

  def doesNotExists(place: Place): EitherT[F, PlaceAlreadyExistsError, Unit]

  def exists(placeId: Long): EitherT[F, PlaceNotFoundError.type, Unit]

}
