package com.pwos.api.domain.places

import cats.{Functor, Monad}
import cats.data.EitherT
import com.pwos.api.domain.{PlaceAlreadyExistsError, PlaceNotFoundError}


class PlaceService[F[_]](placeDAO: PlaceDAOAlgebra[F], placeValidation: PlaceValidationAlgebra[F]) {

  def create(place: Place)(implicit M: Monad[F]): EitherT[F, PlaceAlreadyExistsError, Place] = for {
    _ <- placeValidation.doesNotExists(place)
    newPlace <- EitherT.liftF(placeDAO.create(place))
  } yield newPlace

  def get(id: Long)(implicit F: Functor[F]): EitherT[F, PlaceNotFoundError.type, Place] =
    EitherT.fromOptionF(placeDAO.get(id), PlaceNotFoundError)

  def update(place: Place)(implicit M: Monad[F]): EitherT[F, PlaceNotFoundError.type, Place] = for {
    _ <- placeValidation.exists(place.id)
    updatedPlace <- EitherT.fromOptionF(placeDAO.update(place), PlaceNotFoundError)
  } yield updatedPlace

  def delete(id: Long)(implicit M: Monad[F]): EitherT[F, PlaceNotFoundError.type, Place] = for {
    _ <- placeValidation.exists(Some(id))
    deletedPlace <- EitherT.fromOptionF(placeDAO.delete(id), PlaceNotFoundError)
  } yield deletedPlace

  def list(pageSize: Int, offset: Int): F[List[Place]] =
    placeDAO.list(pageSize, offset)
}


object PlaceService {
  def apply[F[_]](placeDAO: PlaceDAOAlgebra[F], placeValidation: PlaceValidationAlgebra[F]): PlaceService[F] =
    new PlaceService(placeDAO, placeValidation)
}
