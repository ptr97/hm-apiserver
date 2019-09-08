package com.pwos.api.infrastructure.dao.memory

import cats.Id
import com.pwos.api.domain.opinions.OpinionDAOAlgebra


class MemoryOpinionDAOInterpreter extends OpinionDAOAlgebra[Id] {

}

object MemoryOpinionDAOInterpreter {
  def apply(): MemoryOpinionDAOInterpreter =
    new MemoryOpinionDAOInterpreter()
}
