package com.pwos.api.domain.users

import cats.Monad
import cats.data.EitherT
import cats.data.OptionT
import cats.data.ValidatedNel
import cats.implicits._
import com.pwos.api.domain.HelloMountainsError._


class UserValidationInterpreter[F[_] : Monad](userDAO: UserDAOAlgebra[F]) extends UserValidationAlgebra[F] {

  override def doesNotExist(user: User): F[ValidatedNel[UserValidationError, Unit]] = {

    val validateUsername: String => F[ValidatedNel[UserValidationError, Unit]] = username => {
      val maybeUser: F[Option[User]] = userDAO.findByName(username)
      EitherT.fromOptionF(maybeUser, UserWithSameNameAlreadyExistsError(username)).toValidatedNel.map(_.map(_ => ()))
    }

    val validateEmail: String => F[ValidatedNel[UserValidationError, Unit]] = email => {
      val maybeUser: F[Option[User]] = userDAO.findByEmail(email)
      EitherT.fromOptionF(maybeUser, UserWithSameEmailAlreadyExistsError(email)).toValidatedNel.map(_.map(_ => ()))
    }

    validateUsername(user.userName).flatMap { validName =>
      validateEmail(user.email).map { validEmail =>
        (validName, validEmail).mapN { (_, _) => () }
      }
    }
  }

  override def exists(maybeUserId: Option[Long]): EitherT[F, UserNotFoundError.type, Unit] = {
    val maybeUserT: OptionT[F, User] = {
      OptionT.fromOption[F](maybeUserId) flatMap { userId: Long =>
        OptionT(userDAO.get(userId))
      }
    }

    EitherT.fromOptionF(maybeUserT.value, UserNotFoundError).map(_ => ())
  }

  override def validCredentials(createUserModel: UserModels.CreateUserModel): ValidatedNel[UserValidationError, Unit] = {
    val validateUsername: String => ValidatedNel[UserValidationError, Unit] = username => {
      if (username.length > 2) {
        ().validNel
      } else {
        IncorrectUserNameError(username).invalidNel
      }
    }

    val validateEmail: String => ValidatedNel[UserValidationError, Unit] = email => {
      val emailRegex = """^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"""
      if (email.matches(emailRegex)) {
        ().validNel
      } else {
        IncorrectEmailError(email).invalidNel
      }
    }

    val validatePassword: String => ValidatedNel[UserValidationError, Unit] = password => {
      val passwordRegex = """^(?=.*\d)(?=.*[A-Z])(?=.*[a-z])([^\s]){8,200}$"""
      if (password.matches(passwordRegex)) {
        ().validNel
      } else {
        IncorrectPasswordError.invalidNel
      }
    }

    val validatePasswordsCompatibility: (String, String) => ValidatedNel[UserValidationError, Unit] = (password, passwordCheck) => {
      if (password == passwordCheck) {
        ().validNel
      } else {
        PasswordsIncompatibleError.invalidNel
      }
    }


    (
      validateUsername(createUserModel.userName),
      validateEmail(createUserModel.email),
      validatePassword(createUserModel.password),
      validatePasswordsCompatibility(createUserModel.password, createUserModel.passwordCheck)
    ) mapN { (_, _, _, _) => () }
  }

}

object UserValidationInterpreter {
  def apply[F[_] : Monad](userDAO: UserDAOAlgebra[F]): UserValidationInterpreter[F] =
    new UserValidationInterpreter(userDAO)
}
