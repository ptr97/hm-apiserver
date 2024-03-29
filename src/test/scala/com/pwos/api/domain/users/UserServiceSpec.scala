package com.pwos.api.domain.users

import cats.Id
import cats.data.NonEmptyList
import cats.implicits._
import com.pwos.api.domain.HelloMountainsError._
import com.pwos.api.domain.PagingRequest
import com.pwos.api.domain.QueryParameters
import com.pwos.api.domain.authentication.PasswordService
import com.pwos.api.domain.users.UserModels.ChangePasswordModel
import com.pwos.api.domain.users.UserModels.CreateUserModel
import com.pwos.api.domain.users.UserModels.UpdateUserCredentialsModel
import com.pwos.api.domain.users.UserModels.UpdateUserStatusModel
import com.pwos.api.infrastructure.dao.memory.MemoryUserDAOInterpreter
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers


class UserServiceSpec extends AnyFunSpec with Matchers {

  private def getTestResources: (MemoryUserDAOInterpreter, UserService[Id]) = {
    val memoryUserDAO = MemoryUserDAOInterpreter()
    val adminFromDb: Id[User] = memoryUserDAO.create(admin)
    adminView = adminFromDb.toView.get
    adminUserInfo = UserInfo.forUser(adminFromDb)

    val userValidation: UserValidationInterpreter[Id] = UserValidationInterpreter(memoryUserDAO)
    val userService: UserService[Id] = UserService(memoryUserDAO, userValidation)
    (memoryUserDAO, userService)
  }

  private val admin: User = User("admin", "admin@hm.com", "Secret123", UserRole.Admin)
  private var adminView: UserView = _
  private var adminUserInfo: UserInfo = _

  private val userStephenPasswordPlain = "Password123"
  private val userStephenPasswordHashed = PasswordService.hash(userStephenPasswordPlain)
  private val userStephen: User = User("stephCurry", "steph@gsw.com", userStephenPasswordHashed, UserRole.User)

  private val userKlay: User = User("klayThompson", "klay@gsw.com", "SecretPass123", UserRole.User)
  private val userKevin: User = User("kevinDurant", "kevin@gsw.com", "SecretPass123", UserRole.User)


