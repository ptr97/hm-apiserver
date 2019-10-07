package com.pwos.api

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.pwos.api.config.Config
import com.pwos.api.domain.authentication.AuthService
import com.pwos.api.domain.opinions.OpinionService
import com.pwos.api.domain.opinions.OpinionValidationAlgebra
import com.pwos.api.domain.opinions.OpinionValidationInterpreter
import com.pwos.api.domain.places.PlaceService
import com.pwos.api.domain.places.PlaceValidationInterpreter
import com.pwos.api.domain.users.UserService
import com.pwos.api.domain.users.UserValidationInterpreter
import com.pwos.api.infrastructure.dao.slick.DBIOMonad._
import com.pwos.api.infrastructure.dao.slick.opinions.SlickOpinionDAOInterpreter
import com.pwos.api.infrastructure.dao.slick.opinions.reports.SlickReportDAOInterpreter
import com.pwos.api.infrastructure.dao.slick.places.SlickPlaceDAOInterpreter
import com.pwos.api.infrastructure.dao.slick.users.SlickUserDAOInterpreter
import com.pwos.api.infrastructure.http.HttpOps
import com.pwos.api.infrastructure.http.OpinionController
import com.pwos.api.infrastructure.http.PlaceController
import com.pwos.api.infrastructure.http.UserController
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
    HttpOps.withRequestLogging {
      authRoutes ~
      userRoutes ~
      placeRoutes ~
      opinionRoutes
    }
  }

  private def authRoutes(implicit ec: ExecutionContext, database: Database): Route = {
    lazy val userDAO: SlickUserDAOInterpreter = SlickUserDAOInterpreter(ec)
    lazy val authService: AuthService[DBIO] = AuthService(userDAO)
    lazy val authController: AuthController = AuthController(authService)

    authController.authRoutes
  }

  private def userRoutes(implicit ec: ExecutionContext, database: Database): Route = {
    lazy val userDAO: SlickUserDAOInterpreter = SlickUserDAOInterpreter(ec)
    lazy val userValidation: UserValidationInterpreter[DBIO] = UserValidationInterpreter[DBIO](userDAO)
    lazy val userService: UserService[DBIO] = UserService(userDAO, userValidation)
    lazy val userController: UserController = UserController(userService)

    userController.userRoutes
  }

  private def placeRoutes(implicit ec: ExecutionContext, database: Database): Route = {
    lazy val placeDAO: SlickPlaceDAOInterpreter = SlickPlaceDAOInterpreter(ec)
    lazy val placeValidation: PlaceValidationInterpreter[DBIO] = PlaceValidationInterpreter[DBIO](placeDAO)
    lazy val placeService: PlaceService[DBIO] = PlaceService[DBIO](placeDAO, placeValidation)
    lazy val placeController: PlaceController = PlaceController(placeService)

    placeController.placeRoutes
  }

  private def opinionRoutes(implicit ec: ExecutionContext, database: Database): Route = {
    lazy val opinionDAO: SlickOpinionDAOInterpreter = SlickOpinionDAOInterpreter(ec)
    lazy val reportDAO: SlickReportDAOInterpreter = SlickReportDAOInterpreter(ec)
    lazy val userDAO: SlickUserDAOInterpreter = SlickUserDAOInterpreter(ec)
    lazy val placeDAO: SlickPlaceDAOInterpreter = SlickPlaceDAOInterpreter(ec)
    lazy val opinionValidation: OpinionValidationAlgebra[DBIO] = OpinionValidationInterpreter[DBIO](opinionDAO)
    lazy val placeValidation: PlaceValidationInterpreter[DBIO] = PlaceValidationInterpreter[DBIO](placeDAO)
    lazy val opinionService: OpinionService[DBIO] = OpinionService[DBIO](opinionDAO, reportDAO, userDAO, opinionValidation, placeValidation)
    lazy val opinionController: OpinionController = OpinionController(opinionService)

    opinionController.opinionRoutes
  }
}
