package com.pwos.api.domain.users

import cats.Id
import cats.data.NonEmptyList
import cats.implicits._
import com.pwos.api.domain.HelloMountainsError._
import com.pwos.api.domain.users.UserModels.CreateUserModel
import com.pwos.api.infrastructure.dao.memory.MemoryUserDAOInterpreter
import org.scalatest.FunSpec
import org.scalatest.Matchers


class UserServiceSpec extends FunSpec with Matchers {

  private def getTestResources: (MemoryUserDAOInterpreter, UserService[Id]) = {
    val memoryUserDAO = MemoryUserDAOInterpreter()
    val userValidation: UserValidationInterpreter[Id] = UserValidationInterpreter(memoryUserDAO)
    val userService: UserService[Id] = UserService(memoryUserDAO, userValidation)
    (memoryUserDAO, userService)
  }

  private val userStephen: User = User("stephCurry", "steph@gsw.com", "Password123", UserRole.User)
  private val userKlay: User = User("klayThompson", "klay@gsw.com", "SecretPass123", UserRole.User)
  private val admin: User = User("steveKerr", "steve@gsw.com", "Secret123", UserRole.Admin)

  describe("Creating a new user - user registration") {

    val createStephen = CreateUserModel(userStephen.userName, userStephen.email, userStephen.password, userStephen.password)

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
      val tooShortPassword: String = userStephen.password.substring(0, 2)
      val addUserResult: Id[Either[NonEmptyList[UserValidationError], UserView]] =
        userService.create(createStephen.copy(password = tooShortPassword, passwordCheck = tooShortPassword)).value

      addUserResult shouldBe Left(NonEmptyList.of(IncorrectPasswordError))
    }

    it("should not add user when password check doesn't match to password") {
      val (_, userService) = getTestResources
      val addUserResult: Id[Either[NonEmptyList[UserValidationError], UserView]] =
        userService.create(createStephen.copy(passwordCheck = userStephen.password + "this_wont_match")).value

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
      val tooShortPassword: String = userStephen.password.substring(0, 2)
      val addUserResult: Id[Either[NonEmptyList[UserValidationError], UserView]] =
        userService.create(createStephen.copy(password = tooShortPassword, email = incorrectEmail)).value

      addUserResult.leftMap(_.toList).swap.toOption.get should contain theSameElementsAs
        List(IncorrectEmailError(incorrectEmail), PasswordsIncompatibleError, IncorrectPasswordError)
    }


  }
}