  describe("Creating a new user - user registration") {

    val createStephen = CreateUserModel(userStephen.userName, userStephen.email, userStephenPasswordPlain, userStephenPasswordPlain)

    it("should add new user") {
      val (userDAO, userService) = getTestResources
      val addUserResult: Id[Either[NonEmptyList[UserValidationError], UserView]] = userService.create(createStephen).value
      val userFromDb: UserView = userDAO.findByName(userStephen.userName).flatMap(_.toView).get

      addUserResult shouldBe Right(userFromDb)
      addUserResult.map(_.id) shouldBe Right(userFromDb.id)
      addUserResult.map(_.userName) shouldBe Right(userFromDb.userName)
      addUserResult.map(_.email) shouldBe Right(userFromDb.email)
    }

    it("should not add user when exactly same user exists") {
      val (userDAO, userService) = getTestResources
      userDAO.create(userStephen)
      val addUserResult: Id[Either[NonEmptyList[UserValidationError], UserView]] = userService.create(createStephen).value

      addUserResult shouldBe Left(NonEmptyList.of(UserWithSameNameAlreadyExistsError(userStephen.userName), UserWithSameEmailAlreadyExistsError(userStephen.email)))
    }

    it("should not add user when user with same name exists") {
      val (userDAO, userService) = getTestResources
      userDAO.create(userStephen)
      val addUserResult: Id[Either[NonEmptyList[UserValidationError], UserView]] = userService.create(createStephen.copy(email = userKlay.email)).value

      addUserResult shouldBe Left(NonEmptyList.of(UserWithSameNameAlreadyExistsError(userStephen.userName)))
    }

    it("should not add user when user with same email exists") {
      val (userDAO, userService) = getTestResources
      userDAO.create(userStephen)
      val addUserResult: Id[Either[NonEmptyList[UserValidationError], UserView]] = userService.create(createStephen.copy(userName = userKlay.userName)).value

      addUserResult shouldBe Left(NonEmptyList.of(UserWithSameEmailAlreadyExistsError(userStephen.email)))
    }

    it("should not add user when user name is too short") {
      val (_, userService) = getTestResources
      val tooShortName: String = userStephen.userName.substring(0, 2)
      val addUserResult: Id[Either[NonEmptyList[UserValidationError], UserView]] = userService.create(createStephen.copy(userName = tooShortName)).value

      addUserResult shouldBe Left(NonEmptyList.of(IncorrectUserNameError(tooShortName)))
    }

    it("should not add user when email is invalid email address") {
      val (_, userService) = getTestResources
      val incorrectEmail: String = "incorrect_email!"
      val addUserResult: Id[Either[NonEmptyList[UserValidationError], UserView]] = userService.create(createStephen.copy(email = incorrectEmail)).value

      addUserResult shouldBe Left(NonEmptyList.of(IncorrectEmailError(incorrectEmail)))
    }

    it("should not add user when password is too short") {
      val (_, userService) = getTestResources
      val tooShortPassword: String = userStephenPasswordPlain.substring(0, 2)
      val addUserResult: Id[Either[NonEmptyList[UserValidationError], UserView]] =
        userService.create(createStephen.copy(password = tooShortPassword, passwordCheck = tooShortPassword)).value

      addUserResult shouldBe Left(NonEmptyList.of(IncorrectPasswordError))
    }

    it("should not add user when password check doesn't match to password") {
      val (_, userService) = getTestResources
      val addUserResult: Id[Either[NonEmptyList[UserValidationError], UserView]] =
        userService.create(createStephen.copy(passwordCheck = userStephenPasswordPlain + "this_wont_match")).value

      addUserResult shouldBe Left(NonEmptyList.of(PasswordsIncompatibleError))
    }

    it("should not add user when password doesn't have numbers in it") {
      val (_, userService) = getTestResources
      val passwordWithoutNumbers: String = "PasswordWithoutMNumbers"
      val addUserResult: Id[Either[NonEmptyList[UserValidationError], UserView]] =
        userService.create(createStephen.copy(password = passwordWithoutNumbers, passwordCheck = passwordWithoutNumbers)).value

      addUserResult shouldBe Left(NonEmptyList.of(IncorrectPasswordError))
    }

    it("should not add user when password doesn't have capital letter in it") {
      val (_, userService) = getTestResources
      val passwordWithoutCapitalLetter: String = "passwordwithoutcapitalletter123"
      val addUserResult: Id[Either[NonEmptyList[UserValidationError], UserView]] =
        userService.create(createStephen.copy(password = passwordWithoutCapitalLetter, passwordCheck = passwordWithoutCapitalLetter)).value

      addUserResult shouldBe Left(NonEmptyList.of(IncorrectPasswordError))
    }

    it("should return all errors when email is invalid, password is too short and doesn't match password check") {
      val (_, userService) = getTestResources
      val incorrectEmail: String = "incorrect_email!"
      val tooShortPassword: String = userStephenPasswordPlain.substring(0, 2)
      val addUserResult: Id[Either[NonEmptyList[UserValidationError], UserView]] =
        userService.create(createStephen.copy(password = tooShortPassword, email = incorrectEmail)).value

      addUserResult.leftMap(_.toList).swap.toOption.get should contain theSameElementsAs
        List(IncorrectEmailError(incorrectEmail), PasswordsIncompatibleError, IncorrectPasswordError)
    }
  }

  describe("Getting simple user view") {
    it("should return simple user view") {
      val (userDAO, userService) = getTestResources
      val userFromDb: Id[User] = userDAO.create(userStephen)
      val getResult: Id[Either[UserNotFoundError.type, UserView]] = userService.getSimpleView(userFromDb.id.get).value

      getResult shouldBe Right(UserView(userFromDb.id.get, userStephen.userName, userStephen.email))
    }

    it("should UserNotFoundError when user doesn't exist") {
      val (userDAO, userService) = getTestResources
      val notExistingId: Long = userDAO.getLastId + 1
      val getResult: Id[Either[UserNotFoundError.type, UserView]] = userService.getSimpleView(notExistingId).value

      getResult shouldBe Left(UserNotFoundError)
    }
  }

