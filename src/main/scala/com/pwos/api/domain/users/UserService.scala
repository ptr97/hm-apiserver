package com.pwos.api.domain.users

import cats.Monad
import cats.data.EitherT
import cats.data.NonEmptyList
import cats.data.OptionT
import cats.implicits._
import com.pwos.api.PaginatedResult
import com.pwos.api.domain.HelloMountainsError._
import com.pwos.api.domain.PagingRequest
import com.pwos.api.domain.QueryParameters
import com.pwos.api.domain.authentication.PasswordService
import com.pwos.api.domain.authentication.PasswordService.Password
import com.pwos.api.domain.users.UserModels._


class UserService[F[_] : Monad](userDAO: UserDAOAlgebra[F], userValidation: UserValidationAlgebra[F]) {

  def create(createUserModel: CreateUserModel): EitherT[F, NonEmptyList[UserValidationError], UserView] = {
    def userWithHashedPassword: User = {
      val hashedPassword: Password = PasswordService.hash(createUserModel.password)
      User(userName = createUserModel.userName, email = createUserModel.email, password = hashedPassword)
    }

    EitherT(Monad[F].pure(userValidation.validateUserCreationModel(createUserModel).toEither)) flatMap { _ =>
      EitherT(userValidation.doesNotExist(createUserModel.userName, createUserModel.email).map(_.toEither)) flatMap { _ =>
        val userF: F[User] = userDAO.create(userWithHashedPassword)
        EitherT.fromOptionF(userF.map(_.toView), NonEmptyList.of(UserNotFoundError))
      }
    }

  }

  def getSimpleView(userId: Long): EitherT[F, UserNotFoundError.type, UserView] = {
    val userView: OptionT[F, UserView] = OptionT(userDAO.get(userId)).filterNot(_.banned).flatMap { user =>
      OptionT.fromOption(user.toView)
    }

    EitherT.fromOptionF(userView.value, UserNotFoundError)
  }

  def getFullData(userInfo: UserInfo, userId: Long): EitherT[F, UserValidationError, User] = {
    for {
      _ <- EitherT(Monad[F].pure(userValidation.validateAdminAccess(userInfo)))
      fullData <- EitherT.fromOptionF(userDAO.get(userId), UserNotFoundError: UserValidationError)
    } yield fullData
  }

  def list(userInfo: UserInfo, queryParameters: QueryParameters, pagingRequest: PagingRequest): EitherT[F, UserPrivilegeError.type, PaginatedResult[UserView]] = {
    for {
      _ <- EitherT(Monad[F].pure(userValidation.validateAdminAccess(userInfo)))
      users <- EitherT.liftF(userDAO.list(queryParameters, pagingRequest).map(_.flatMapResult(_.toView)))
    } yield users
  }

  def updateCredentials(userId: Long, updateUserCredentialsModel: UpdateUserCredentialsModel): EitherT[F, NonEmptyList[UserValidationError], UserView] = {
    type UserUpdate = User => Option[User]

    val updateUsername: UserUpdate = user => updateUserCredentialsModel.userName.map(name => user.copy(userName = name))
    val updateEmail: UserUpdate = user => updateUserCredentialsModel.email.map(email => user.copy(email = email))

    val updates: List[UserUpdate] = List(updateUsername, updateEmail)

    val updateUserData: User => User = oldUser => {
      updates.foldLeft(oldUser)((user, updateFun) => updateFun(user).getOrElse(user))
    }

    EitherT(Monad[F].pure(userValidation.validateUserUpdateModel(updateUserCredentialsModel).toEither)) flatMap { _ =>
      EitherT(userValidation.doesNotExist(updateUserCredentialsModel.userName, updateUserCredentialsModel.email).map(_.toEither)) flatMap { _ =>
        userValidation.exists(userId.some).leftMap(NonEmptyList.of(_)) flatMap { _ =>
          EitherT.fromOptionF(userDAO.get(userId), NonEmptyList.of(UserNotFoundError)) flatMap { user: User =>
            val updatedUser: EitherT[F, NonEmptyList[UserValidationError], User] = EitherT.fromOptionF(userDAO.update(updateUserData(user)), NonEmptyList.of(UserNotFoundError))
            updatedUser.map(_.toView.get)
          }
        }
      }
    }
  }

  def updatePassword(userId: Long, changePasswordModel: ChangePasswordModel): EitherT[F, NonEmptyList[UserValidationError], Boolean] = {

    val validatePassword: (String, Password) => EitherT[F, UserValidationError, Unit] = (plainPassword, hashedPassword) => {
      if (PasswordService.compare(plainPassword, hashedPassword)) {
        EitherT.rightT(())
      } else {
        EitherT.leftT(IncorrectCredentials)
      }
    }

    val getUserWithHashedPassword: User => User = user => {
      val hashedPassword: Password = PasswordService.hash(changePasswordModel.newPassword)
      user.copy(password = hashedPassword)
    }

    EitherT(Monad[F].pure(userValidation.validateChangePasswordModel(changePasswordModel).toEither)) flatMap { _ =>
      userValidation.exists(userId.some).leftMap(err => NonEmptyList.of(err)) flatMap { _ =>
        EitherT.fromOptionF(userDAO.get(userId), NonEmptyList.of(UserNotFoundError)) flatMap { user: User =>
          validatePassword(changePasswordModel.oldPassword, user.password).leftMap(NonEmptyList.of(_)) flatMap { _ =>
            val userWithHashedPassword: User = getUserWithHashedPassword(user)
            val updatedUser: EitherT[F, NonEmptyList[UserValidationError], User] = EitherT.fromOptionF(userDAO.update(userWithHashedPassword), NonEmptyList.of(UserNotFoundError))
            updatedUser.map { _ => true }
          }
        }
      }
    }
  }

  def updateStatus(userInfo: UserInfo, userId: Long, updateUserStatusModel: UpdateUserStatusModel): EitherT[F, UserValidationError, User] = {
    for {
      _ <- EitherT(Monad[F].pure(userValidation.validateAdminAccess(userInfo)))
      _ <- userValidation.exists(userId.some)
      user <- getFullData(userInfo, userId)
      updatedUser <- EitherT.fromOptionF(userDAO.update(user.copy(banned = updateUserStatusModel.banned)), UserNotFoundError: UserValidationError)
    } yield updatedUser
  }

  def delete(userId: Long): EitherT[F, UserValidationError, Boolean] = {
    for {
      _ <- userValidation.exists(userId.some)
      deleteResult <- EitherT.liftF(userDAO.delete(userId))
    } yield deleteResult
  }

}

object UserService {
  def apply[F[_] : Monad](userDAO: UserDAOAlgebra[F], userValidation: UserValidationAlgebra[F]): UserService[F] =
    new UserService(userDAO, userValidation)
}
