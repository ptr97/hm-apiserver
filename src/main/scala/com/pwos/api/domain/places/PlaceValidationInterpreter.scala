package com.pwos.api.domain.places

import cats.Monad
import cats.data.EitherT
import cats.implicits._
import com.pwos.api.domain.HelloMountainsError._
import com.pwos.api.domain.authentication.AuthValidation
import com.pwos.api.domain.users.UserInfo


final class PlaceValidationInterpreter[F[_] : Monad](placeDAO: PlaceDAOAlgebra[F]) extends PlaceValidationAlgebra[F] with AuthValidation {

  override def doesNotExist(place: Place): EitherT[F, PlaceAlreadyExistsError, Unit] = EitherT {
    placeDAO.findByName(place.name).map {
      case Some(_) => Left(PlaceAlreadyExistsError(place))
      case None => Right(())
    }
  }

  override def exists(placeId: Long): EitherT[F, PlaceNotFoundError.type, Unit] =  {
    EitherT.fromOptionF(placeDAO.get(placeId), PlaceNotFoundError).map(_ => ())
  }

  override def validateAdminAccess(userInfo: UserInfo): Either[PlacePrivilegeError.type, Unit] = {
    super.validateAdminAccess[PlacePrivilegeError.type](userInfo)(PlacePrivilegeError)
  }
}

object PlaceValidationInterpreter {
  def apply[F[_] : Monad](placeDAO: PlaceDAOAlgebra[F]): PlaceValidationInterpreter[F] =
    new PlaceValidationInterpreter[F](placeDAO)
}
