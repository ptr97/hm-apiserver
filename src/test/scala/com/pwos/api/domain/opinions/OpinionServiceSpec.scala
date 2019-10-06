package com.pwos.api.domain.opinions

import cats.Id
import cats.implicits._
import com.pwos.api.PaginatedResult
import com.pwos.api.domain.HelloMountainsError._
import com.pwos.api.domain.PagingRequest
import com.pwos.api.domain.QueryParameters
import com.pwos.api.domain.opinions.OpinionModels._
import com.pwos.api.domain.opinions.reports.Report
import com.pwos.api.domain.opinions.reports.ReportCategory
import com.pwos.api.domain.opinions.reports.ReportView
import com.pwos.api.domain.opinions.tags.Tag
import com.pwos.api.domain.opinions.tags.TagCategory
import com.pwos.api.domain.places.Place
import com.pwos.api.domain.places.PlaceValidationInterpreter
import com.pwos.api.domain.users.User
import com.pwos.api.domain.users.UserInfo
import com.pwos.api.domain.users.UserRole
import com.pwos.api.infrastructure.dao.memory.MemoryOpinionDAOInterpreter
import com.pwos.api.infrastructure.dao.memory.MemoryPlaceDAOInterpreter
import com.pwos.api.infrastructure.dao.memory.MemoryReportDAOInterpreter
import com.pwos.api.infrastructure.dao.memory.MemoryUserDAOInterpreter
import org.scalatest.FunSpec
import org.scalatest.Matchers


class OpinionServiceSpec extends FunSpec with Matchers {

  case class OpinionServiceSpecResources(
    opinionDAO: MemoryOpinionDAOInterpreter,
    reportDAO: MemoryReportDAOInterpreter,
    userDAO: MemoryUserDAOInterpreter,
    placeDAO: MemoryPlaceDAOInterpreter,
    opinionValidation: OpinionValidationInterpreter[Id],
    placeValidation: PlaceValidationInterpreter[Id],
    opinionService: OpinionService[Id]
  )

  object OpinionServiceSpecResources {
    def apply(): OpinionServiceSpecResources = {

      val memoryOpinionDAO = MemoryOpinionDAOInterpreter(listOfTags)
      val memoryReportDAO = MemoryReportDAOInterpreter()
      val memoryUserDAO = MemoryUserDAOInterpreter()
      val memoryPlaceDAO = MemoryPlaceDAOInterpreter()
      val opinionValidation: OpinionValidationInterpreter[Id] = OpinionValidationInterpreter(memoryOpinionDAO)
      val placeValidation: PlaceValidationInterpreter[Id] = PlaceValidationInterpreter(memoryPlaceDAO)
      val opinionService: OpinionService[Id] = OpinionService(memoryOpinionDAO, memoryReportDAO, memoryUserDAO, opinionValidation, placeValidation)

      new OpinionServiceSpecResources(
        memoryOpinionDAO,
        memoryReportDAO,
        memoryUserDAO,
        memoryPlaceDAO,
        opinionValidation,
        placeValidation,
        opinionService
      )
    }
  }


  private val admin: User = User("steveKerr", "steve@gsw.com", "Secret123", UserRole.Admin, id = 1L.some)
  private val userStephen: User = User("stephCurry", "steph@gsw.com", "SecretPass123", UserRole.User, id = 2L.some)
  private val userKlay: User = User("klayThompson", "klay@gsw.com", "SecretPass123", UserRole.User, id = 3L.some)
  private val userKevin: User = User("kevinDurant", "kevin@gsw.com", "SecretPass123", UserRole.User, id = 4L.some)

  private val adminUserInfo = UserInfo.forUser(admin)
  private val userStephenUserInfo = UserInfo.forUser(userStephen)
  private val userKlayUserInfo = UserInfo.forUser(userKlay)
  private val userKevinUserInfo = UserInfo.forUser(userKevin)


  private val place: Place = Place("AGH", 50.067106, 19.913587, 203, id = 1L.some)
  private val secondPlace: Place = Place("Zakynthos", 37.7825062, 20.8950319, 13, id = 2L.some)
  private val thirdPlace: Place = Place("Meladives", -0.617644, 73.093730, 7, id = 3L.some)
  private val fourthPlace: Place = Place("Los Angeles", 34.0536909, -118.2427666, 88, id = 4L.some)


