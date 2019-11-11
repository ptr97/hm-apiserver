package com.pwos.api.domain.places

import cats.data.EitherT
import com.pwos.api.domain.HelloMountainsError._
import com.pwos.api.domain.users.UserInfo


trait PlaceValidationAlgebra[F[_]] {

  def doesNotExist(place: Place): EitherT[F, PlaceAlreadyExistsError, Unit]

  def exists(placeId: Long): EitherT[F, PlaceNotFoundError.type, Unit]

  def validateAdminAccess(userInfo: UserInfo): Either[PlacePrivilegeError.type, Unit]
}
