package com.pwos.api.domain.places

import cats.data.EitherT
import cats.Functor
import cats.Monad
import com.pwos.api.domain.PlaceAlreadyExistsError
import com.pwos.api.domain.PlaceNotFoundError


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

  def delete(id: Long)(implicit M: Monad[F]): EitherT[F, PlaceNotFoundError.type, Boolean] = for {
    _ <- placeValidation.exists(Some(id))
    deletedPlace <- EitherT.liftF(placeDAO.delete(id))
  } yield deletedPlace

  def list(pageSize: Option[Int], offset: Option[Int]): F[List[Place]] =
    placeDAO.list(pageSize, offset)
}


object PlaceService {
  def apply[F[_]](placeDAO: PlaceDAOAlgebra[F], placeValidation: PlaceValidationAlgebra[F]): PlaceService[F] =
    new PlaceService(placeDAO, placeValidation)
}