  describe("Getting full user data") {
    it("should return full user data") {
      val (userDAO, userService) = getTestResources
      val userFromDb: Id[User] = userDAO.create(userStephen)
      val getResult = userService.getFullData(adminUserInfo, userFromDb.id.get).value

      getResult shouldBe Right(userStephen.copy(id = userFromDb.id))
    }

    it("should UserNotFoundError when user doesn't exist") {
      val (userDAO, userService) = getTestResources
      val notExistingId: Long = userDAO.getLastId + 1
      val getResult = userService.getFullData(adminUserInfo, notExistingId).value

      getResult shouldBe Left(UserNotFoundError)
    }
  }

  describe("Getting list of users") {

    val emptyQueryParams: QueryParameters = QueryParameters.empty
    val emptyPagingReq: PagingRequest = PagingRequest.empty

    it("should return empty list when there is no users - only with admin for call this function") {
      val (_, userService) = getTestResources
      val getAllUsersResult = userService.list(adminUserInfo, emptyQueryParams, emptyPagingReq).value

      getAllUsersResult.map(_.items) shouldBe Right(List(adminView))
      getAllUsersResult.map(_.totalCount) shouldBe Right(1)
      getAllUsersResult.map(_.hasNextPage) shouldBe Right(false)
    }

    it("should return all users") {
      val (userDAO, userService) = getTestResources
      val u1: UserView = userDAO.create(userStephen).map(_.toView).get
      val u2: UserView = userDAO.create(userKlay).map(_.toView).get
      val u3: UserView = userDAO.create(userKevin).map(_.toView).get
      val allUsers: List[UserView] = List(adminView, u1, u2, u3)

      val getAllUsersResult = userService.list(adminUserInfo, emptyQueryParams, emptyPagingReq).value.toOption.get

      getAllUsersResult.items shouldBe allUsers
      getAllUsersResult.totalCount shouldBe allUsers.length
      getAllUsersResult.hasNextPage shouldBe false
    }

    it("should return list of users with proper page size") {
      val (userDAO, userService) = getTestResources
      val u1: UserView = userDAO.create(userStephen).map(_.toView).get
      val u2: UserView = userDAO.create(userKlay).map(_.toView).get
      val u3: UserView = userDAO.create(userKevin).map(_.toView).get
      val allUsers: List[UserView] = List(adminView, u1, u2, u3)

      val pageSize = 2
      val pagingRequest: PagingRequest = PagingRequest(0, Some(pageSize), None)

      val getAllUsersResult = userService.list(adminUserInfo, emptyQueryParams, pagingRequest).value.toOption.get


      getAllUsersResult.items shouldBe allUsers.take(pageSize)
      getAllUsersResult.totalCount shouldBe allUsers.length
      getAllUsersResult.hasNextPage shouldBe true
    }

    it("should return list of users with proper offset") {
      val (userDAO, userService) = getTestResources
      val u1: UserView = userDAO.create(userStephen).map(_.toView).get
      val u2: UserView = userDAO.create(userKlay).map(_.toView).get
      val u3: UserView = userDAO.create(userKevin).map(_.toView).get
      val allUsers: List[UserView] = List(adminView, u1, u2, u3)

      val pageSize = 2
      val page = 1
      val pagingRequest: PagingRequest = PagingRequest(page, Some(pageSize), None)

      val getAllUsersResult = userService.list(adminUserInfo, emptyQueryParams, pagingRequest).value.toOption.get


      getAllUsersResult.items shouldBe allUsers.drop(page * pageSize)
      getAllUsersResult.totalCount shouldBe allUsers.length
      getAllUsersResult.hasNextPage shouldBe false
    }

    it("should return list of users with proper page size and offset") {
      val (userDAO, userService) = getTestResources
      val u1: UserView = userDAO.create(userStephen).map(_.toView).get
      val u2: UserView = userDAO.create(userKlay).map(_.toView).get
      val u3: UserView = userDAO.create(userKevin).map(_.toView).get
      val allUsers: List[UserView] = List(adminView, u1, u2, u3)

      val pageSize = 1
      val page = 1
      val pagingRequest: PagingRequest = PagingRequest(page, Some(pageSize), None)

      val getAllUsersResult = userService.list(adminUserInfo, emptyQueryParams, pagingRequest).value.toOption.get

      getAllUsersResult.items shouldBe List(u1)
      getAllUsersResult.totalCount shouldBe allUsers.length
      getAllUsersResult.hasNextPage shouldBe true
    }
  }

