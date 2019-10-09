package com.pwos.api.domain.opinions.tags

import cats.Monad
import cats.implicits._
import cats.data.EitherT
import com.pwos.api.domain.HelloMountainsError._
import com.pwos.api.domain.authentication.AuthValidation
import com.pwos.api.domain.users.UserInfo


class TagValidationInterpreter[F[_] : Monad](tagDAO: TagDAOAlgebra[F]) extends TagValidationAlgebra[F] with AuthValidation {

  override def doesNotExists(tag: Tag): EitherT[F, TagAlreadyExistsError, Unit] = EitherT {
    tagDAO.findByName(tag.name).map {
      case Some(_) => Left(TagAlreadyExistsError(tag))
      case None => Right(())
    }
  }

  override def exists(placeId: Long): EitherT[F, TagNotFoundError.type, Unit] = {
    EitherT.fromOptionF(tagDAO.get(placeId), TagNotFoundError).map(_ => ())
  }

  override def validateAdminAccess(userInfo: UserInfo): Either[TagPrivilegeError.type, Unit] = {
    super.validateAdminAccess[TagPrivilegeError.type](userInfo)(TagPrivilegeError)
  }

}

object TagValidationInterpreter {
  def apply[F[_] : Monad](tagDAO: TagDAOAlgebra[F]): TagValidationInterpreter[F] =
    new TagValidationInterpreter[F](tagDAO)
}
