package com.pwos.api.infastructure.users

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.MessageEntity
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthorizationFailedRejection
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cats.implicits._
import com.pwos.api.PaginatedResult
import com.pwos.api.config.Config
import com.pwos.api.domain.users.User
import com.pwos.api.domain.users.UserModels._
import com.pwos.api.domain.users.UserView
import com.pwos.api.infastructure.DatabaseManager
import com.pwos.api.infastructure.Mocks._
import com.pwos.api.infrastructure.dao.slick.users.SlickUserDAOInterpreter
import com.pwos.api.infrastructure.http.HttpResponses.SuccessResponse
import com.pwos.api.infrastructure.http.JsonImplicits._
import com.pwos.api.infrastructure.http.UserController
import com.pwos.api.infrastructure.http.UserController._
import com.pwos.api.infrastructure.http.versions._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import org.scalatest.BeforeAndAfter
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.concurrent.ScalaFutures
import slick.jdbc.MySQLProfile.backend.Database

import scala.concurrent.Await
import scala.concurrent.duration.Duration


class UserIntegrationSpec extends AnyFunSpec with Matchers with ScalatestRouteTest with ScalaFutures with BeforeAndAfter {

  val config: Config = Config.unsafeLoadTestConfig
  implicit val db: Database = config.database

  private def prepareContext: UserController= {
    val (userDAO, userService, userController) = userResources
    adminToken = createAdmin(Users.admin, userService, userDAO)
    user1Token = createUser(Users.user1, userService, userDAO)
    userController
  }

  private var adminToken: String = _
  private var user1Token: String = _

  before {
    DatabaseManager.createDatabase(db)
  }

  after {
    DatabaseManager.cleanDatabase(db)
  }

  describe("Adding new user - POST /v1/users") {
    it("should create new user") {
      val userController = prepareContext
      val createUserModel = CreateUserModel(Users.user2.userName, Users.user2.email, Users.user2.password, Users.user2.password)
      val userJson: MessageEntity = Marshal(createUserModel).to[MessageEntity].futureValue
      val request = Post(s"/$v1/$USERS")
        .withEntity(userJson)

      request ~> userController.userRoutes ~> check {
        status shouldBe StatusCodes.Created
        contentType shouldBe ContentTypes.`application/json`
        val response: SuccessResponse[UserView] = entityAs[SuccessResponse[UserView]]
        response.data shouldBe Users.user2.toView.get
      }
    }
  }

