package com.pwos.api.config

import pureconfig.generic.auto._
import pureconfig.error.ConfigReaderFailures
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.backend.Database


case class Config(api: ApplicationConfig, database: Database)
case class ApplicationConfig(server: ServerConfig)
case class ServerConfig(host: String, port: Int)


object Config {
  def unsafeLoadConfig: Config = {
    Config(ApplicationConfig.unsafeLoadAppConfig, DatabaseConnection.connectToDatabase)
  }

  def unsafeLoadTestConfig: Config = {
    Config(ApplicationConfig.unsafeLoadAppConfig, DatabaseConnection.connectToTestDatabase)
  }
}

object DatabaseConnection {
  def connectToDatabase: MySQLProfile.backend.Database = {
    Database.forConfig(path = "database")
  }

  def connectToTestDatabase: MySQLProfile.backend.Database = {
    Database.forConfig(path = "testdatabase")
  }
}


object ApplicationConfig {
  def loadAppConfig: Either[ConfigReaderFailures, ApplicationConfig] = {
    pureconfig.loadConfig[ApplicationConfig]
  }

  def unsafeLoadAppConfig: ApplicationConfig = {
    pureconfig.loadConfig[ApplicationConfig] match {
      case Right(conf) => conf
      case Left(ex) =>
        println(s"Error loading configuration: $ex.\nProgram will now exit.")
        throw new Exception(ex.toString)
    }
  }

  def recoverWith(defaultConfig: ApplicationConfig): ApplicationConfig = {
    val handleFailure: ConfigReaderFailures => ApplicationConfig = fails => {
      println(
        s"""[ERROR] Failed to load config from application.conf
           |$fails
           |Setting default ApplicationConfig.""".stripMargin)
      defaultConfig
    }

    loadAppConfig.fold(handleFailure, identity)
  }
}