  describe("Updating user credentials") {
    it("should update all user credentials") {
      val (userDAO, userService) = getTestResources
      val userFromDb: Id[User] = userDAO.create(userStephen)
      val userId: Long = userFromDb.id.get
      val differentUserName = "differentUserName"
      val differentEmail = "differentEmail@gsw.com"
      val updateUserCredentialsModel = UpdateUserCredentialsModel(userName = Some(differentUserName), email = Some(differentEmail))

      val updateCredentialsResult: Id[Either[NonEmptyList[UserValidationError], UserView]] = userService.updateCredentials(userId, updateUserCredentialsModel).value

      updateCredentialsResult shouldBe Right(UserView(userId, differentUserName, differentEmail))

      val updatedUser: User = userDAO.get(userId).get
      updatedUser.userName shouldBe differentUserName
      updatedUser.email shouldBe differentEmail
    }

    it("should update user name only") {
      val (userDAO, userService) = getTestResources
      val userFromDb: Id[User] = userDAO.create(userStephen)
      val userId: Long = userFromDb.id.get
      val differentUserName = "differentUserName"
      val updateUserCredentialsModel = UpdateUserCredentialsModel(userName = Some(differentUserName))

      val updateCredentialsResult: Id[Either[NonEmptyList[UserValidationError], UserView]] = userService.updateCredentials(userId, updateUserCredentialsModel).value

      updateCredentialsResult shouldBe Right(UserView(userId, differentUserName, userStephen.email))
      val updatedUser: User = userDAO.get(userId).get
      updatedUser.userName shouldBe differentUserName
    }

    it("should update user email") {
      val (userDAO, userService) = getTestResources
      val userFromDb: Id[User] = userDAO.create(userStephen)
      val userId: Long = userFromDb.id.get
      val differentEmail = "differentEmail@gsw.com"
      val updateUserCredentialsModel = UpdateUserCredentialsModel(email = Some(differentEmail))

      val updateCredentialsResult: Id[Either[NonEmptyList[UserValidationError], UserView]] = userService.updateCredentials(userId, updateUserCredentialsModel).value

      updateCredentialsResult shouldBe Right(UserView(userId, userStephen.userName, differentEmail))
      val updatedUser: User = userDAO.get(userId).get
      updatedUser.email shouldBe differentEmail
    }

    it("should return proper error when user with same userName already exists") {
      val (userDAO, userService) = getTestResources
      val userStephFromDb: Id[User] = userDAO.create(userStephen)
      val userKlayFromDb: Id[User] = userDAO.create(userKlay)
      val updateUserCredentialsModel = UpdateUserCredentialsModel(userName = Some(userKlay.userName))

      val updateCredentialsResult: Id[Either[NonEmptyList[UserValidationError], UserView]] = userService.updateCredentials(userStephFromDb.id.get, updateUserCredentialsModel).value

      updateCredentialsResult shouldBe Left(NonEmptyList.of(UserWithSameNameAlreadyExistsError(userKlay.userName)))
    }

    it("should return proper error when user with same email already exists") {
      val (userDAO, userService) = getTestResources
      val userStephFromDb: Id[User] = userDAO.create(userStephen)
      val userKlayFromDb: Id[User] = userDAO.create(userKlay)
      val updateUserCredentialsModel = UpdateUserCredentialsModel(email = Some(userKlay.email))

      val updateCredentialsResult: Id[Either[NonEmptyList[UserValidationError], UserView]] = userService.updateCredentials(userStephFromDb.id.get, updateUserCredentialsModel).value

      updateCredentialsResult shouldBe Left(NonEmptyList.of(UserWithSameEmailAlreadyExistsError(userKlay.email)))
    }

    it("should accumulate errors when user with same userName and email already exists") {
      val (userDAO, userService) = getTestResources
      val userStephFromDb: Id[User] = userDAO.create(userStephen)
      val userKlayFromDb: Id[User] = userDAO.create(userKlay)
      val updateUserCredentialsModel = UpdateUserCredentialsModel(userName = Some(userKlay.userName), email = Some(userKlay.email))

      val updateCredentialsResult: Id[Either[NonEmptyList[UserValidationError], UserView]] = userService.updateCredentials(userStephFromDb.id.get, updateUserCredentialsModel).value

      updateCredentialsResult shouldBe Left(NonEmptyList.of(UserWithSameNameAlreadyExistsError(userKlay.userName), UserWithSameEmailAlreadyExistsError(userKlay.email)))
    }

    it("should return proper error when userName is too short") {
      val (userDAO, userService) = getTestResources
      val userStephFromDb: Id[User] = userDAO.create(userStephen)
      val tooShortName: String = userStephen.userName.substring(0, 2)
      val updateUserCredentialsModel = UpdateUserCredentialsModel(userName = Some(tooShortName))

      val updateCredentialsResult: Id[Either[NonEmptyList[UserValidationError], UserView]] = userService.updateCredentials(userStephFromDb.id.get, updateUserCredentialsModel).value

      updateCredentialsResult shouldBe Left(NonEmptyList.of(IncorrectUserNameError(tooShortName)))
    }

    it("should return proper error when email is invalid") {
      val (userDAO, userService) = getTestResources
      val userStephFromDb: Id[User] = userDAO.create(userStephen)
      val incorrectEmail: String = "incorrect_email!"
      val updateUserCredentialsModel = UpdateUserCredentialsModel(email = Some(incorrectEmail))

      val updateCredentialsResult: Id[Either[NonEmptyList[UserValidationError], UserView]] = userService.updateCredentials(userStephFromDb.id.get, updateUserCredentialsModel).value

      updateCredentialsResult shouldBe Left(NonEmptyList.of(IncorrectEmailError(incorrectEmail)))
    }
  }

