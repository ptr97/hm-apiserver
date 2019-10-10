package com.pwos.api.infastructure

import slick.jdbc.MySQLProfile.api._
import slick.jdbc.MySQLProfile.backend.Database

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.Source


object DatabaseManager {

  def createDatabase(db: Database): Unit = {
    runScript(db, "db/01_DatabaseSetup.sql")
  }

  def cleanDatabase(db: Database): Unit = {
    runScript(db, "db/02_DatabaseCleanup.sql")
  }

  private def runScript(db: Database, path: String): Unit = {
    val script: String = Source.fromResource(path).getLines().mkString("\n")
    val listOfCommands: List[String] = script.split(";").toList

    listOfCommands.foreach { cmd =>
      val sqlCmd = sqlu"#$cmd"
      Await.result(db.run(sqlCmd.transactionally), Duration.Inf)
    }
  }

}
