package com.pwos.api.domain.places

import cats.Monad
import cats.data.EitherT
import com.pwos.api.domain.HelloMountainsError._
import com.pwos.api.domain.users.UserInfo


class PlaceService[F[_] : Monad](placeDAO: PlaceDAOAlgebra[F], placeValidation: PlaceValidationAlgebra[F]) {

  def create(userInfo: UserInfo, place: Place): EitherT[F, PlaceValidationError, Place] = {
    for {
      _ <- EitherT(Monad[F].pure(placeValidation.validateAdminAccess(userInfo)))
      _ <- placeValidation.doesNotExist(place)
      newPlace <- EitherT.liftF(placeDAO.create(place))
    } yield newPlace
  }

  def get(id: Long): EitherT[F, PlaceNotFoundError.type, Place] = {
    EitherT.fromOptionF(placeDAO.get(id), PlaceNotFoundError)
  }

  def update(userInfo: UserInfo, placeId: Long, placeUpdateModel: PlaceUpdateModel): EitherT[F, PlaceValidationError, Place] = {
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
      _ <- EitherT(Monad[F].pure(placeValidation.validateAdminAccess(userInfo)))
      _ <- placeValidation.exists(placeId)
      placeToUpdate <- get(placeId)
      updatedPlace = updatePlaceData(placeToUpdate)
      placeUpdateResult <- EitherT.fromOptionF(placeDAO.update(updatedPlace), PlaceNotFoundError: PlaceValidationError)
    } yield placeUpdateResult
  }

  def delete(userInfo: UserInfo, placeId: Long): EitherT[F, PlaceValidationError, Boolean] = {
    for {
      _ <- EitherT(Monad[F].pure(placeValidation.validateAdminAccess(userInfo)))
      _ <- placeValidation.exists(placeId)
      deletedPlace <- EitherT.liftF(placeDAO.delete(placeId))
    } yield deletedPlace
  }

  def list(): F[List[Place]] = {
    placeDAO.all
  }
}


object PlaceService {
  def apply[F[_] : Monad](placeDAO: PlaceDAOAlgebra[F], placeValidation: PlaceValidationAlgebra[F]): PlaceService[F] =
    new PlaceService(placeDAO, placeValidation)
}