  describe("Updating user password") {
    it("should update user password") {
      val (userDAO, userService) = getTestResources
      val userStephFromDb: Id[User] = userDAO.create(userStephen)
      val newPassword: String = "NewPassword123"
      val changePasswordModel: ChangePasswordModel = ChangePasswordModel(userStephenPasswordPlain, newPassword, newPassword)

      val changePasswordResult: Id[Either[NonEmptyList[UserValidationError], Boolean]] = userService.updatePassword(userStephFromDb.id.get, changePasswordModel).value

      changePasswordResult shouldBe Right(true)

      val userWithChangedPassword: Id[User] = userDAO.get(userStephFromDb.id.get).get
      PasswordService.compare(newPassword, userWithChangedPassword.password) shouldBe true
    }

    it("should return proper error when new password is too short") {
      val (userDAO, userService) = getTestResources
      val userStephFromDb: Id[User] = userDAO.create(userStephen)
      val newPassword: String = userStephenPasswordPlain.substring(0, 2)
      val changePasswordModel: ChangePasswordModel = ChangePasswordModel(userStephenPasswordPlain, newPassword, newPassword)

      val changePasswordResult: Id[Either[NonEmptyList[UserValidationError], Boolean]] = userService.updatePassword(userStephFromDb.id.get, changePasswordModel).value

      changePasswordResult shouldBe Left(NonEmptyList.of(IncorrectPasswordError))

      val userCheck: Id[User] = userDAO.get(userStephFromDb.id.get).get
      PasswordService.compare(newPassword, userCheck.password) shouldBe false
    }

    it("should return proper error when new password is has not capital letter") {
      val (userDAO, userService) = getTestResources
      val userStephFromDb: Id[User] = userDAO.create(userStephen)
      val newPassword: String = "passwordwithoutcapitalletter123"
      val changePasswordModel: ChangePasswordModel = ChangePasswordModel(userStephenPasswordPlain, newPassword, newPassword)

      val changePasswordResult: Id[Either[NonEmptyList[UserValidationError], Boolean]] = userService.updatePassword(userStephFromDb.id.get, changePasswordModel).value

      changePasswordResult shouldBe Left(NonEmptyList.of(IncorrectPasswordError))

      val userCheck: Id[User] = userDAO.get(userStephFromDb.id.get).get
      PasswordService.compare(newPassword, userCheck.password) shouldBe false
    }

    it("should return proper error when new password hasn't any digit") {
      val (userDAO, userService) = getTestResources
      val userStephFromDb: Id[User] = userDAO.create(userStephen)
      val newPassword: String = "PasswordWithoutDigits"
      val changePasswordModel: ChangePasswordModel = ChangePasswordModel(userStephenPasswordPlain, newPassword, newPassword)

      val changePasswordResult: Id[Either[NonEmptyList[UserValidationError], Boolean]] = userService.updatePassword(userStephFromDb.id.get, changePasswordModel).value

      changePasswordResult shouldBe Left(NonEmptyList.of(IncorrectPasswordError))

      val userCheck: Id[User] = userDAO.get(userStephFromDb.id.get).get
      PasswordService.compare(newPassword, userCheck.password) shouldBe false
    }

    it("should return proper error when provided password doesn't match old password") {
      val (userDAO, userService) = getTestResources
      val userStephFromDb: Id[User] = userDAO.create(userStephen)
      val newPassword: String = "NewPassword123"
      val changePasswordModel: ChangePasswordModel = ChangePasswordModel("wrongPassword", newPassword, newPassword)

      val changePasswordResult: Id[Either[NonEmptyList[UserValidationError], Boolean]] = userService.updatePassword(userStephFromDb.id.get, changePasswordModel).value

      changePasswordResult shouldBe Left(NonEmptyList.of(IncorrectCredentials))

      val userCheck: Id[User] = userDAO.get(userStephFromDb.id.get).get
      PasswordService.compare(newPassword, userCheck.password) shouldBe false
    }
  }

