package com.pwos.api.domain.users

import cats.data.EitherT
import cats.data.ValidatedNel
import com.pwos.api.domain.HelloMountainsError._
import com.pwos.api.domain.users.UserModels.CreateUserModel


trait UserValidationAlgebra[F[_]] {

  def doesNotExist(user: User): F[ValidatedNel[UserValidationError, Unit]]

  def exists(userId: Option[Long]): EitherT[F, UserNotFoundError.type, Unit]

  def validCredentials(createUserModel: CreateUserModel): ValidatedNel[UserValidationError, Unit]

}
