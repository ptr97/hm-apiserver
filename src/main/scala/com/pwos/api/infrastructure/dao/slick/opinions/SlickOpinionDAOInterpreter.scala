package com.pwos.api.infrastructure.dao.slick.opinions

import com.pwos.api.domain.opinions.OpinionDAOAlgebra
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext


class SlickOpinionDAOInterpreter(implicit ec: ExecutionContext) extends OpinionDAOAlgebra[DBIO] {

}

object SlickOpinionDAOInterpreter {
  def apply(implicit ec: ExecutionContext): SlickOpinionDAOInterpreter =
    new SlickOpinionDAOInterpreter()
}
