package com.pwos.api.infastructure.places

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.MessageEntity
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthorizationFailedRejection
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.pwos.api.config.Config
import com.pwos.api.domain.places.Place
import com.pwos.api.infastructure.DatabaseManager
import com.pwos.api.infastructure.Mocks.Places._
import com.pwos.api.infastructure.Mocks._
import com.pwos.api.infastructure._
import com.pwos.api.infrastructure.dao.slick.places.SlickPlaceDAOInterpreter
import com.pwos.api.infrastructure.http.HttpResponses.SuccessResponse
import com.pwos.api.infrastructure.http.PlaceController
import com.pwos.api.infrastructure.http.PlaceController._
import com.pwos.api.infrastructure.http.versions._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import org.scalatest.BeforeAndAfter
import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.scalatest.concurrent.ScalaFutures
import slick.jdbc.MySQLProfile.backend.Database


class PlaceIntegrationSpec extends FunSpec with Matchers with ScalatestRouteTest with ScalaFutures with BeforeAndAfter {

  val config: Config = Config.unsafeLoadTestConfig
  implicit val db: Database = config.database

  private def prepareContext: (SlickPlaceDAOInterpreter, PlaceController) = {
    val (userDAO, userService, _) = userResources
    adminToken = createAdmin(Mocks.Users.admin, userService, userDAO)
    userToken = createUser(Mocks.Users.user1, userService, userDAO)
    placeResources
  }

  private var adminToken: String = _
  private var userToken: String = _

  before {
    DatabaseManager.createDatabase(db)
  }

  after {
    DatabaseManager.cleanDatabase(db)
  }

  describe("Adding new place - POST /v1/places") {

    it("should create new place") {
      val (_, placeController) = prepareContext
      val placeJson: MessageEntity = Marshal(place1).to[MessageEntity].futureValue
      val request = Post(s"/$v1/$PLACES")
        .withHeaders(authHeader(adminToken))
        .withEntity(placeJson)

      request ~> placeController.placeRoutes ~> check {
        status shouldBe StatusCodes.Created
        contentType shouldBe ContentTypes.`application/json`
        val response: SuccessResponse[Place] = entityAs[SuccessResponse[Place]]
        response.data shouldBe place1
      }
    }

    it("should reject request for non admin") {
      val (_, placeController) = prepareContext
      val placeJson: MessageEntity = Marshal(place1).to[MessageEntity].futureValue
      val request = Post(s"/$v1/$PLACES")
        .withHeaders(authHeader(userToken))
        .withEntity(placeJson)

      request ~> placeController.placeRoutes ~> check {
        rejection shouldEqual AuthorizationFailedRejection
      }
    }
  }

  describe("Getting single place - GET /v1/places/<id>") {
    it("should return existing place") {
      val (placeDAO, placeController) = prepareContext
      val placeFromDb: Place = db.run(placeDAO.create(place1)).futureValue
      val request = Get(s"/$v1/$PLACES/${placeFromDb.id.get}")
        .withHeaders(authHeader(userToken))

      request ~> placeController.placeRoutes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        val response: SuccessResponse[Place] = entityAs[SuccessResponse[Place]]
        response.data shouldBe placeFromDb
      }
    }
  }

  describe("Updating place") {
    it("should update place - PUT /v1/places/<id>") {
      val (placeDAO, placeController) = prepareContext
      val place1FromDb: Place = db.run(placeDAO.create(place1)).futureValue
      val placeJson: MessageEntity = Marshal(place2).to[MessageEntity].futureValue
      val request = Put(s"/$v1/$PLACES/${place1FromDb.id.get}")
        .withHeaders(authHeader(adminToken))
        .withEntity(placeJson)

      request ~> placeController.placeRoutes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        val response: SuccessResponse[Place] = entityAs[SuccessResponse[Place]]
        response.data shouldBe place2.copy(id = place1FromDb.id)
      }
    }

    it("should reject request for non admin") {
      val (placeDAO, placeController) = prepareContext
      val placeFromDb: Place = db.run(placeDAO.create(place1)).futureValue
      val placeJson: MessageEntity = Marshal(place2).to[MessageEntity].futureValue
      val request = Put(s"/$v1/$PLACES/${placeFromDb.id.get}")
        .withHeaders(authHeader(userToken))
        .withEntity(placeJson)

      request ~> placeController.placeRoutes ~> check {
        rejection shouldEqual AuthorizationFailedRejection
      }
    }
  }

  describe("Deleting place") {
    it("should update place - DELETE /v1/places/<id>") {
      val (placeDAO, placeController) = prepareContext
      val placeFromDb: Place = db.run(placeDAO.create(place1)).futureValue
      val request = Delete(s"/$v1/$PLACES/${placeFromDb.id.get}")
        .withHeaders(authHeader(adminToken))

      request ~> placeController.placeRoutes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        val response: SuccessResponse[String] = entityAs[SuccessResponse[String]]
        response.data shouldBe "Place deleted"
      }
    }

    it("should reject request for non admin") {
      val (placeDAO, placeController) = prepareContext
      val placeFromDb: Place = db.run(placeDAO.create(place1)).futureValue
      val request = Delete(s"/$v1/$PLACES/${placeFromDb.id.get}")
        .withHeaders(authHeader(userToken))

      request ~> placeController.placeRoutes ~> check {
        rejection shouldEqual AuthorizationFailedRejection
      }
    }
  }

  describe("Listing places") {
    it("should list all places on GET /v1/places") {
      val (placeDAO, placeController) = prepareContext
      val place1FromDb: Place = db.run(placeDAO.create(place1)).futureValue
      val place2FromDb: Place = db.run(placeDAO.create(place2)).futureValue
      val place3FromDb: Place = db.run(placeDAO.create(place3)).futureValue
      val request = Get(s"/$v1/$PLACES")
        .withHeaders(authHeader(userToken))

      request ~> placeController.placeRoutes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        val response: SuccessResponse[List[Place]] = entityAs[SuccessResponse[List[Place]]]
        response.data shouldBe List(place1FromDb, place2FromDb, place3FromDb)
      }
    }
  }

}
