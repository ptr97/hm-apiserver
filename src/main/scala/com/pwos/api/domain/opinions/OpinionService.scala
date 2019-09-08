package com.pwos.api.domain.opinions

import cats.Monad


class OpinionService[F[_] : Monad](opinionDAO: OpinionDAOAlgebra[F], opinionValidation: OpinionValidationAlgebra[F]) {

}

object OpinionService {
  def apply[F[_] : Monad](opinionDAO: OpinionDAOAlgebra[F], opinionValidation: OpinionValidationAlgebra[F]): OpinionService[F] =
    new OpinionService(opinionDAO, opinionValidation)
}
