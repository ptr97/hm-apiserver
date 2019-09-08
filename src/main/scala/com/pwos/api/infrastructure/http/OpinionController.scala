package com.pwos.api.infrastructure.http

import com.pwos.api.domain.opinions.OpinionService
import com.pwos.api.infrastructure.http.authentication.SecuredAccess
import slick.dbio.DBIO
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext
import scala.language.postfixOps



class OpinionController(opinionService: OpinionService[DBIO])(implicit ec: ExecutionContext, database: Database) extends SecuredAccess {

}

object OpinionController {
  val OPINIONS = "opinions"

  def apply(opinionService: OpinionService[DBIO])(implicit ec: ExecutionContext, database: Database): OpinionController =
    new OpinionController(opinionService)
}