  private val tagOne: Tag = Tag("Tag 1", TagCategory.SUBSOIL, id = 1L.some)
  private val tagTwo: Tag = Tag("Tag 2", TagCategory.THREATS, id = 2L.some)
  private val tagThree: Tag = Tag("Tag 3", TagCategory.EQUIPMENT, id = 3L.some)
  private val tagFour: Tag = Tag("Tag 4", TagCategory.SUBSOIL, id = 4L.some)

  private val listOfTags: List[Tag] = List(tagOne, tagTwo, tagThree, tagFour)


  private val opinionOne: Opinion = Opinion(place.id.get, userStephen.id.get, "Opinion 1 Body".some)
  private val opinionTwo: Opinion = Opinion(place.id.get, userKlay.id.get, "Opinion 2 Body".some)
  private val opinionThree: Opinion = Opinion(place.id.get, userKevin.id.get, "Opinion 3 Body".some)
  private val opinionFour: Opinion = Opinion(fourthPlace.id.get, userKevin.id.get, "Opinion 3 Body".some)



  describe("Getting list of opinions for place") {

    def insertOpinionWithTagsToDb(resources: OpinionServiceSpecResources, opinion: Opinion, tags: List[Tag]): Id[Opinion] = {
      val opinionFromDb: Id[Opinion] = resources.opinionDAO.create(opinion)
      resources.opinionDAO.addTags(opinionFromDb.id.get, tags.flatMap(_.id))
      opinionFromDb
    }

    it("should return all opinion") {
      val resources = OpinionServiceSpecResources()

      val tagsForOpinionOne: List[Tag] = listOfTags.slice(0, 3)
      val opinionOneFromDb: Id[Opinion] = insertOpinionWithTagsToDb(resources, opinionOne, tagsForOpinionOne)

      val tagsForOpinionTwo: List[Tag] = listOfTags.slice(1, 3)
      val opinionTwoFromDb: Id[Opinion] = insertOpinionWithTagsToDb(resources, opinionTwo, tagsForOpinionTwo)

      val tagsForOpinionThree: List[Tag] = listOfTags.slice(2, 4)
      val opinionThreeFromDb: Id[Opinion] = insertOpinionWithTagsToDb(resources, opinionThree, tagsForOpinionThree)

      val getResult: Id[PaginatedResult[OpinionView]] = resources.opinionService.list(userStephenUserInfo, None, QueryParameters.empty, PagingRequest.empty)

      getResult.items shouldBe List(
        OpinionView(opinionThreeFromDb, tagsForOpinionThree.map(_.name), OpinionLikes.empty),
        OpinionView(opinionTwoFromDb, tagsForOpinionTwo.map(_.name), OpinionLikes.empty),
        OpinionView(opinionOneFromDb, tagsForOpinionOne.map(_.name), OpinionLikes.empty)
      )
      getResult.hasNextPage shouldBe false
      getResult.totalCount shouldBe 3
    }

    it("should return all opinion for place") {
      val resources = OpinionServiceSpecResources()

      val tagsForOpinionOne: List[Tag] = listOfTags.slice(0, 3)
      val opinionOneFromDb: Id[Opinion] = insertOpinionWithTagsToDb(resources, opinionOne, tagsForOpinionOne)

      val tagsForOpinionTwo: List[Tag] = listOfTags.slice(1, 3)
      val opinionTwoFromDb: Id[Opinion] = insertOpinionWithTagsToDb(resources, opinionTwo, tagsForOpinionTwo)

      val tagsForOpinionFour: List[Tag] = listOfTags.slice(2, 4)
      val _: Id[Opinion] = insertOpinionWithTagsToDb(resources, opinionFour, tagsForOpinionFour)

      val getResult: Id[PaginatedResult[OpinionView]] = resources.opinionService.list(userStephenUserInfo, place.id, QueryParameters.empty, PagingRequest.empty)

      getResult.items shouldBe List(
        OpinionView(opinionTwoFromDb, tagsForOpinionTwo.map(_.name), OpinionLikes.empty),
        OpinionView(opinionOneFromDb, tagsForOpinionOne.map(_.name), OpinionLikes.empty)
      )
      getResult.hasNextPage shouldBe false
      getResult.totalCount shouldBe 2
    }

    it("should return empty list when there is no opinions") {
      val resources = OpinionServiceSpecResources()

      val getResult: Id[PaginatedResult[OpinionView]] = resources.opinionService.list(adminUserInfo, None, QueryParameters.empty, PagingRequest.empty)

      getResult.items shouldBe List.empty
      getResult.hasNextPage shouldBe false
      getResult.totalCount shouldBe 0
    }
  }

