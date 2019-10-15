package com.pwos.api.infastructure.opinions.tags

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.MessageEntity
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthorizationFailedRejection
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.pwos.api.config.Config
import com.pwos.api.domain.opinions.tags.Tag
import com.pwos.api.domain.opinions.tags.TagCategory
import com.pwos.api.domain.opinions.tags.TagModels.UpdateTagModel
import com.pwos.api.infastructure.DatabaseManager
import com.pwos.api.infastructure.Mocks._
import com.pwos.api.infrastructure.http.HttpResponses.SuccessResponse
import com.pwos.api.infrastructure.http.JsonImplicits._
import com.pwos.api.infrastructure.http.TagController
import com.pwos.api.infrastructure.http.TagController._
import com.pwos.api.infrastructure.http.versions._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import org.scalatest.BeforeAndAfter
import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.scalatest.concurrent.ScalaFutures
import slick.dbio.DBIO
import slick.jdbc.MySQLProfile.backend.Database

import scala.concurrent.Await
import scala.concurrent.duration.Duration


class TagIntegrationSpec extends FunSpec with Matchers with ScalatestRouteTest with ScalaFutures with BeforeAndAfter {

  val config: Config = Config.unsafeLoadTestConfig
  implicit val db: Database = config.database

  private def prepareContext: TagController = {
    val (userDAO, userService, userController) = userResources
    adminToken = createAdmin(Users.admin, userService, userDAO)
    user1Token = createUser(Users.user1, userService, userDAO)

    val (tagDAO, tagController) = tagResources
    val tagsCreation = List(tagDAO.create(Tags.tag1), tagDAO.create(Tags.tag2), tagDAO.create(Tags.tag3), tagDAO.create(Tags.tag4Disabled))
    Await.result(db.run(DBIO.sequence(tagsCreation)), Duration.Inf)

    tagController
  }

  private var adminToken: String = _
  private var user1Token: String = _

  before {
    DatabaseManager.createDatabase(db)
  }

  after {
    DatabaseManager.cleanDatabase(db)
  }

  describe("Tag creation - POST /v1/tags") {
    val newTag = Tag("New Tag", TagCategory.SUBSOIL, id = Some(5L))
    val tagJson: MessageEntity = Marshal(newTag).to[MessageEntity].futureValue

    it("should create Tag for Admin") {
      val tagController = prepareContext
      val request = Post(s"/$v1/$TAGS")
        .withEntity(tagJson)
        .withHeaders(authHeader(adminToken))

      request ~> tagController.tagRoutes ~> check {
        status shouldBe StatusCodes.Created
        contentType shouldBe ContentTypes.`application/json`
        val response: SuccessResponse[Tag] = entityAs[SuccessResponse[Tag]]
        response.data shouldBe newTag
      }
    }

    it("should reject request for non admin") {
      val tagController = prepareContext
      val request = Post(s"/$v1/$TAGS")
        .withEntity(tagJson)
        .withHeaders(authHeader(user1Token))

      request ~> tagController.tagRoutes ~> check {
        rejection shouldEqual AuthorizationFailedRejection
      }
    }
  }

  describe("Updating Tag status - PUT /v1/tags/<id>") {
    val updatedName = "Updated Name"
    val updateTagModel = UpdateTagModel(maybeName = Some(updatedName))
    val updateJson: MessageEntity = Marshal(updateTagModel).to[MessageEntity].futureValue

    it("should update Tag data for Admin") {
      val tagController = prepareContext
      val request = Put(s"/$v1/$TAGS/${Tags.tag1.id.get}")
        .withEntity(updateJson)
        .withHeaders(authHeader(adminToken))

      request ~> tagController.tagRoutes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        val response: SuccessResponse[Boolean] = entityAs[SuccessResponse[Boolean]]
        response.data shouldBe true
      }
    }

    it("should reject request for non admin") {
      val tagController = prepareContext
      val request = Put(s"/$v1/$TAGS/${Tags.tag1.id.get}")
        .withEntity(updateJson)
        .withHeaders(authHeader(user1Token))

      request ~> tagController.tagRoutes ~> check {
        rejection shouldEqual AuthorizationFailedRejection
      }
    }
  }

  describe("Listing all Tags (for Admin) - GET /v1/tags/all") {
    it("should list all active tags") {
      val tagController = prepareContext
      val request = Get(s"/$v1/$TAGS/all?active=true")
        .withHeaders(authHeader(adminToken))

      request ~> tagController.tagRoutes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        val response: SuccessResponse[List[Tag]] = entityAs[SuccessResponse[List[Tag]]]
        response.data shouldBe List(Tags.tag1, Tags.tag2, Tags.tag3)
      }
    }

    it("should list all inactive tags") {
      val tagController = prepareContext
      val request = Get(s"/$v1/$TAGS/all?active=false")
        .withHeaders(authHeader(adminToken))

      request ~> tagController.tagRoutes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        val response: SuccessResponse[List[Tag]] = entityAs[SuccessResponse[List[Tag]]]
        response.data shouldBe List(Tags.tag4Disabled)
      }
    }

    it("should reject request for non admin") {
      val tagController = prepareContext
      val request = Get(s"/$v1/$TAGS/all")
        .withHeaders(authHeader(user1Token))

      request ~> tagController.tagRoutes ~> check {
        rejection shouldEqual AuthorizationFailedRejection
      }
    }
  }

  describe("Listing active Tags - GET /v1/tags") {
    it("should list all tags") {
      val tagController = prepareContext
      val request = Get(s"/$v1/$TAGS")
        .withHeaders(authHeader(user1Token))

      request ~> tagController.tagRoutes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        val response: SuccessResponse[List[Tag]] = entityAs[SuccessResponse[List[Tag]]]
        response.data shouldBe List(Tags.tag1, Tags.tag2, Tags.tag3)
      }
    }
  }
}
