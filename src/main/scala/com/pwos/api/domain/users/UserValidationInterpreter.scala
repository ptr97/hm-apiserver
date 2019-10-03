package com.pwos.api.domain.users

import cats.Monad
import cats.data.EitherT
import cats.data.OptionT
import cats.data.Validated.Valid
import cats.data.ValidatedNel
import cats.implicits._
import com.pwos.api.domain.HelloMountainsError._
import com.pwos.api.domain.users.UserModels.UpdateUserCredentialsModel


class UserValidationInterpreter[F[_] : Monad](userDAO: UserDAOAlgebra[F]) extends UserValidationAlgebra[F] {

  private def validateFieldUniques(field: String, fetchByField: String => F[Option[User]], error: UserValidationError): F[ValidatedNel[UserValidationError, Unit]] = {
    val userF: F[Option[User]] = fetchByField(field)
    userF map {
      case Some(_) => error.invalidNel
      case None => ().validNel
    }
  }

  private def validateUserNameUniques(userName: String): F[ValidatedNel[UserValidationError, Unit]] = {
    validateFieldUniques(userName, userDAO.findByName, UserWithSameNameAlreadyExistsError(userName))
  }

  private def validateEmailUniques(email: String): F[ValidatedNel[UserValidationError, Unit]] = {
    validateFieldUniques(email, userDAO.findByEmail, UserWithSameEmailAlreadyExistsError(email))
  }

  override def doesNotExist(userName: String, email: String): F[ValidatedNel[UserValidationError, Unit]] = {
    validateUserNameUniques(userName).flatMap { validName =>
      validateEmailUniques(email).map { validEmail =>
        (validName, validEmail).mapN { (_, _) => () }
      }
    }
  }

  override def doesNotExist(maybeUserName: Option[String], maybeEmail: Option[String]): F[ValidatedNel[UserValidationError, Unit]] = {
    val validateUserNameF: F[ValidatedNel[UserValidationError, Unit]] = maybeUserName.map(validateUserNameUniques).getOrElse(Monad[F].pure(Valid(Unit)))
    val validateEmailF: F[ValidatedNel[UserValidationError, Unit]] = maybeEmail.map(validateEmailUniques).getOrElse(Monad[F].pure(Valid(Unit)))

    validateUserNameF flatMap { validUserName =>
      validateEmailF map { validEmail =>
        (validUserName, validEmail).mapN { (_, _) => () }
      }
    }
  }

  override def exists(maybeUserId: Option[Long]): EitherT[F, UserValidationError, Unit] = {
    val maybeUserT: OptionT[F, User] = {
      OptionT.fromOption[F](maybeUserId) flatMap { userId: Long =>
        OptionT(userDAO.get(userId))
      }
    }

    EitherT.fromOptionF(maybeUserT.value, UserNotFoundError: UserValidationError).map(_ => ())
  }

  override def validateUserCreationModel(createUserModel: UserModels.CreateUserModel): ValidatedNel[UserValidationError, Unit] = {
    (
      validateUsername(createUserModel.userName),
      validateEmail(createUserModel.email),
      validatePassword(createUserModel.password),
      validatePasswordsCompatibility(createUserModel.password, createUserModel.passwordCheck)
    ) mapN { (_, _, _, _) => () }
  }

  override def validateUserUpdateModel(updateUserCredentialsModel: UpdateUserCredentialsModel): ValidatedNel[UserValidationError, Unit] = {
    val validName: ValidatedNel[UserValidationError, Unit] = updateUserCredentialsModel.userName.map(validateUsername).getOrElse(().validNel)
    val validEmail: ValidatedNel[UserValidationError, Unit] = updateUserCredentialsModel.email.map(validateEmail).getOrElse(().validNel)

    (validName, validEmail) mapN { (_, _) => () }
  }

  override def validateChangePasswordModel(changePasswordModel: UserModels.ChangePasswordModel): ValidatedNel[UserValidationError, Unit] = {
    (
      validatePassword(changePasswordModel.newPassword),
      validatePasswordsCompatibility(changePasswordModel.newPassword, changePasswordModel.newPasswordCheck)
    ) mapN { (_, _) => () }
  }


  private def validateUsername(userName: String): ValidatedNel[UserValidationError, Unit] = {
    if (userName.length > 2) {
      ().validNel
    } else {
      IncorrectUserNameError(userName).invalidNel
    }
  }

  private def validateEmail(email: String): ValidatedNel[UserValidationError, Unit] = {
    val emailRegex = """^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"""
    if (email.matches(emailRegex)) {
      ().validNel
    } else {
      IncorrectEmailError(email).invalidNel
    }
  }

  private def validatePassword(password: String): ValidatedNel[UserValidationError, Unit] = {
    val passwordRegex = """^(?=.*\d)(?=.*[A-Z])(?=.*[a-z])([^\s]){8,200}$"""
    if (password.matches(passwordRegex)) {
      ().validNel
    } else {
      IncorrectPasswordError.invalidNel
    }
  }

  private def validatePasswordsCompatibility(password: String, passwordCheck: String): ValidatedNel[UserValidationError, Unit] = {
    if (password == passwordCheck) {
      ().validNel
    } else {
      PasswordsIncompatibleError.invalidNel
    }
  }

}

object UserValidationInterpreter {
  def apply[F[_] : Monad](userDAO: UserDAOAlgebra[F]): UserValidationInterpreter[F] =
    new UserValidationInterpreter(userDAO)
}