  describe("Adding new Opinion") {

    val tagsForNewOpinion: List[Tag] = listOfTags.take(3)
    val createOpinionModel = CreateOpinionModel("New Opinion".some, tagsForNewOpinion.flatMap(_.id))

    it("should add new opinion") {
      val resources = OpinionServiceSpecResources()
      val placeFromDb: Id[Place] = resources.placeDAO.create(place)
      val createResult: Id[Either[PlaceNotFoundError.type, OpinionView]] = resources.opinionService.addOpinion(userStephenUserInfo, placeFromDb.id.get, createOpinionModel).value

      val fromDb: Id[Option[(Opinion, List[String], List[Long])]] = resources.opinionDAO.get(createResult.map(_.opinion).toOption.flatMap(_.id).get)

      createResult.map(_.opinion) shouldBe Right(fromDb.map(_._1).get)
      createResult.map(_.tags) shouldBe Right(fromDb.map(_._2).get)
      createResult.map(_.likes) shouldBe Right(OpinionLikes.empty)
    }

    it("should not add opinion when place which opinion reference to does not exist") {
      val resources = OpinionServiceSpecResources()
      val notExistingPlaceId: Long = resources.placeDAO.getLastId
      val result: Id[Either[PlaceNotFoundError.type, OpinionView]] = resources.opinionService.addOpinion(userStephenUserInfo, notExistingPlaceId, createOpinionModel).value

      result shouldBe Left(PlaceNotFoundError)
    }
  }

  describe("Getting single Opinion View") {
    it("should return opinion view") {
      val resources = OpinionServiceSpecResources()
      val tagsForOpinion: List[Tag] = listOfTags.take(3)
      val opinionOneFromDb: Id[Opinion] = resources.opinionDAO.create(opinionOne)
      val _: Id[Boolean] = resources.opinionDAO.addTags(opinionOneFromDb.id.get, tagsForOpinion.flatMap(_.id))

      val getResult: Id[Either[OpinionNotFoundError.type, OpinionView]] = resources.opinionService.getOpinionView(userStephenUserInfo, opinionOneFromDb.id.get).value

      getResult.map(_.opinion) shouldBe Right(opinionOneFromDb)
      getResult.map(_.likes) shouldBe Right(OpinionLikes.empty)
      getResult.map(_.tags) shouldBe Right(tagsForOpinion.map(_.name))
    }

    it("should return OpinionNotFoundError when Opinion does not exist") {
      val resources = OpinionServiceSpecResources()
      val notExistingOpinionId: Long = resources.opinionDAO.getLastOpinionId
      val result: Id[Either[OpinionNotFoundError.type, OpinionModels.OpinionView]] =
        resources.opinionService.getOpinionView(userStephenUserInfo, notExistingOpinionId).value

      result shouldBe Left(OpinionNotFoundError)
    }
  }

