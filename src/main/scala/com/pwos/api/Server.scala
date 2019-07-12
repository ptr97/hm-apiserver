package com.pwos.api

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.pwos.api.config.ApplicationConfig
import com.pwos.api.domain.places.{PlaceService, PlaceValidationInterpreter}
import com.pwos.api.infrastructure.dao.slick.DBIOMonad._
import com.pwos.api.infrastructure.dao.slick.places.SlickPlaceDAOInterpreter
import com.pwos.api.infrastructure.http.PlaceController

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}


object Server {

  def run(config: ApplicationConfig): Unit = {
    implicit val system: ActorSystem = ActorSystem("hm-apiserver")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContext = system.dispatcher


    Http().bindAndHandle(routes, config.server.host, config.server.port)
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

  private def routes(implicit ec: ExecutionContext): Route = {
    placeRoutes
  }

  private def placeRoutes(implicit ec: ExecutionContext): Route = {
    lazy val placeDAOInterpreter = SlickPlaceDAOInterpreter(ec)
    lazy val placeValidationInterpreter = PlaceValidationInterpreter(placeDAOInterpreter)
    lazy val placeService = PlaceService(placeDAOInterpreter, placeValidationInterpreter)
    lazy val placeController = PlaceController(placeService)

    placeController.placeRoutes
  }
}
