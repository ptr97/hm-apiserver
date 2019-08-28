package com.pwos.api.domain.users

import cats.data.EitherT
import cats.data.ValidatedNel
import com.pwos.api.domain.HelloMountainsError._
import com.pwos.api.domain.users.UserModels._


trait UserValidationAlgebra[F[_]] {

  def doesNotExist(userName: String, email: String): F[ValidatedNel[UserValidationError, Unit]]

  def doesNotExist(maybeUserName: Option[String], maybeEmail: Option[String]): F[ValidatedNel[UserValidationError, Unit]]

  def exists(userId: Option[Long]): EitherT[F, UserValidationError, Unit]

  def validateUserCreationModel(createUserModel: CreateUserModel): ValidatedNel[UserValidationError, Unit]

  def validateUserUpdateModel(updateUserModel: UpdateUserCredentialsModel): ValidatedNel[UserValidationError, Unit]

  def validateChangePasswordModel(changePasswordModel: ChangePasswordModel): ValidatedNel[UserValidationError, Unit]

}
