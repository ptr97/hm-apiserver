package com.pwos.api.infastructure.authentication

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.MessageEntity
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.pwos.api.config.Config
import com.pwos.api.domain.HelloMountainsError.IncorrectCredentials
import com.pwos.api.domain.users.UserModels.LoginModel
import com.pwos.api.infastructure.DatabaseManager
import com.pwos.api.infastructure.Mocks._
import com.pwos.api.infrastructure.http.HttpResponses.ErrorResponse
import com.pwos.api.infrastructure.http.HttpResponses.SuccessResponse
import com.pwos.api.infrastructure.http.authentication.AuthController
import com.pwos.api.infrastructure.http.authentication.AuthController.LOG_IN
import com.pwos.api.infrastructure.http.authentication.JsonWebToken
import com.pwos.api.infrastructure.http.versions.v1
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import org.scalatest.BeforeAndAfter
import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.scalatest.concurrent.ScalaFutures
import slick.jdbc.MySQLProfile.backend.Database


class AuthIntegrationSpec extends FunSpec with Matchers with ScalatestRouteTest with ScalaFutures with BeforeAndAfter {

  val config: Config = Config.unsafeLoadTestConfig
  implicit val db: Database = config.database

  private def prepareContext: AuthController = {
    val (userDAO, userService, _) = userResources
    createAdmin(Users.admin, userService, userDAO)
    createUser(Users.user1, userService, userDAO)

    val (_, authController) = authResources
    authController
  }

  before {
    DatabaseManager.createDatabase(db)
  }

  after {
    DatabaseManager.cleanDatabase(db)
  }

  describe("Logging in - POST /v1/logIn") {
    it("should log in Admin by login") {
      val authController = prepareContext
      val logInModel = LoginModel(Users.admin.userName, Users.admin.password)
      val credentials: MessageEntity = Marshal(logInModel).to[MessageEntity].futureValue
      val request = Post(s"/$v1/$LOG_IN")
          .withEntity(credentials)

      request ~> authController.authRoutes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        val response: SuccessResponse[JsonWebToken] = entityAs[SuccessResponse[JsonWebToken]]
        response.data.token.nonEmpty shouldBe true
      }
    }

    it("should log in User by email") {
      val authController = prepareContext
      val logInModel = LoginModel(Users.user1.email, Users.user1.password)
      val credentials: MessageEntity = Marshal(logInModel).to[MessageEntity].futureValue
      val request = Post(s"/$v1/$LOG_IN")
        .withEntity(credentials)

      request ~> authController.authRoutes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        val response: SuccessResponse[JsonWebToken] = entityAs[SuccessResponse[JsonWebToken]]
        response.data.token.nonEmpty shouldBe true
      }
    }

    it("should return unauthorized response credentials are not valid") {
      val authController = prepareContext
      val logInModel = LoginModel("NotExistingUser", "NotExistingUserPass123")
      val credentials: MessageEntity = Marshal(logInModel).to[MessageEntity].futureValue
      val request = Post(s"/$v1/$LOG_IN")
        .withEntity(credentials)

      request ~> authController.authRoutes ~> check {
        status shouldBe StatusCodes.Unauthorized
        contentType shouldBe ContentTypes.`application/json`
        val response: ErrorResponse[IncorrectCredentials.type] = entityAs[ErrorResponse[IncorrectCredentials.type]]
        response.success shouldBe false
        response.error shouldBe IncorrectCredentials
      }
    }
  }
}
