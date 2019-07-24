package com.pwos.api.domain.places

import cats.Monad
import cats.data.EitherT
import cats.implicits._
import com.pwos.api.domain.PlaceAlreadyExistsError
import com.pwos.api.domain.PlaceNotFoundError


final class PlaceValidationInterpreter[F[_] : Monad](placeDAO: PlaceDAOAlgebra[F]) extends PlaceValidationAlgebra[F] {

  override def doesNotExists(place: Place): EitherT[F, PlaceAlreadyExistsError, Unit] = EitherT {
    placeDAO.findByName(place.name).map {
      case Some(_) => Left(PlaceAlreadyExistsError(place))
      case None => Right(())
    }
  }

  override def exists(placeId: Option[Long]): EitherT[F, PlaceNotFoundError.type, Unit] = EitherT {
    placeId.map { id =>
      placeDAO.get(id).map {
        case Some(_) => Right(())
        case None => Left(PlaceNotFoundError)
      }
    }.getOrElse {
      Either.left[PlaceNotFoundError.type, Unit](PlaceNotFoundError).pure[F]
    }
  }
}

object PlaceValidationInterpreter {
  def apply[F[_] : Monad](placeDAO: PlaceDAOAlgebra[F]): PlaceValidationInterpreter[F] =
    new PlaceValidationInterpreter[F](placeDAO)
}
