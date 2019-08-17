package com.pwos.api.domain.places

import cats.Functor
import cats.Monad
import cats.data.EitherT
import cats.data.IdT
import com.pwos.api.domain.HelloMountainsError._


class PlaceService[F[_]](placeDAO: PlaceDAOAlgebra[F], placeValidation: PlaceValidationAlgebra[F]) {

  def create(place: Place)(implicit M: Monad[F]): EitherT[F, PlaceAlreadyExistsError, Place] = for {
    _ <- placeValidation.doesNotExists(place)
    newPlace <- EitherT.liftF(placeDAO.create(place))
  } yield newPlace

  def get(id: Long)(implicit F: Functor[F]): EitherT[F, PlaceNotFoundError.type, Place] =
    EitherT.fromOptionF(placeDAO.get(id), PlaceNotFoundError)

  def update(id: Long, placeUpdateModel: PlaceUpdateModel)(implicit M: Monad[F]): EitherT[F, PlaceNotFoundError.type, Place] = {
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
      _ <- placeValidation.exists(Option(id))
      placeToUpdate <- get(id)
      updatedPlace = updatePlaceData(placeToUpdate)
      placeUpdateResult <- EitherT.fromOptionF(placeDAO.update(updatedPlace), PlaceNotFoundError)
    } yield placeUpdateResult
  }

  def delete(id: Long)(implicit M: Monad[F]): EitherT[F, PlaceNotFoundError.type, Boolean] = for {
    _ <- placeValidation.exists(Option(id))
    deletedPlace <- EitherT.liftF(placeDAO.delete(id))
  } yield deletedPlace

  def list(pageSize: Option[Int], offset: Option[Int])(implicit M: Monad[F]): F[List[Place]] = {
    val places: IdT[F, List[Place]] = for {
      allPlaces <- IdT(placeDAO.all).map(_.sortBy(_.id))
      withOffset = offset.map(off => allPlaces.drop(off)).getOrElse(allPlaces)
      result = pageSize.map(size => withOffset.take(size)).getOrElse(withOffset)
    } yield result

    places.value
  }
}


object PlaceService {
  def apply[F[_]](placeDAO: PlaceDAOAlgebra[F], placeValidation: PlaceValidationAlgebra[F]): PlaceService[F] =
    new PlaceService(placeDAO, placeValidation)
}
