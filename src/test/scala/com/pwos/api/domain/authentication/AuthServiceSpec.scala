package com.pwos.api.domain.authentication

import cats.Id
import com.pwos.api.domain.HelloMountainsError.IncorrectCredentials
import com.pwos.api.domain.HelloMountainsError.UserNotFoundError
import com.pwos.api.domain.users.User
import com.pwos.api.domain.users.UserModels.LoginModel
import com.pwos.api.domain.users.UserRole
import com.pwos.api.infrastructure.dao.memory.MemoryUserDAOInterpreter
import org.scalatest.FunSpec
import org.scalatest.Matchers


class AuthServiceSpec extends FunSpec with Matchers {

  private val userStephenPasswordPlain = "Password123"
  private val userStephenPasswordHashed = PasswordService.hash(userStephenPasswordPlain)
  private val userStephen: User = User("stephCurry", "steph@gsw.com", userStephenPasswordHashed, UserRole.User)


  describe("Getting User") {
    def getTestResources: (MemoryUserDAOInterpreter, AuthService[Id]) = {
      val memoryUserDAO = MemoryUserDAOInterpreter()
      val authService: AuthService[Id] = AuthService(memoryUserDAO)
      (memoryUserDAO, authService)
    }

    it("should return user when user exists") {
      val (userDAO, authService) = getTestResources
      val userFromDb: Id[User] = userDAO.create(userStephen)

      val result = authService.get(userFromDb.id.get).value
      result shouldBe Right(userFromDb)
    }

    it("should return UserNotFoundError when user doesn't exist") {
      val (userDAO, authService) = getTestResources
      val notExistingId: Long = userDAO.getLastId + 1

      val result = authService.get(notExistingId).value
      result shouldBe Left(UserNotFoundError)
    }

    it("should return UserNotFoundError when user is blocked") {
      val (userDAO, authService) = getTestResources
      val userFromDb: Id[User] = userDAO.create(userStephen.copy(banned = true))

      val result = authService.get(userFromDb.id.get).value
      result shouldBe Left(UserNotFoundError)
    }

    it("should return UserNotFoundError when user is marked as deleted") {
      val (userDAO, authService) = getTestResources
      val userFromDb: Id[User] = userDAO.create(userStephen.copy(deleted = true))

      val result = authService.get(userFromDb.id.get).value
      result shouldBe Left(UserNotFoundError)
    }

  }

  describe("Logging In User") {
    var userStephId: Long = 0

    def getTestResourcesWithTestData: (MemoryUserDAOInterpreter, AuthService[Id]) = {
      val memoryUserDAO = MemoryUserDAOInterpreter()
      val authService: AuthService[Id] = AuthService(memoryUserDAO)

      val userFromDb: Id[User] = memoryUserDAO.create(userStephen)
      userStephId = userFromDb.id.get

      (memoryUserDAO, authService)
    }

    it("should log user in when credentials are valid - login and password") {
      val (_, authService) = getTestResourcesWithTestData
      val validLoginModel = LoginModel(userStephen.userName, userStephenPasswordPlain)

      val result = authService.logIn(validLoginModel).value
      result shouldBe a [Right[_, _]]
    }

    it("should log user in when credentials are valid - email and password") {
      val (_, authService) = getTestResourcesWithTestData
      val validLoginModel = LoginModel(userStephen.email, userStephenPasswordPlain)

      val result = authService.logIn(validLoginModel).value
      result shouldBe a [Right[_, _]]
    }

    it("should not log user in when credentials are not valid - invalid login") {
      val (_, authService) = getTestResourcesWithTestData
      val validLoginModel = LoginModel("invalid_login", userStephenPasswordPlain)

      val result = authService.logIn(validLoginModel).value
      result shouldBe Left(IncorrectCredentials)
    }

    it ("should not log user in when credentials are not valid - invalid email") {
      val (_, authService) = getTestResourcesWithTestData
      val validLoginModel = LoginModel("invalid_mail@invalid.com", userStephenPasswordPlain)

      val result = authService.logIn(validLoginModel).value
      result shouldBe Left(IncorrectCredentials)
    }

    it("should not log user in when credentials are not valid - invalid password") {
      val (_, authService) = getTestResourcesWithTestData
      val validLoginModel = LoginModel(userStephen.email, "InvalidPassword12345")

      val result = authService.logIn(validLoginModel).value
      result shouldBe Left(IncorrectCredentials)
    }
  }
}
