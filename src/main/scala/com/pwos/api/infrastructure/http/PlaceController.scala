package com.pwos.api.infrastructure.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.pwos.api.domain.places.PlaceService


class PlaceController[F[_]](placeService: PlaceService[F]) {

  val placeRoutes: Route = listPlaces ~ getPlace ~ addPlace


  import PlaceController.PLACES

  def listPlaces: Route = path(PLACES) {
    get {
      complete("places")
    }
  }

  def getPlace: Route = path(PLACES / LongNumber) { placeId =>
    get {
      complete(s"place with id = $placeId")
    }
  }

  def addPlace: Route = path(PLACES) {
    post {
      complete("add place")
    }
  }
}

object PlaceController {
  val PLACES: String = "places"

  def apply[F[_]](placeService: PlaceService[F]): PlaceController[F] =
    new PlaceController(placeService)
}
