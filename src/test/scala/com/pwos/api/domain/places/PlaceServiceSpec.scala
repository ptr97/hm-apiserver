package com.pwos.api.domain.places

import cats.Id
import com.pwos.api.domain.HelloMountainsError._
import com.pwos.api.domain.users.User
import com.pwos.api.domain.users.UserInfo
import com.pwos.api.domain.users.UserRole
import com.pwos.api.infrastructure.dao.memory.MemoryPlaceDAOInterpreter
import com.pwos.api.infrastructure.dao.memory.MemoryUserDAOInterpreter
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers


class PlaceServiceSpec extends AnyFunSpec with Matchers {

  private def getTestResources: (MemoryPlaceDAOInterpreter, PlaceService[Id]) = {
    val memoryUserDAO = MemoryUserDAOInterpreter()

    val adminFromDb: Id[User] = memoryUserDAO.create(admin)
    adminUserInfo = UserInfo.forUser(adminFromDb)

    val userFromDb: Id[User] = memoryUserDAO.create(userStephen)
    userStephInfo = UserInfo.forUser(userFromDb)

    val memoryPlaceDAO = MemoryPlaceDAOInterpreter()
    val placeValidation: PlaceValidationInterpreter[Id] = PlaceValidationInterpreter(memoryPlaceDAO)
    val placeService: PlaceService[Id] = PlaceService(memoryPlaceDAO, placeValidation)
    (memoryPlaceDAO, placeService)
  }

  private val admin: User = User("admin", "admin@hm.com", "HashedPassword123", UserRole.Admin)
  private var adminUserInfo: UserInfo = _

  private val userStephen: User = User("stephCurry", "steph@gsw.com", "HashedPassword123", UserRole.User)
  private var userStephInfo: UserInfo = _


  private val place: Place = Place("AGH", 50.067106, 19.913587, 203)
  private val secondPlace: Place = Place("Zakynthos", 37.7825062, 20.8950319, 13)
  private val thirdPlace: Place = Place("Meladives", -0.617644, 73.093730, 7)
  private val fourthPlace: Place = Place("Los Angeles", 34.0536909, -118.2427666, 88)


  describe("Adding new place") {
    it("should add new place") {
      val (placeDAO, placeService) = getTestResources
      val addPlaceResult = placeService.create(adminUserInfo, place).value
      val placeFromDb: Id[Place] = placeDAO.findByName(place.name).get

      addPlaceResult shouldBe Right(placeFromDb)
      addPlaceResult.map(_.id) shouldBe Right(placeFromDb.id)
      addPlaceResult.map(_.name) shouldBe Right(place.name)
      addPlaceResult.map(_.latitude) shouldBe Right(place.latitude)
      addPlaceResult.map(_.longitude) shouldBe Right(place.longitude)
      addPlaceResult.map(_.elevation) shouldBe Right(place.elevation)
    }

    it("should return PlaceAlreadyExistsError when exactly the same place exists") {
      val (placeDAO, placeService) = getTestResources
      placeDAO.create(place)
      val addPlaceResult = placeService.create(adminUserInfo, place).value

      addPlaceResult shouldBe Left(PlaceAlreadyExistsError(place))
    }

    it("should return PlaceAlreadyExistsError when place with same name exists") {
      val (placeDAO, placeService) = getTestResources
      placeDAO.create(place)
      val placeWithSameName: Place = Place(place.name, 10.10, 10.10, 100)
      val addPlaceResult = placeService.create(adminUserInfo, placeWithSameName).value

      addPlaceResult shouldBe Left(PlaceAlreadyExistsError(placeWithSameName))
    }

    it ("should return PlacePrivilegeError when non admin users tries to add place") {
      val (_, placeService) = getTestResources
      val addPlaceResult = placeService.create(userStephInfo, place).value

      addPlaceResult shouldBe Left(PlacePrivilegeError)
    }
  }

  describe("Getting single place by id") {
    it("should return place when place exists") {
      val (placeDAO, placeService) = getTestResources
      val placeFromDb: Id[Place] = placeDAO.create(place)
      val getResult: Id[Either[PlaceNotFoundError.type, Place]] = placeService.get(placeFromDb.id.get).value

      getResult shouldBe Right(placeFromDb)
      getResult.map(_.id) shouldBe Right(placeFromDb.id)
      getResult.map(_.name) shouldBe Right(place.name)
      getResult.map(_.latitude) shouldBe Right(place.latitude)
      getResult.map(_.longitude) shouldBe Right(place.longitude)
      getResult.map(_.elevation) shouldBe Right(place.elevation)
    }

    it("should return PlaceNotFoundError when place does not exist") {
      val (placeDAO, placeService) = getTestResources
      val notExistingId: Long = placeDAO.getLastId + 1

      val getResult: Id[Either[PlaceNotFoundError.type, Place]] = placeService.get(notExistingId).value
      getResult shouldBe Left(PlaceNotFoundError)
    }
  }

