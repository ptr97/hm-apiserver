package com.pwos.api

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.pwos.api.config.Config
import com.pwos.api.domain.authentication.AuthService
import com.pwos.api.domain.places.PlaceService
import com.pwos.api.domain.places.PlaceValidationInterpreter
import com.pwos.api.domain.users.UserValidationInterpreter
import com.pwos.api.infrastructure.RequestOps
import com.pwos.api.infrastructure.dao.slick.DBIOMonad._
import com.pwos.api.infrastructure.dao.slick.places.SlickPlaceDAOInterpreter
import com.pwos.api.infrastructure.dao.slick.users.SlickUserDAOInterpreter
import com.pwos.api.infrastructure.http.PlaceController
import com.pwos.api.infrastructure.http.authentication.AuthController
import slick.dbio.DBIO
import slick.jdbc.MySQLProfile.backend.Database

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.util.Failure
import scala.util.Success


object Server {

  def run(config: Config): Unit = {
    implicit val system: ActorSystem = ActorSystem("hm-apiserver")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContext = system.dispatcher

    implicit val db: Database = config.database

    Http().bindAndHandle(routes, config.api.server.host, config.api.server.port)
      .onComplete {
        case Success(bound) =>
          println(s"Server online at http://${bound.localAddress.getHostString}:${bound.localAddress.getPort}/")
        case Failure(e) =>
          Console.err.println(s"Server could not start!")
          e.printStackTrace()
          system.terminate()
      }

    Await.result(system.whenTerminated, Duration.Inf)
  }

  private def routes(implicit ec: ExecutionContext, database: Database): Route = {
    RequestOps.withRequestLogging {
      authRoutes ~
      placeRoutes
    }
  }

  private def authRoutes(implicit ec: ExecutionContext, database: Database): Route = {
    lazy val userDAO: SlickUserDAOInterpreter = SlickUserDAOInterpreter(ec)
    lazy val userValidation: UserValidationInterpreter[DBIO] = UserValidationInterpreter[DBIO](userDAO)
    lazy val authService: AuthService[DBIO] = AuthService(userDAO, userValidation)
    lazy val authController: AuthController = AuthController(authService)

    authController.authRoutes
  }

  private def placeRoutes(implicit ec: ExecutionContext, database: Database): Route = {
    lazy val placeDAOInterpreter: SlickPlaceDAOInterpreter = SlickPlaceDAOInterpreter(ec)
    lazy val placeValidationInterpreter: PlaceValidationInterpreter[DBIO] = PlaceValidationInterpreter[DBIO](placeDAOInterpreter)
    lazy val placeService: PlaceService[DBIO] = PlaceService[DBIO](placeDAOInterpreter, placeValidationInterpreter)
    lazy val placeController: PlaceController = PlaceController(placeService)

    placeController.placeRoutes
  }
}
