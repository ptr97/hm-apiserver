package com.pwos.api.infastructure.opinions

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.MessageEntity
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthorizationFailedRejection
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cats.implicits._
import com.pwos.api.PaginatedResult
import com.pwos.api.config.Config
import com.pwos.api.domain.opinions.OpinionModels._
import com.pwos.api.domain.opinions.reports.ReportCategory
import com.pwos.api.domain.opinions.reports.ReportCategory.ReportCategory
import com.pwos.api.domain.opinions.reports.ReportView
import com.pwos.api.infastructure.DatabaseManager
import com.pwos.api.infastructure.Mocks._
import com.pwos.api.infrastructure.dao.slick.opinions.reports.SlickReportDAOInterpreter
import com.pwos.api.infrastructure.dao.slick.opinions.tags.SlickTagDAOInterpreter
import com.pwos.api.infrastructure.dao.slick.places.SlickPlaceDAOInterpreter
import com.pwos.api.infrastructure.http.HttpResponses.SuccessResponse
import com.pwos.api.infrastructure.http.JsonImplicits._
import com.pwos.api.infrastructure.http.OpinionController
import com.pwos.api.infrastructure.http.OpinionController._
import com.pwos.api.infrastructure.http.PlaceController.PLACES
import com.pwos.api.infrastructure.http.versions._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import org.joda.time.DateTime
import org.scalatest.BeforeAndAfter
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.concurrent.ScalaFutures
import slick.jdbc.MySQLProfile.backend.Database

import scala.concurrent.Await
import scala.concurrent.duration.Duration


class OpinionIntegrationSpec extends AnyFunSpec with Matchers with ScalatestRouteTest with ScalaFutures with BeforeAndAfter {

  val config: Config = Config.unsafeLoadTestConfig
  implicit val db: Database = config.database

  private def prepareContext: OpinionController = {
    val (userDAO, userService, _) = userResources
    adminToken = createAdmin(Users.admin, userService, userDAO)
    user1Token = createUser(Users.user1, userService, userDAO)
    createUser(Users.user2, userService, userDAO)

    val placeDAO = SlickPlaceDAOInterpreter(executor)
    val placesCreation = placeDAO.create(Places.place1) >> placeDAO.create(Places.place2)


    val tagDAO = SlickTagDAOInterpreter(executor)
    val tagsCreation =
      tagDAO.create(Tags.tag1) >>
      tagDAO.create(Tags.tag2) >>
      tagDAO.create(Tags.tag3) >>
      tagDAO.create(Tags.tag4Disabled)

    val (opinionDAO, opinionController) = opinionResources
    val opinionsCreation =
      opinionDAO.create(Opinions.opinion1) >>
      opinionDAO.create(Opinions.opinion2) >>
      opinionDAO.create(Opinions.opinion3) >>
      opinionDAO.create(Opinions.opinion4) >>
      opinionDAO.create(Opinions.opinion5ForPlace2) >>
      opinionDAO.create(Opinions.opinion6ForPlace2)


    val opinionsTagsCreation =
      opinionDAO.addTags(Opinions.opinion1.id.get, List(Tags.tag1.id.get)) >>
      opinionDAO.addTags(Opinions.opinion1.id.get, List(Tags.tag2.id.get)) >>
      opinionDAO.addTags(Opinions.opinion2.id.get, List(Tags.tag2.id.get)) >>
      opinionDAO.addTags(Opinions.opinion2.id.get, List(Tags.tag3.id.get)) >>
      opinionDAO.addTags(Opinions.opinion3.id.get, List(Tags.tag4Disabled.id.get))


    val allActions = db.run(placesCreation) >> db.run(tagsCreation) >> db.run(opinionsCreation) >> db.run(opinionsTagsCreation)
    Await.result(allActions, Duration.Inf)

    opinionController
  }

  private var adminToken: String = _
  private var user1Token: String = _


  before {
    DatabaseManager.createDatabase(db)
  }

  after {
    DatabaseManager.cleanDatabase(db)
  }

  describe("Adding new Opinion - POST /v1/places/<place_id>/opinions") {
    it("should add new opinion") {
      val opinionController = prepareContext
      val placeId: Long = Places.place1.id.get
      val newOpinionBody = "New opinion body".some
      val createOpinionModel = CreateOpinionModel(newOpinionBody, List(Tags.tag1.id.get))
      val createJson: MessageEntity = Marshal(createOpinionModel).to[MessageEntity].futureValue
      val request = Post(s"/$v1/$PLACES/$placeId/$OPINIONS")
        .withEntity(createJson)
        .withHeaders(authHeader(user1Token))

      request ~> opinionController.opinionRoutes ~> check {
        status shouldBe StatusCodes.Created
        contentType shouldBe ContentTypes.`application/json`
        val response: SuccessResponse[OpinionView] = entityAs[SuccessResponse[OpinionView]]

        response.data.tags shouldBe List(Tags.tag1.name)
        response.data.likes shouldBe OpinionLikes.empty

        response.data.opinion.body shouldBe newOpinionBody
        response.data.opinion.placeId shouldBe placeId
        response.data.opinion.authorId shouldBe Users.user1.id.get
      }
    }
  }