  describe("Deleting Opinion") {
    it("should mark opinion deleted when author tried to delete it") {
      val resources = OpinionServiceSpecResources()
      val opinionFromDb: Id[Opinion] = resources.opinionDAO.create(opinionOne)
      val opinionId: Long = opinionFromDb.id.get

      val deleteByOwnerResult: Id[Either[OpinionValidationError, Boolean]] = resources.opinionService.deleteOpinion(userStephenUserInfo, opinionId).value

      deleteByOwnerResult shouldBe Right(true)

      val deletedOpinion: Id[Option[(Opinion, List[String], List[Long])]] = resources.opinionDAO.get(opinionId)
      deletedOpinion shouldBe None
    }

    it("should mark opinion deleted when admin tried to delete it") {
      val resources = OpinionServiceSpecResources()
      val opinionFromDb: Id[Opinion] = resources.opinionDAO.create(opinionOne)
      val opinionId: Long = opinionFromDb.id.get

      val deleteByAdminResult: Id[Either[OpinionValidationError, Boolean]] = resources.opinionService.deleteOpinion(adminUserInfo, opinionId).value

      deleteByAdminResult shouldBe Right(true)

      val deletedOpinion: Id[Option[(Opinion, List[String], List[Long])]] = resources.opinionDAO.get(opinionId)
      deletedOpinion shouldBe None
    }

    it("should return OpinionNotFoundError when Opinion does not exist") {
      val resources = OpinionServiceSpecResources()
      val notExistingOpinionId: Long = resources.opinionDAO.getLastOpinionId
      val result = resources.opinionService.deleteOpinion(userStephenUserInfo, notExistingOpinionId).value

      result shouldBe Left(OpinionNotFoundError)
    }

    it("should return OpinionOwnershipError when user who is not an author tries to delete it") {
      val resources = OpinionServiceSpecResources()
      val opinionFromDb: Id[Opinion] = resources.opinionDAO.create(opinionOne)
      val opinionId: Long = opinionFromDb.id.get

      val deleteBySomeoneElseResult: Id[Either[OpinionValidationError, Boolean]] = resources.opinionService.deleteOpinion(userKevinUserInfo, opinionId).value

      deleteBySomeoneElseResult shouldBe Left(OpinionOwnershipError)

      val opinion: Id[Option[(Opinion, List[String], List[Long])]] = resources.opinionDAO.get(opinionId)
      opinion.map(_._1) shouldBe opinionFromDb.some
    }
  }

  describe("Updating Opinion") {
    val updateOpinionModel: UpdateOpinionModel = UpdateOpinionModel("Opinion New Body".some, None, None)

    it("should update opinion when author tried to update it") {
      val resources = OpinionServiceSpecResources()
      val opinionFromDb: Id[Opinion] = resources.opinionDAO.create(opinionOne)
      val opinionId: Long = opinionFromDb.id.get

      val updateByOwnerResult: Id[Either[OpinionValidationError, Boolean]] = resources.opinionService.updateOpinion(userStephenUserInfo, opinionId, updateOpinionModel).value

      updateByOwnerResult shouldBe Right(true)

      val updatedOpinion: Id[Option[(Opinion, List[String], List[Long])]] = resources.opinionDAO.get(opinionId)
      updatedOpinion.map(_._1) shouldBe opinionFromDb.copy(body = updateOpinionModel.body, lastModified = updatedOpinion.get._1.lastModified).some
    }

    it("should return OpinionNotFoundError when Opinion does not exist") {
      val resources = OpinionServiceSpecResources()
      val notExistingOpinionId: Long = resources.opinionDAO.getLastOpinionId
      val result: Id[Either[OpinionValidationError, Boolean]] = resources.opinionService.updateOpinion(userStephenUserInfo, notExistingOpinionId, updateOpinionModel).value

      result shouldBe Left(OpinionNotFoundError)
    }

    it("should return OpinionOwnershipError when user who is not an author tries to update it") {
      val resources = OpinionServiceSpecResources()
      val opinionFromDb: Id[Opinion] = resources.opinionDAO.create(opinionOne)
      val opinionId: Long = opinionFromDb.id.get

      val updateByOwnerResult: Id[Either[OpinionValidationError, Boolean]] = resources.opinionService.updateOpinion(userKlayUserInfo, opinionId, updateOpinionModel).value

      updateByOwnerResult shouldBe Left(OpinionOwnershipError)

      val sameOpinion: Id[Option[(Opinion, List[String], List[Long])]] = resources.opinionDAO.get(opinionId)
      sameOpinion.map(_._1) shouldBe opinionFromDb.some
    }
  }