  describe("Updating place") {
    it("should update place all place details when specified") {
      val (placeDAO, placeService) = getTestResources
      val placeFromDb: Id[Place] = placeDAO.create(place)
      val placeUpdateModel = PlaceUpdateModel(name = Some(secondPlace.name),
        latitude = Some(secondPlace.latitude),
        longitude = Some(secondPlace.longitude),
        elevation = Some(secondPlace.elevation))

      val updateResult = placeService.update(adminUserInfo, placeFromDb.id.get, placeUpdateModel).value

      updateResult shouldBe Right(Place(
        placeUpdateModel.name.get,
        placeUpdateModel.latitude.get,
        placeUpdateModel.longitude.get,
        placeUpdateModel.elevation.get,
        placeFromDb.id
      ))
    }

    it("should update only place name when specified") {
      val (placeDAO, placeService) = getTestResources
      val placeFromDb: Id[Place] = placeDAO.create(place)
      val placeUpdateModel = PlaceUpdateModel(name = Some(secondPlace.name))

      val updateResult = placeService.update(adminUserInfo, placeFromDb.id.get, placeUpdateModel).value

      updateResult shouldBe Right(placeFromDb.copy(name = placeUpdateModel.name.get))
    }

    it("should update only place latitude when specified") {
      val (placeDAO, placeService) = getTestResources
      val placeFromDb: Id[Place] = placeDAO.create(place)
      val placeUpdateModel = PlaceUpdateModel(latitude = Some(secondPlace.latitude))

      val updateResult = placeService.update(adminUserInfo, placeFromDb.id.get, placeUpdateModel).value

      updateResult shouldBe Right(placeFromDb.copy(latitude = placeUpdateModel.latitude.get))
    }

    it("should not update place name when place does not exist") {
      val (placeDAO, placeService) = getTestResources
      val placeUpdateModel = PlaceUpdateModel(elevation = Some(secondPlace.elevation))
      val notExistingId: Long = placeDAO.getLastId + 1

      val updateResult = placeService.update(adminUserInfo, notExistingId, placeUpdateModel).value

      updateResult shouldBe Left(PlaceNotFoundError)
    }

    it ("should return PlacePrivilegeError when non admin users tries to update place") {
      val (placeDAO, placeService) = getTestResources
      val placeFromDb: Id[Place] = placeDAO.create(place)
      val placeUpdateModel = PlaceUpdateModel(latitude = Some(secondPlace.latitude))
      val result = placeService.update(userStephInfo, placeFromDb.id.get, placeUpdateModel).value

      result shouldBe Left(PlacePrivilegeError)
    }
  }

  describe("Deleting place") {
    it("should delete place when it exists") {
      val (placeDAO, placeService) = getTestResources
      val placeFromDb: Id[Place] = placeDAO.create(place)
      val deleteResult = placeService.delete(adminUserInfo, placeFromDb.id.get).value

      deleteResult shouldBe Right(true)
    }

    it("should return PlaceNotFoundError when place does not exist") {
      val (placeDAO, placeService) = getTestResources
      val notExistingId: Long = placeDAO.getLastId + 1

      val deleteResult = placeService.delete(adminUserInfo, notExistingId).value

      deleteResult shouldBe Left(PlaceNotFoundError)
    }

    it ("should return PlacePrivilegeError when non admin users tries to delete place") {
      val (placeDAO, placeService) = getTestResources
      val placeFromDb: Id[Place] = placeDAO.create(place)
      val deleteResult = placeService.delete(userStephInfo, placeFromDb.id.get).value

      deleteResult shouldBe Left(PlacePrivilegeError)
    }
  }

  describe("Getting list of places") {
    it("should return list of places when there are some places") {
      val (placeDAO, placeService) = getTestResources
      val p1: Id[Place] = placeDAO.create(place)
      val p2: Id[Place] = placeDAO.create(secondPlace)
      val p3: Id[Place] = placeDAO.create(thirdPlace)
      val p4: Id[Place] = placeDAO.create(fourthPlace)

      val getAllPlacesResult: Id[List[Place]] = placeService.list()

      getAllPlacesResult shouldBe List(p4, p3, p2, p1)
    }

    it("should return empty list of places when there are not any places") {
      val (_, placeService) = getTestResources
      val getAllPlacesResult: Id[List[Place]] = placeService.list()

      getAllPlacesResult shouldBe List.empty
    }
  }
}
