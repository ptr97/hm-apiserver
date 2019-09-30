package com.pwos.api.domain.opinions

import cats.Monad
import cats.data.EitherT
import com.pwos.api.domain.HelloMountainsError


class OpinionValidationInterpreter[F[_] : Monad](opinionDAO: OpinionDAOAlgebra[F]) extends OpinionValidationAlgebra[F] {
  override def exists(opinionUUID: String): EitherT[F, HelloMountainsError.OpinionNotFoundError.type, Unit] = ???

  override def validateOwnership(userId: Long, opinionUUID: String): EitherT[F, HelloMountainsError.OpinionOwnershipError.type, Unit] = ???
}


object OpinionValidationInterpreter {
  def apply[F[_] : Monad](opinionDAO: OpinionDAOAlgebra[F]): OpinionValidationInterpreter[F] =
    new OpinionValidationInterpreter(opinionDAO)
}
