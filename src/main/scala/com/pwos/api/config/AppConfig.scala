package com.pwos.api.config

import pureconfig.generic.auto._
import pureconfig.error.ConfigReaderFailures


case class ApplicationConfig(database: DatabaseConfig, server: ServerConfig)
case class DatabaseConfig(profile: String, dataSourceClass: String, connection: DatabaseConnection)
case class DatabaseConnection(url: String, driver: String, user: String, password: String)
case class ServerConfig(host: String, port: Int)


object ApplicationConfig {
  def loadConfig: Either[ConfigReaderFailures, ApplicationConfig] = {
    pureconfig.loadConfig[ApplicationConfig]
  }

  def unsafeLoadConfig: ApplicationConfig = {
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

    loadConfig.fold(handleFailure, identity)
  }

  def recoverWithDefaultConfig: ApplicationConfig = {
    val defaultConfig: ApplicationConfig = ApplicationConfig(
      DatabaseConfig("slick.jdbc.MySQLProfile$", "slick.jdbc.DatabaseUrlDataSource",
        DatabaseConnection("jdbc:mysql://localhost:3306/hm_db", "com.mysql.cj.jdbc.Driver", "piotr", "password123")),
      ServerConfig("127.0.0.1", 7007))
    recoverWith(defaultConfig)
  }
}