  describe("Reporting Opinion") {
    val reportOpinionModel = ReportOpinionModel("Report Text".some, ReportCategory.MISLEADING)

    it("should report opinion") {
      val resources = OpinionServiceSpecResources()
      val opinionFromDb: Id[Opinion] = resources.opinionDAO.create(opinionOne)
      val opinionId: Long = opinionFromDb.id.get

      val result: Id[Either[OpinionNotFoundError.type, Boolean]] = resources.opinionService.reportOpinion(userKlayUserInfo, opinionId, reportOpinionModel).value

      result shouldBe Right(true)

      val reportsFromDb: Id[List[Report]] = resources.reportDAO.list(opinionId)
      reportsFromDb shouldBe List(Report(
        authorId = userKlayUserInfo.id,
        opinionId = opinionId,
        body = reportOpinionModel.body,
        reportCategory = reportOpinionModel.reportCategory,
        creationDate = resources.reportDAO.creationDateMock,
        id = (resources.reportDAO.lastReportId - 1).some
      ))
    }

    it("should return OpinionNotFoundError when Opinion does not exist") {
      val resources = OpinionServiceSpecResources()
      val notExistingOpinionId: Long = resources.opinionDAO.getLastOpinionId
      val result: Id[Either[OpinionNotFoundError.type, Boolean]] = resources.opinionService.reportOpinion(userStephenUserInfo, notExistingOpinionId, reportOpinionModel).value

      result shouldBe Left(OpinionNotFoundError)
    }

//    it("should not allow to report opinion more than once")
//    it("should block opinion when it has 3 reports")
  }

  describe("Updating Opinion status") {
    val updateStatusModel = UpdateOpinionStatusModel(blocked = true)

    it("should update opinion status") {
      val resources = OpinionServiceSpecResources()
      val opinionFromDb: Id[Opinion] = resources.opinionDAO.create(opinionOne)
      val opinionId: Long = opinionFromDb.id.get

      val result: Id[Either[OpinionNotFoundError.type, Boolean]] = resources.opinionService.updateOpinionStatus(adminUserInfo, opinionId, updateStatusModel).value

      result shouldBe Right(true)

      val blockedOpinion: Id[Option[(Opinion, List[String], List[Long])]] = resources.opinionDAO.get(opinionId)
      blockedOpinion shouldBe None
    }

    it("should return OpinionNotFoundError when Opinion does not exist") {
      val resources = OpinionServiceSpecResources()
      val notExistingOpinionId: Long = resources.opinionDAO.getLastOpinionId
      val result: Id[Either[OpinionNotFoundError.type, Boolean]] = resources.opinionService.updateOpinionStatus(userStephenUserInfo, notExistingOpinionId, updateStatusModel).value

      result shouldBe Left(OpinionNotFoundError)
    }
  }

