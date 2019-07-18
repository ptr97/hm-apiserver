package com.pwos.api

import slick.dbio.DBIO
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}


package object infrastructure {

  object implicits {

    implicit class DBIOtoFuture[T](dbio: DBIO[T])(implicit ec: ExecutionContext, db: Database) {
      def unsafeRun: Future[T] = db.run(dbio)
    }

  }

}
