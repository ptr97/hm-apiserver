package com.pwos.api.domain.opinions

import cats.Monad
import cats.implicits._
import cats.data.EitherT
import cats.data.OptionT
import com.pwos.api.domain.HelloMountainsError._


class OpinionValidationInterpreter[F[_] : Monad](opinionDAO: OpinionDAOAlgebra[F]) extends OpinionValidationAlgebra[F] {

  override def exists(opinionId: Long): EitherT[F, OpinionNotFoundError.type, Unit] = {
    EitherT.fromOptionF(opinionDAO.get(opinionId), OpinionNotFoundError).map(_ => ())
  }

  override def validateOwnership(userId: Long, opinionId: Long): EitherT[F, OpinionOwnershipError.type, Unit] = {
    val maybeOpinionWithValidOwner: OptionT[F, Unit] = OptionT(opinionDAO.get(opinionId)) map(_._1) filter { maybeOpinion =>
      maybeOpinion.authorId === userId
    } map(_ => ())

    EitherT.fromOptionF(maybeOpinionWithValidOwner.value, OpinionOwnershipError)
  }

}


object OpinionValidationInterpreter {
  def apply[F[_] : Monad](opinionDAO: OpinionDAOAlgebra[F]): OpinionValidationInterpreter[F] =
    new OpinionValidationInterpreter(opinionDAO)
}
