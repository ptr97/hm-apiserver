package com.pwos.api.infrastructure.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.pwos.api.domain.PlaceAlreadyExistsError
import com.pwos.api.domain.PlaceNotFoundError
import com.pwos.api.domain.places.Place
import com.pwos.api.domain.places.PlaceService
import com.pwos.api.infrastructure.dao.slick.DBIOMonad._
import com.pwos.api.infrastructure.implicits._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import slick.dbio.DBIO
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext
import scala.concurrent.Future


class PlaceController(placeService: PlaceService[DBIO])(implicit ec: ExecutionContext, database: Database) {

  val placeRoutes: Route = listPlaces ~ getPlace ~ addPlace

  import PlaceController.PLACES


  def addPlace: Route = path(PLACES) {
    post {
      entity(as[Place]) { place: Place =>
        val createdPlaceDBIO: DBIO[Either[PlaceAlreadyExistsError, Place]] = placeService.create(place).value
        val createdPlaceFuture: Future[Either[PlaceAlreadyExistsError, Place]] = createdPlaceDBIO.unsafeRun
        val response: Future[Json] = createdPlaceFuture.map {
          case Right(createdPlace) => {
            val t = createdPlace.asJson
            println(t)
            t
          }
          case Left(placeAlreadyExistsError) => placeAlreadyExistsError.asJson
        }

        complete(response)
      }
    }
  }

  def getPlace: Route = path(PLACES / LongNumber) { placeId: Long =>
    get {
      val placeDBIO: DBIO[Either[PlaceNotFoundError.type, Place]] = placeService.get(placeId).value
      val placeFuture: Future[Either[PlaceNotFoundError.type, Place]] = placeDBIO.unsafeRun

      val response = placeFuture.map {
        case Right(place) => {
          val t = place.asJson
          println(t)
          t
        }
        case Left(placeNotFoundError) => {
          val t = placeNotFoundError.asJson
          println(t)
          t
        }
      }

      complete(response)
    }
  }

  def updatePlace: Route = path(PLACES / LongNumber) { placeId =>
    put {
      complete(s"update place with id = $placeId")
    }
  }

  def deletePlace: Route = path(PLACES / LongNumber) { placeId =>
    delete {
      complete(s"delete place with id = $placeId")
    }
  }

  def listPlaces: Route = path(PLACES) {
    get {
      complete("places")
    }
  }
}

object PlaceController {
  val PLACES: String = "places"

  def apply(placeService: PlaceService[DBIO])(implicit ec: ExecutionContext, database: Database): PlaceController =
    new PlaceController(placeService)
}
