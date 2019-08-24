package com.pwos.api.domain.users

import cats.Monad
import cats.data.EitherT
import cats.data.IdT
import cats.data.OptionT
import cats.data.ValidatedNel
import cats.implicits._
import com.pwos.api.domain.HelloMountainsError._
import com.pwos.api.domain.authentication.PasswordService
import com.pwos.api.domain.authentication.PasswordService.Password
import com.pwos.api.domain.users.UserModels._


class UserService[F[_] : Monad](userDAO: UserDAOAlgebra[F], userValidation: UserValidationAlgebra[F]) {

  def create(createUserModel: CreateUserModel): F[ValidatedNel[UserValidationError, UserView]] = {
    for {
      _ <- implicitly[Monad[F]].pure(userValidation.validateUserCreationModel(createUserModel))
      _ <- userValidation.doesNotExist(createUserModel.userName, createUserModel.email)
      hashedPassword = PasswordService.hash(createUserModel.password)
      newUser = User(userName = createUserModel.userName, email = createUserModel.email, password = hashedPassword)
      createdUser <- userDAO.create(newUser)
    } yield {
      createdUser.toView match {
        case Some(user) => user.validNel
        case _ => UserNotFoundError.invalidNel
      }
    }
  }

  def getSimpleView(id: Long): EitherT[F, UserNotFoundError.type, UserView] = {
    val userView: OptionT[F, UserView] = OptionT(userDAO.get(id)).flatMap { user =>
      OptionT.fromOption(user.toView)
    }

    EitherT.fromOptionF(userView.value, UserNotFoundError)
  }

  def getFullData(id: Long): EitherT[F, UserNotFoundError.type, User] = {
    EitherT.fromOptionF(userDAO.get(id), UserNotFoundError)
  }

  def list(pageSize: Option[Int], offset: Option[Int]): F[List[UserView]] = {
    val users: IdT[F, List[UserView]] = for {
      allUsers <- IdT(userDAO.all).map(_.sortBy(_.id)).map(_.flatMap(_.toView))
      withOffset = offset.map(off => allUsers.drop(off)).getOrElse(allUsers)
      result = pageSize.map(size => withOffset.take(size)).getOrElse(withOffset)
    } yield result

    users.value
  }

  def updateCredentials(id: Long, updateUserCredentialsModel: UpdateUserCredentialsModel): F[ValidatedNel[UserValidationError, UserView]] = {
    type UserUpdate = User => Option[User]

    val updateUsername: UserUpdate = user => updateUserCredentialsModel.userName.map(userName => user.copy(userName = userName))
    val updateEmail: UserUpdate = user => updateUserCredentialsModel.email.map(email => user.copy(email = email))

    val updates: List[UserUpdate] = List(updateUsername, updateEmail)

    val updateUserData: User => User = oldUser => {
      updates.foldLeft(oldUser)((user, updateFun) => updateFun(user).getOrElse(user))
    }

    for {
      _ <- implicitly[Monad[F]].pure(userValidation.validateUserUpdateModel(updateUserCredentialsModel))
      _ <- userValidation.exists(Option(id)).toValidatedNel
      userFromDb <- userDAO.get(id).map(_.get)
      updatedUser = updateUserData(userFromDb)
      updateResult <- EitherT.fromOptionF(userDAO.update(updatedUser).map(_.flatMap(_.toView)), UserNotFoundError).toValidatedNel
    } yield updateResult
  }

  def updatePassword(id: Long, changePasswordModel: ChangePasswordModel): F[ValidatedNel[UserValidationError, Boolean]] = {

    val validatePassword: (String, Password) => EitherT[F, UserValidationError, Unit] = (plainPassword, hashedPassword) => {
      if (PasswordService.compare(plainPassword, hashedPassword)) {
        EitherT.rightT(())
      } else {
        EitherT.leftT(IncorrectCredentials)
      }
    }

    for {
      _ <- implicitly[Monad[F]].pure(userValidation.validateChangePasswordModel(changePasswordModel))
      _ <- userValidation.exists(Option(id)).toValidatedNel
      user <- userDAO.get(id).map(_.get)
      _ <- validatePassword(changePasswordModel.oldPassword, user.password).toValidatedNel
      hashedPassword = PasswordService.hash(changePasswordModel.newPassword)
      userWithNewPassword: User = user.copy(password = hashedPassword)
      passwordUpdateResult <- userDAO.update(userWithNewPassword).map(_.isDefined).map(_.validNel)
    } yield passwordUpdateResult
  }

  def updateStatus(id: Long, updateUserStatusModel: UpdateUserStatusModel): EitherT[F, UserValidationError, User] = {
    for {
      _ <- userValidation.exists(Option(id))
      user <- EitherT.liftF(userDAO.get(id)).map(_.get)
      updatedUser <- EitherT.fromOptionF(userDAO.update(user.copy(banned = updateUserStatusModel.banned)), UserNotFoundError : UserValidationError)
    } yield updatedUser
  }

  def delete(id: Long): EitherT[F, UserNotFoundError.type, Boolean] = {
    for {
      _ <- userValidation.exists(Option(id))
      deleteResult <- EitherT.liftF(userDAO.markDeleted(id))
    } yield deleteResult
  }

}

object UserService {
  def apply[F[_]: Monad](userDAO: UserDAOAlgebra[F], userValidation: UserValidationAlgebra[F]): UserService[F] =
    new UserService(userDAO, userValidation)
}
