package com.pwos.api.domain.opinions

import cats.Monad


class OpinionValidationInterpreter[F[_] : Monad](opinionDAO: OpinionDAOAlgebra[F]) extends OpinionDAOAlgebra[F] {

}


object OpinionValidationInterpreter {
  def apply[F[_] : Monad](opinionDAO: OpinionDAOAlgebra[F]): OpinionValidationInterpreter[F] =
    new OpinionValidationInterpreter(opinionDAO)
}
