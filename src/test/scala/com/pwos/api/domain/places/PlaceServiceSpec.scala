package com.pwos.api.domain.places

import cats.Id
import com.pwos.api.domain.HelloMountainsError._
import com.pwos.api.infrastructure.dao.memory.MemoryPlaceDAOInterpreter
import org.scalatest.FunSpec
import org.scalatest.Matchers


class PlaceServiceSpec extends FunSpec with Matchers {

  private def getTestResources: (MemoryPlaceDAOInterpreter, PlaceService[Id]) = {
    val memoryPlaceDAO = MemoryPlaceDAOInterpreter()
    val placeValidation: PlaceValidationInterpreter[Id] = PlaceValidationInterpreter(memoryPlaceDAO)
    val placeService: PlaceService[Id] = PlaceService(memoryPlaceDAO, placeValidation)
    (memoryPlaceDAO, placeService)
  }

  private val place: Place = Place("AGH", 50.067106, 19.913587, 203)
  private val secondPlace: Place = Place("Zakynthos", 37.7825062, 20.8950319, 13)
  private val thirdPlace: Place = Place("Meladives", -0.617644, 73.093730, 7)
  private val fourthPlace: Place = Place("Los Angeles", 34.0536909, -118.2427666, 88)


    describe("Adding new place") {
    it("should add new place") {
      val (placeDAO, placeService) = getTestResources
      val addPlaceResult: Id[Either[PlaceAlreadyExistsError, Place]] = placeService.create(place).value
      val placeFromDb: Id[Place] = placeDAO.findByName(place.name).get

      addPlaceResult shouldBe Right(placeFromDb)
      addPlaceResult.map(_.id) shouldBe Right(placeFromDb.id)
      addPlaceResult.map(_.name) shouldBe Right(place.name)
      addPlaceResult.map(_.latitude) shouldBe Right(place.latitude)
      addPlaceResult.map(_.longitude) shouldBe Right(place.longitude)
      addPlaceResult.map(_.elevation) shouldBe Right(place.elevation)
    }

    it("should not add new place when exactly the same place exists") {
      val (placeDAO, placeService) = getTestResources
      placeDAO.create(place)
      val addPlaceResult: Id[Either[PlaceAlreadyExistsError, Place]] = placeService.create(place).value

      addPlaceResult shouldBe Left(PlaceAlreadyExistsError(place))
    }

    it("should not add new place when place with same name exists") {
      val (placeDAO, placeService) = getTestResources
      placeDAO.create(place)
      val placeWithSameName: Place = Place(place.name, 10.10, 10.10, 100)
      val addPlaceResult = placeService.create(placeWithSameName).value

      addPlaceResult shouldBe Left(PlaceAlreadyExistsError(placeWithSameName))
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

      val updateResult: Id[Either[PlaceNotFoundError.type, Place]] = placeService.update(placeFromDb.id.get, placeUpdateModel).value

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

      val updateResult: Id[Either[PlaceNotFoundError.type, Place]] = placeService.update(placeFromDb.id.get, placeUpdateModel).value

      updateResult shouldBe Right(placeFromDb.copy(name = placeUpdateModel.name.get))
    }

    it("should update only place latitude when specified") {
      val (placeDAO, placeService) = getTestResources
      val placeFromDb: Id[Place] = placeDAO.create(place)
      val placeUpdateModel = PlaceUpdateModel(latitude = Some(secondPlace.latitude))

      val updateResult: Id[Either[PlaceNotFoundError.type, Place]] = placeService.update(placeFromDb.id.get, placeUpdateModel).value

      updateResult shouldBe Right(placeFromDb.copy(latitude = placeUpdateModel.latitude.get))
    }

    it("should not update place name when place does not exist") {
      val (placeDAO, placeService) = getTestResources
      val placeUpdateModel = PlaceUpdateModel(elevation = Some(secondPlace.elevation))
      val notExistingId: Long = placeDAO.getLastId + 1

      val updateResult: Id[Either[PlaceNotFoundError.type, Place]] = placeService.update(notExistingId, placeUpdateModel).value

      updateResult shouldBe Left(PlaceNotFoundError)
    }
  }

  describe("Deleting place") {
    it("should delete place when it exists") {
      val (placeDAO, placeService) = getTestResources
      val placeFromDb: Id[Place] = placeDAO.create(place)
      val deleteResult: Id[Either[PlaceNotFoundError.type, Boolean]] = placeService.delete(placeFromDb.id.get).value

      deleteResult shouldBe Right(true)
    }

    it("should not delete place when it does not exist") {
      val (placeDAO, placeService) = getTestResources
      val notExistingId: Long = placeDAO.getLastId + 1

      val deleteResult: Id[Either[PlaceNotFoundError.type, Boolean]] = placeService.delete(notExistingId).value

      deleteResult shouldBe Left(PlaceNotFoundError)
    }
  }

  describe("Getting list of places") {
    it("should return list of places when there are some places") {
      val (placeDAO, placeService) = getTestResources
      val p1: Id[Place] = placeDAO.create(place)
      val p2: Id[Place] = placeDAO.create(secondPlace)
      val p3: Id[Place] = placeDAO.create(thirdPlace)
      val p4: Id[Place] = placeDAO.create(fourthPlace)

      val getAllPlacesResult: Id[List[Place]] = placeService.list(None, None)

      getAllPlacesResult.sortBy(_.name) shouldBe List(p1, p2, p3, p4).sortBy(_.name)
    }

    it("should return empty list of places when there are not any places") {
      val (_, placeService) = getTestResources
      val getAllPlacesResult: Id[List[Place]] = placeService.list(None, None)

      getAllPlacesResult shouldBe List.empty
    }

    it("should return list of places with proper page size") {
      val (placeDAO, placeService) = getTestResources
      val p1: Id[Place] = placeDAO.create(place)
      val p2: Id[Place] = placeDAO.create(secondPlace)
      val p3: Id[Place] = placeDAO.create(thirdPlace)
      val p4: Id[Place] = placeDAO.create(fourthPlace)

      val getAllPlacesResult: Id[List[Place]] = placeService.list(Some(2), None)

      getAllPlacesResult.sortBy(_.name) shouldBe List(p1, p2).sortBy(_.name)
    }

    it("should return list of places with proper offset") {
      val (placeDAO, placeService) = getTestResources
      val p1: Id[Place] = placeDAO.create(place)
      val p2: Id[Place] = placeDAO.create(secondPlace)
      val p3: Id[Place] = placeDAO.create(thirdPlace)
      val p4: Id[Place] = placeDAO.create(fourthPlace)

      val getAllPlacesResult: Id[List[Place]] = placeService.list(None, Some(2))

      getAllPlacesResult.sortBy(_.name) shouldBe List(p3, p4).sortBy(_.name)
    }

    it("should return list of places with proper page size and offset") {
      val (placeDAO, placeService) = getTestResources
      val p1: Id[Place] = placeDAO.create(place)
      val p2: Id[Place] = placeDAO.create(secondPlace)
      val p3: Id[Place] = placeDAO.create(thirdPlace)
      val p4: Id[Place] = placeDAO.create(fourthPlace)

      val getAllPlacesResult: Id[List[Place]] = placeService.list(Some(2), Some(1))

      getAllPlacesResult.sortBy(_.name) shouldBe List(p2, p3).sortBy(_.name)
    }
  }
}
