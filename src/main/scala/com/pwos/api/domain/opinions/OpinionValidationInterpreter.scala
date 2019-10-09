package com.pwos.api.domain.opinions

import cats.Monad
import cats.implicits._
import cats.data.EitherT
import cats.data.OptionT
import com.pwos.api.domain.HelloMountainsError._
import com.pwos.api.domain.authentication.AuthValidation
import com.pwos.api.domain.users.UserInfo


class OpinionValidationInterpreter[F[_] : Monad](opinionDAO: OpinionDAOAlgebra[F]) extends OpinionValidationAlgebra[F] with AuthValidation {

  override def exists(opinionId: Long): EitherT[F, OpinionNotFoundError.type, Unit] = {
    EitherT.fromOptionF(opinionDAO.getActiveOpinion(opinionId), OpinionNotFoundError).map(_ => ())
  }

  override def validateOwnership(userId: Long, opinionId: Long): EitherT[F, OpinionOwnershipError.type, Unit] = {
    val maybeOpinionWithValidOwner: OptionT[F, Unit] = OptionT(opinionDAO.getActiveOpinion(opinionId)) filter { maybeOpinion =>
      maybeOpinion.authorId === userId
    } map(_ => ())

    EitherT.fromOptionF(maybeOpinionWithValidOwner.value, OpinionOwnershipError)
  }

  override def validateAdminAccess(userInfo: UserInfo): Either[OpinionPrivilegeError.type, Unit] = {
    super.validateAdminAccess[OpinionPrivilegeError.type](userInfo)(OpinionPrivilegeError)
  }

}


object OpinionValidationInterpreter {
  def apply[F[_] : Monad](opinionDAO: OpinionDAOAlgebra[F]): OpinionValidationInterpreter[F] =
    new OpinionValidationInterpreter(opinionDAO)
}
