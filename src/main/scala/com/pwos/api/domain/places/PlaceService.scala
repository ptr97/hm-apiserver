package com.pwos.api.domain.places

import cats.Monad
import cats.data.EitherT
import com.pwos.api.domain.HelloMountainsError._


class PlaceService[F[_] : Monad](placeDAO: PlaceDAOAlgebra[F], placeValidation: PlaceValidationAlgebra[F]) {

  def create(place: Place): EitherT[F, PlaceAlreadyExistsError, Place] = for {
    _ <- placeValidation.doesNotExists(place)
    newPlace <- EitherT.liftF(placeDAO.create(place))
  } yield newPlace

  def get(id: Long): EitherT[F, PlaceNotFoundError.type, Place] =
    EitherT.fromOptionF(placeDAO.get(id), PlaceNotFoundError)

  def update(id: Long, placeUpdateModel: PlaceUpdateModel): EitherT[F, PlaceNotFoundError.type, Place] = {
    type PlaceUpdate = Place => Option[Place]

    val updateName: PlaceUpdate = place => placeUpdateModel.name.map(name => place.copy(name = name))
    val updateLatitude: PlaceUpdate = place => placeUpdateModel.latitude.map(latitude => place.copy(latitude = latitude))
    val updateLongitude: PlaceUpdate = place => placeUpdateModel.longitude.map(longitude => place.copy(longitude = longitude))
    val updateElevation: PlaceUpdate = place => placeUpdateModel.elevation.map(elevation => place.copy(elevation = elevation))

    val updates: List[PlaceUpdate] = List(updateName, updateLatitude, updateLongitude, updateElevation)

    val updatePlaceData: Place => Place = oldPlace => {
      updates.foldLeft(oldPlace)((place, updateFun) => updateFun(place).getOrElse(place))
    }

    for {
      _ <- placeValidation.exists(id)
      placeToUpdate <- get(id)
      updatedPlace = updatePlaceData(placeToUpdate)
      placeUpdateResult <- EitherT.fromOptionF(placeDAO.update(updatedPlace), PlaceNotFoundError)
    } yield placeUpdateResult
  }

  def delete(id: Long): EitherT[F, PlaceNotFoundError.type, Boolean] = for {
    _ <- placeValidation.exists(id)
    deletedPlace <- EitherT.liftF(placeDAO.delete(id))
  } yield deletedPlace

  def list(): F[List[Place]] = {
    placeDAO.all
  }
}


object PlaceService {
  def apply[F[_] : Monad](placeDAO: PlaceDAOAlgebra[F], placeValidation: PlaceValidationAlgebra[F]): PlaceService[F] =
    new PlaceService(placeDAO, placeValidation)
}