  describe("Getting self data - GET /v1/users/me") {
    it("should return user data") {
      val userController = prepareContext
      val request = Get(s"/$v1/$USERS/me")
        .withHeaders(authHeader(user1Token))

      request ~> userController.userRoutes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        val response: SuccessResponse[UserView] = entityAs[SuccessResponse[UserView]]
        response.data shouldBe Users.user1.toView.get
      }
    }
  }

  describe("Updating self data - PUT /v1/users/me") {
    it("should update user data") {
      val userController = prepareContext
      val newUserName = "Another UserName"
      val updateUserCredentialsModel = UpdateUserCredentialsModel(userName = newUserName.some)
      val updateJson: MessageEntity = Marshal(updateUserCredentialsModel).to[MessageEntity].futureValue

      val request = Put(s"/$v1/$USERS/me")
        .withEntity(updateJson)
        .withHeaders(authHeader(user1Token))

      request ~> userController.userRoutes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        val response: SuccessResponse[UserView] = entityAs[SuccessResponse[UserView]]
        response.data shouldBe Users.user1.toView.get.copy(userName = newUserName)
      }
    }
  }

  describe("Updating password - PUT /v1/users/me/password") {
    it("should update user password") {
      val userController = prepareContext
      val newPassword = "NewPassword123"
      val changePasswordModel = ChangePasswordModel(oldPassword = Users.user1.password, newPassword, newPassword)
      val updateJson: MessageEntity = Marshal(changePasswordModel).to[MessageEntity].futureValue

      val request = Put(s"/$v1/$USERS/me/password")
        .withEntity(updateJson)
        .withHeaders(authHeader(user1Token))

      request ~> userController.userRoutes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        val response: SuccessResponse[Boolean] = entityAs[SuccessResponse[Boolean]]
        response.data shouldBe true
      }
    }
  }

  describe("Deleting account - DELETE /v1/users/me") {
    it("should delete user account") {
      val userController = prepareContext
      val request = Delete(s"/$v1/$USERS/me")
        .withHeaders(authHeader(user1Token))

      request ~> userController.userRoutes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        val response: SuccessResponse[Boolean] = entityAs[SuccessResponse[Boolean]]
        response.data shouldBe true
      }
    }
  }

  describe("Getting user by Admin - GET /v1/users/<id>") {
    it("should return user data") {
      val userController = prepareContext
      val request = Get(s"/$v1/$USERS/${Users.user1.id.get}")
        .withHeaders(authHeader(adminToken))

      request ~> userController.userRoutes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        val response: SuccessResponse[User] = entityAs[SuccessResponse[User]]
        response.data.copy(password = "") shouldBe Users.user1.copy(password = "")
      }
    }

    it("should reject request for non admin") {
      val userController = prepareContext
      val request = Get(s"/$v1/$USERS/${Users.user1.id.get}")
        .withHeaders(authHeader(user1Token))

      request ~> userController.userRoutes ~> check {
        rejection shouldEqual AuthorizationFailedRejection
      }
    }

  }

  describe("Updating user status by Admin - PUT /v1/users/<id>") {
    it("should update user status") {
      val userController = prepareContext
      val updateUserStatusModel = UpdateUserStatusModel(banned = true)
      val updateJson: MessageEntity = Marshal(updateUserStatusModel).to[MessageEntity].futureValue
      val request = Put(s"/$v1/$USERS/${Users.user1.id.get}")
        .withEntity(updateJson)
        .withHeaders(authHeader(adminToken))

      request ~> userController.userRoutes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        val response: SuccessResponse[User] = entityAs[SuccessResponse[User]]
        response.data.copy(password = "") shouldBe Users.user1.copy(banned = true, password = "")
      }
    }

    it("should reject request for non admin") {
      val userController = prepareContext
      val request = Put(s"/$v1/$USERS/${Users.user1.id.get}")
        .withHeaders(authHeader(user1Token))

      request ~> userController.userRoutes ~> check {
        rejection shouldEqual AuthorizationFailedRejection
      }
    }

  }

  describe("Listing users for Admin") {
    it("should list all users - GET /v1/users") {
      val userController = prepareContext
      val request = Get(s"/$v1/$USERS")
        .withHeaders(authHeader(adminToken))

      request ~> userController.userRoutes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        val response: SuccessResponse[PaginatedResult[UserView]] = entityAs[SuccessResponse[PaginatedResult[UserView]]]
        response.data shouldBe PaginatedResult(2, List(Users.admin.toView.get, Users.user1.toView.get), hasNextPage = false)
      }
    }

    it("should reject request for non admin") {
      val userController = prepareContext
      val request = Get(s"/$v1/$USERS")
        .withHeaders(authHeader(user1Token))

      request ~> userController.userRoutes ~> check {
        rejection shouldEqual AuthorizationFailedRejection
      }
    }

    def prepareContextForListing: (SlickUserDAOInterpreter, UserController) = {
      val (userDAO, userService, userController) = userResources
      adminToken = createAdmin(Users.admin, userService, userDAO)
      user1Token = createUser(Users.user1, userService, userDAO)
      createUser(Users.user2, userService, userDAO)
      createUser(Users.user3, userService, userDAO)
      createUser(Users.user4, userService, userDAO)
      (userDAO, userController)
    }

    it("should list users with proper offset and page size when next page exists") {
      val (_, userController) = prepareContextForListing
      val queryParams = s"page=1&pageSize=2&sortBy=id:asc"
      val request = Get(s"/$v1/$USERS?$queryParams")
        .withHeaders(authHeader(adminToken))

      request ~> userController.userRoutes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        val response: SuccessResponse[PaginatedResult[UserView]] = entityAs[SuccessResponse[PaginatedResult[UserView]]]
        response.data shouldBe PaginatedResult(5, List(Users.user2.toView.get, Users.user3.toView.get), hasNextPage = true)
      }
    }

    it("should list users with proper offset and page size when next page does not exist") {
      val (_, userController) = prepareContextForListing
      val queryParams = s"page=2&pageSize=2&sortBy=id:asc"
      val request = Get(s"/$v1/$USERS?$queryParams")
        .withHeaders(authHeader(adminToken))

      request ~> userController.userRoutes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        val response: SuccessResponse[PaginatedResult[UserView]] = entityAs[SuccessResponse[PaginatedResult[UserView]]]
        response.data shouldBe PaginatedResult(5, List(Users.user4.toView.get), hasNextPage = false)
      }
    }

    it("should find user by username and there is only one user fulfilling criteria") {
      val (_, userController) = prepareContextForListing
      val queryParams = s"search=admin"
      val request = Get(s"/$v1/$USERS?$queryParams")
        .withHeaders(authHeader(adminToken))

      request ~> userController.userRoutes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        val response: SuccessResponse[PaginatedResult[UserView]] = entityAs[SuccessResponse[PaginatedResult[UserView]]]
        response.data shouldBe PaginatedResult(1, List(Users.admin.toView.get), hasNextPage = false)
      }
    }

    it("should find user by username and there is many users fulfilling criteria") {
      val (_, userController) = prepareContextForListing
      val queryParams = s"search=Test%20User&page=0&pageSize=2&sortBy=id:asc"
      val request = Get(s"/$v1/$USERS?$queryParams")
        .withHeaders(authHeader(adminToken))

      request ~> userController.userRoutes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        val response: SuccessResponse[PaginatedResult[UserView]] = entityAs[SuccessResponse[PaginatedResult[UserView]]]
        response.data shouldBe PaginatedResult(4, List(Users.user1.toView.get, Users.user2.toView.get), hasNextPage = true)
      }
    }

    it("should find user by email") {
      val (_, userController) = prepareContextForListing
      val queryParams = s"search=admin@hm.com"
      val request = Get(s"/$v1/$USERS?$queryParams")
        .withHeaders(authHeader(adminToken))

      request ~> userController.userRoutes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        val response: SuccessResponse[PaginatedResult[UserView]] = entityAs[SuccessResponse[PaginatedResult[UserView]]]
        response.data shouldBe PaginatedResult(1, List(Users.admin.toView.get), hasNextPage = false)
      }
    }

    it("should filter users by status (banned)") {
      val (userDAO, userController) = prepareContextForListing

      Await.result(db.run(userDAO.update(Users.user2.copy(banned = true))), Duration.Inf)

      val queryParams = s"page=0&pageSize=2&sortBy=id:asc&filterBy=banned:true"
      val request = Get(s"/$v1/$USERS?$queryParams")
        .withHeaders(authHeader(adminToken))

      request ~> userController.userRoutes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        val response: SuccessResponse[PaginatedResult[UserView]] = entityAs[SuccessResponse[PaginatedResult[UserView]]]
        response.data shouldBe PaginatedResult(1, List(Users.user2.toView.get), hasNextPage = false)
      }
    }

    it("should filter users by role") {
      val (_, userController) = prepareContextForListing
      val queryParams = s"page=0&pageSize=2&sortBy=id:asc&filterBy=role:admin"
      val request = Get(s"/$v1/$USERS?$queryParams")
        .withHeaders(authHeader(adminToken))

      request ~> userController.userRoutes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        val response: SuccessResponse[PaginatedResult[UserView]] = entityAs[SuccessResponse[PaginatedResult[UserView]]]
        response.data shouldBe PaginatedResult(1, List(Users.admin.toView.get), hasNextPage = false)
      }
    }

    it("should sort users by username") {
      val (_, userController) = prepareContextForListing
      val queryParams = s"page=0&pageSize=3&sortBy=userName:asc&filterBy=role:user"
      val request = Get(s"/$v1/$USERS?$queryParams")
        .withHeaders(authHeader(adminToken))

      request ~> userController.userRoutes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        val response: SuccessResponse[PaginatedResult[UserView]] = entityAs[SuccessResponse[PaginatedResult[UserView]]]
        response.data shouldBe PaginatedResult(4, List(Users.user1.toView.get, Users.user2.toView.get, Users.user3.toView.get), hasNextPage = true)
      }
    }
  }

}