  describe("Deleting Opinion - DELETE /v1/opinions/<opinion_id>") {
    it("should delete opinion") {
      val opinionController = prepareContext
      val request = Delete(s"/$v1/$OPINIONS/${Opinions.opinion1.id.get}")
        .withHeaders(authHeader(user1Token))

      request ~> opinionController.opinionRoutes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        val response: SuccessResponse[String] = entityAs[SuccessResponse[String]]
        response.data shouldBe "Opinion deleted"
      }
    }
  }

  describe("Updating Opinion - PUT /v1/opinions/<opinion_id>") {
    it("should update opinion") {
      val opinionController = prepareContext
      val updatedBody = "Updated opinion body"
      val updateOpinionModel = UpdateOpinionModel(updatedBody.some)
      val updateJson: MessageEntity = Marshal(updateOpinionModel).to[MessageEntity].futureValue
      val request = Put(s"/$v1/$OPINIONS/${Opinions.opinion1.id.get}")
        .withEntity(updateJson)
        .withHeaders(authHeader(user1Token))

      request ~> opinionController.opinionRoutes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        val response: SuccessResponse[Boolean] = entityAs[SuccessResponse[Boolean]]
        response.data shouldBe true
      }
    }
  }

  describe("Getting list of Opinions for Place - GET /v1/places/<place_id>/opinions") {
    it("should return opinions list with proper paging") {
      val opinionController = prepareContext
      val placeId: Long = Places.place1.id.get
      val request = Get(s"/$v1/$PLACES/$placeId/$OPINIONS?page=0&pageSize=2")
        .withHeaders(authHeader(user1Token))

      request ~> opinionController.opinionRoutes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        val response: SuccessResponse[PaginatedResult[OpinionView]] = entityAs[SuccessResponse[PaginatedResult[OpinionView]]]
        val MOCK_DATE: DateTime = DateTime.now
        val opinionViews = response.data.items.map { opinionView =>
          opinionView.copy(opinion = opinionView.opinion.copy(referenceDate = MOCK_DATE, creationDate = MOCK_DATE, lastModified = MOCK_DATE))
        }

        opinionViews shouldBe List(
          OpinionView(opinion = Opinions.opinion4.copy(referenceDate = MOCK_DATE, creationDate = MOCK_DATE, lastModified = MOCK_DATE), tags = List.empty, likes = OpinionLikes.empty),
          OpinionView(opinion = Opinions.opinion3.copy(referenceDate = MOCK_DATE, creationDate = MOCK_DATE, lastModified = MOCK_DATE), tags = List(Tags.tag4Disabled.name), likes = OpinionLikes.empty)
        )
        response.data.hasNextPage shouldBe true
        response.data.totalCount shouldBe 4
      }
    }
  }

  describe("Adding likes to opinion - PUT /v1/opinions/<id>/likes") {
    it("should add like to opinion") {
      val opinionController = prepareContext
      val like = UpdateOpinionLikesModel(like = true)
      val updateJson: MessageEntity = Marshal(like).to[MessageEntity].futureValue
      val request = Put(s"/$v1/$OPINIONS/${Opinions.opinion1.id.get}/likes")
        .withEntity(updateJson)
        .withHeaders(authHeader(user1Token))

      request ~> opinionController.opinionRoutes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        val response: SuccessResponse[Boolean] = entityAs[SuccessResponse[Boolean]]
        response.data shouldBe true
      }
    }
  }

  describe("Getting Report categories - GET /v1/opinions/reports") {
    it("should get possible reports reasons") {
      val opinionController = prepareContext
      val request = Get(s"/$v1/$OPINIONS/$REPORTS")
        .withHeaders(authHeader(user1Token))

      request ~> opinionController.opinionRoutes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        val response: SuccessResponse[List[ReportCategory]] = entityAs[SuccessResponse[List[ReportCategory]]]
        response.data shouldBe List(ReportCategory.MISLEADING, ReportCategory.VULGAR, ReportCategory.FAULTY)
      }
    }
  }

  describe("Reporting Opinion - POST /v1/opinions/<id>/reports") {
    it("should report Opinion") {
      val opinionController = prepareContext
      val like = ReportOpinionModel("I don't like it".some, ReportCategory.FAULTY)
      val updateJson: MessageEntity = Marshal(like).to[MessageEntity].futureValue
      val request = Post(s"/$v1/$OPINIONS/${Opinions.opinion1.id.get}/$REPORTS")
        .withEntity(updateJson)
        .withHeaders(authHeader(user1Token))

      request ~> opinionController.opinionRoutes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        val response: SuccessResponse[String] = entityAs[SuccessResponse[String]]
        response.data shouldBe "Opinion reported"
      }
    }
  }


  describe("Updating Opinion status - PUT /v1/opinions/<id>/status") {
    val updateOpinionStatusModel = UpdateOpinionStatusModel(blocked = true)
    val updateJson: MessageEntity = Marshal(updateOpinionStatusModel).to[MessageEntity].futureValue

    it("should update Opinion for admin") {
      val opinionController = prepareContext
      val request = Put(s"/$v1/$OPINIONS/${Opinions.opinion1.id.get}/status")
        .withEntity(updateJson)
        .withHeaders(authHeader(adminToken))

      request ~> opinionController.opinionRoutes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        val response: SuccessResponse[Boolean] = entityAs[SuccessResponse[Boolean]]
        response.data shouldBe true
      }
    }

    it("should reject request for non admin") {
      val opinionController = prepareContext
      val request = Put(s"/$v1/$OPINIONS/${Opinions.opinion1.id.get}/status")
        .withEntity(updateJson)
        .withHeaders(authHeader(user1Token))

      request ~> opinionController.opinionRoutes ~> check {
        rejection shouldEqual AuthorizationFailedRejection
      }
    }
  }


  describe("Getting list of all Opinions - GET /v1/opinions") {

    it("should return opinions list with proper paging for admin") {
      val opinionController = prepareContext
      val request = Get(s"/$v1/$OPINIONS?page=0&pageSize=3")
        .withHeaders(authHeader(adminToken))

      request ~> opinionController.opinionRoutes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        val response: SuccessResponse[PaginatedResult[OpinionView]] = entityAs[SuccessResponse[PaginatedResult[OpinionView]]]
        val MOCK_DATE: DateTime = DateTime.now
        val opinionViews = response.data.items.map { opinionView =>
          opinionView.copy(opinion = opinionView.opinion.copy(referenceDate = MOCK_DATE, creationDate = MOCK_DATE, lastModified = MOCK_DATE))
        }

        opinionViews shouldBe List(
          OpinionView(opinion = Opinions.opinion6ForPlace2.copy(referenceDate = MOCK_DATE, creationDate = MOCK_DATE, lastModified = MOCK_DATE), tags = List.empty, likes = OpinionLikes.empty),
          OpinionView(opinion = Opinions.opinion5ForPlace2.copy(referenceDate = MOCK_DATE, creationDate = MOCK_DATE, lastModified = MOCK_DATE), tags = List.empty, likes = OpinionLikes.empty),
          OpinionView(opinion = Opinions.opinion4.copy(referenceDate = MOCK_DATE, creationDate = MOCK_DATE, lastModified = MOCK_DATE), tags = List.empty, likes = OpinionLikes.empty)
        )
        response.data.hasNextPage shouldBe true
        response.data.totalCount shouldBe 6
      }
    }

    it("should reject request for non admin") {
      val opinionController = prepareContext
      val request = Get(s"/$v1/$OPINIONS")
        .withHeaders(authHeader(user1Token))

      request ~> opinionController.opinionRoutes ~> check {
        rejection shouldEqual AuthorizationFailedRejection
      }
    }
  }

  describe("Getting single Opinion - GET /v1/opinions/<id>") {
    it("should return opinion for admin") {

    }

    it("should reject request for non admin") {
      val opinionController = prepareContext
      val request = Get(s"/$v1/$OPINIONS/${Opinions.opinion1.id.get}")
        .withHeaders(authHeader(user1Token))

      request ~> opinionController.opinionRoutes ~> check {
        rejection shouldEqual AuthorizationFailedRejection
      }
    }
  }

  describe("Getting Opinion's Reports - GET /v1/opinions/<id>/reports") {
    it("should return opinion's reports list for admin") {
      val opinionController = prepareContext
      val reportDAO = SlickReportDAOInterpreter(executor)
      val reportCreation = reportDAO.create(Reports.report1) >> reportDAO.create(Reports.report2)
      Await.result(db.run(reportCreation), Duration.Inf)

      val request = Get(s"/$v1/$OPINIONS/${Opinions.opinion1.id.get}/$REPORTS")
        .withHeaders(authHeader(adminToken))

      request ~> opinionController.opinionRoutes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        val response: SuccessResponse[List[ReportView]] = entityAs[SuccessResponse[List[ReportView]]]
        val MOCK_DATE: DateTime = DateTime.now
        response.data.map(_.copy(creationDate = MOCK_DATE)) shouldBe List(
          ReportView.fromReport(Users.user1.id.get, Users.user1.userName, Reports.report1.copy(creationDate = MOCK_DATE)),
          ReportView.fromReport(Users.user2.id.get, Users.user2.userName, Reports.report2.copy(creationDate = MOCK_DATE))
        )
      }
    }

    it("should reject request for non admin") {
      val opinionController = prepareContext
      val request = Get(s"/$v1/$OPINIONS/${Opinions.opinion1.id.get}/$REPORTS")
        .withHeaders(authHeader(user1Token))

      request ~> opinionController.opinionRoutes ~> check {
        rejection shouldEqual AuthorizationFailedRejection
      }
    }
  }
}
