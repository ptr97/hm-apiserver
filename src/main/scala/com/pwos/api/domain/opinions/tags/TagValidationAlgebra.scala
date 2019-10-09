package com.pwos.api.domain.opinions.tags

import cats.data.EitherT
import com.pwos.api.domain.HelloMountainsError._
import com.pwos.api.domain.users.UserInfo


trait TagValidationAlgebra[F[_]] {

  def doesNotExists(tag: Tag): EitherT[F, TagAlreadyExistsError, Unit]

  def exists(placeId: Long): EitherT[F, TagNotFoundError.type, Unit]

  def validateAdminAccess(userInfo: UserInfo): Either[TagPrivilegeError.type, Unit]

}