  describe("Updating user status - blocking and unblocking") {
    it("should block user") {
      val (userDAO, userService) = getTestResources
      val userFromDb: Id[User] = userDAO.create(userStephen)
      val newBannedStatus: Boolean = true
      val updateUserStatusModel = UpdateUserStatusModel(banned = newBannedStatus)
      val updateStatusResult: Id[Either[UserValidationError, User]] = userService.updateStatus(adminUserInfo, userFromDb.id.get, updateUserStatusModel).value

      updateStatusResult shouldBe Right(userFromDb.copy(banned = newBannedStatus))

      val userCheck: Id[User] = userDAO.get(userFromDb.id.get).get
      userCheck shouldBe userFromDb.copy(banned = true)
    }

    it("should unblock user") {
      val (userDAO, userService) = getTestResources
      val userFromDb: Id[User] = userDAO.create(userStephen)
      val bannedUserFromDb: Id[User] = userDAO.update(userFromDb.copy(banned = true)).get
      val newBannedStatus: Boolean = false
      val updateUserStatusModel = UpdateUserStatusModel(banned = newBannedStatus)
      val updateStatusResult = userService.updateStatus(adminUserInfo, userFromDb.id.get, updateUserStatusModel).value

      updateStatusResult shouldBe Right(bannedUserFromDb.copy(banned = newBannedStatus))
      val userCheck: Id[User] = userDAO.get(userFromDb.id.get).get
      userCheck.banned shouldBe newBannedStatus
    }

    it("should return proper error when user does not exist") {
      val (userDAO, userService) = getTestResources
      val notExistingId: Long = userDAO.getLastId + 1
      val newBannedStatus: Boolean = true
      val updateUserStatusModel = UpdateUserStatusModel(banned = newBannedStatus)

      val updateStatusResult: Id[Either[UserValidationError, User]] = userService.updateStatus(adminUserInfo, notExistingId, updateUserStatusModel).value

      updateStatusResult shouldBe Left(UserNotFoundError)
    }
  }

  describe("Deleting user") {
    it("should delete user") {
      val (userDAO, userService) = getTestResources
      val userFromDb: Id[User] = userDAO.create(userStephen)

      val updateStatusResult: Id[Either[UserValidationError, Boolean]] = userService.delete(userFromDb.id.get).value

      updateStatusResult shouldBe Right(true)

      val userCheck: Id[Option[User]] = userDAO.get(userFromDb.id.get)
      userCheck shouldBe None
    }

    it("should return proper error when user does not exist") {
      val (userDAO, userService) = getTestResources
      val notExistingId: Long = userDAO.getLastId + 1
      val updateStatusResult: Id[Either[UserValidationError, Boolean]] = userService.delete(notExistingId).value

      updateStatusResult shouldBe Left(UserNotFoundError)
    }
  }

}