  describe("Updating Opinion likes") {
    val like = UpdateOpinionLikesModel(like = true)
    val unlike = UpdateOpinionLikesModel(like = false)

    it("should add like to opinion") {
      val resources = OpinionServiceSpecResources()
      val opinionFromDb: Id[Opinion] = resources.opinionDAO.create(opinionOne)
      val opinionId: Long = opinionFromDb.id.get

      val result: Id[Either[OpinionValidationError, Boolean]] = resources.opinionService.updateOpinionLikes(userKlayUserInfo, opinionId, like).value

      result shouldBe Right(true)

      val getResultAsSteph: Id[Either[OpinionNotFoundError.type, OpinionView]] = resources.opinionService.getOpinionView(userStephenUserInfo, opinionId).value
      getResultAsSteph.map(_.opinion) shouldBe Right(opinionFromDb)
      getResultAsSteph.map(_.likes) shouldBe Right(OpinionLikes(1, likedByYou = false))

      val getResultAsKlay: Id[Either[OpinionNotFoundError.type, OpinionView]] = resources.opinionService.getOpinionView(userKlayUserInfo, opinionId).value
      getResultAsKlay.map(_.opinion) shouldBe Right(opinionFromDb)
      getResultAsKlay.map(_.likes) shouldBe Right(OpinionLikes(1, likedByYou = true))
    }

    it("should remove like from opinion") {
      val resources = OpinionServiceSpecResources()
      val opinionFromDb: Id[Opinion] = resources.opinionDAO.create(opinionOne)
      val opinionId: Long = opinionFromDb.id.get
      val _ = resources.opinionDAO.addLike(opinionId, userKlayUserInfo.id)

      val result: Id[Either[OpinionValidationError, Boolean]] = resources.opinionService.updateOpinionLikes(userKlayUserInfo, opinionId, unlike).value

      result shouldBe Right(true)

      val getResultAsSteph: Id[Either[OpinionNotFoundError.type, OpinionView]] = resources.opinionService.getOpinionView(userStephenUserInfo, opinionId).value
      getResultAsSteph.map(_.opinion) shouldBe Right(opinionFromDb)
      getResultAsSteph.map(_.likes) shouldBe Right(OpinionLikes(0, likedByYou = false))

      val getResultAsKlay: Id[Either[OpinionNotFoundError.type, OpinionView]] = resources.opinionService.getOpinionView(userKlayUserInfo, opinionId).value
      getResultAsKlay.map(_.opinion) shouldBe Right(opinionFromDb)
      getResultAsKlay.map(_.likes) shouldBe Right(OpinionLikes(0, likedByYou = false))
    }

    it("should return OpinionAlreadyLikedError when user already like opinion") {
      val resources = OpinionServiceSpecResources()
      val opinionFromDb: Id[Opinion] = resources.opinionDAO.create(opinionOne)
      val opinionId: Long = opinionFromDb.id.get
      val _ = resources.opinionDAO.addLike(opinionId, userKlayUserInfo.id)

      val result: Id[Either[OpinionValidationError, Boolean]] = resources.opinionService.updateOpinionLikes(userKlayUserInfo, opinionId, like).value

      result shouldBe Left(OpinionAlreadyLikedError)
    }

    it("should return OpinionWasNotLikedError when user has not liked opinion before") {
      val resources = OpinionServiceSpecResources()
      val opinionFromDb: Id[Opinion] = resources.opinionDAO.create(opinionOne)
      val opinionId: Long = opinionFromDb.id.get

      val result: Id[Either[OpinionValidationError, Boolean]] = resources.opinionService.updateOpinionLikes(userKlayUserInfo, opinionId, unlike).value

      result shouldBe Left(OpinionWasNotLikedError)
    }

    it("should return OpinionNotFoundError when Opinion does not exist") {
      val resources = OpinionServiceSpecResources()
      val notExistingOpinionId: Long = resources.opinionDAO.getLastOpinionId
      val result: Id[Either[OpinionValidationError, Boolean]] = resources.opinionService.updateOpinionLikes(userStephenUserInfo, notExistingOpinionId, like).value

      result shouldBe Left(OpinionNotFoundError)
    }
  }

  describe("Getting Opinion reports") {
    it("should return opinion reports") {
      val resources = OpinionServiceSpecResources()
      val opinionFromDb: Id[Opinion] = resources.opinionDAO.create(opinionOne)
      val opinionId: Long = opinionFromDb.id.get

      val reportOne = Report(userKlayUserInfo.id, opinionId, "Report Text 1".some, ReportCategory.MISLEADING)
      val reportTwo = Report(userKevinUserInfo.id, opinionId, "Report Text 2".some, ReportCategory.VULGAR)
      val reportThree = Report(userStephenUserInfo.id, opinionId, "Report Text 3".some, ReportCategory.FAULTY)

      val reportOneFromDb: Id[Report] = resources.reportDAO.create(reportOne)
      val reportTwoFromDb: Id[Report] = resources.reportDAO.create(reportTwo)
      val reportThreeFromDb: Id[Report] = resources.reportDAO.create(reportThree)

      val result: Id[List[Report]] = resources.reportDAO.list(opinionId)

      result shouldBe List(reportThreeFromDb, reportTwoFromDb, reportOneFromDb)
    }

    it("should return empty reports list when opinion has no reports") {
      val resources = OpinionServiceSpecResources()
      val opinionFromDb: Id[Opinion] = resources.opinionDAO.create(opinionOne)
      val opinionId: Long = opinionFromDb.id.get

      val result: Id[List[Report]] = resources.reportDAO.list(opinionId)

      result shouldBe List.empty
    }

    it("should return OpinionNotFoundError when Opinion does not exist") {
      val resources = OpinionServiceSpecResources()
      val notExistingOpinionId: Long = resources.opinionDAO.getLastOpinionId
      val result: Id[Either[OpinionNotFoundError.type, List[ReportView]]] = resources.opinionService.reports(userStephenUserInfo, notExistingOpinionId).value

      result shouldBe Left(OpinionNotFoundError)
    }
  }

}
