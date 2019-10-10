package com.pwos.api.infrastructure.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.pwos.api.domain.HelloMountainsError._
import com.pwos.api.domain.places.Place
import com.pwos.api.domain.places.PlaceService
import com.pwos.api.domain.places.PlaceUpdateModel
import com.pwos.api.domain.users.UserRole
import com.pwos.api.infrastructure.http.authentication.SecuredAccess
import com.pwos.api.infrastructure.http.versions._
import com.pwos.api.infrastructure.implicits._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import slick.dbio.DBIO
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext


class PlaceController(placeService: PlaceService[DBIO])(implicit ec: ExecutionContext, database: Database) extends SecuredAccess {

  val placeRoutes: Route = listPlaces ~ getPlace ~ addPlace ~ updatePlace ~ deletePlace

  import PlaceController.PLACES


  def addPlace: Route = path(v1 / PLACES) {
    authorizedPost(UserRole.Admin) { userInfo =>
      entity(as[Place]) { place: Place =>
        complete {
          placeService.create(userInfo, place).value.unsafeRun map {
            case Right(createdPlace) => HttpOps.created(createdPlace)
            case Left(placeError) => placeError match {
              case placeError: PlaceAlreadyExistsError => HttpOps.badRequest(placeError)
              case placeError: PlacePrivilegeError.type => HttpOps.badRequest(placeError)
              case _ => HttpOps.internalServerError()
            }
          }
        }
      }
    }
  }

  def getPlace: Route = path(v1 / PLACES / LongNumber) { placeId: Long =>
    authorizedGet(UserRole.User) { _ =>
      complete {
        placeService.get(placeId).value.unsafeRun map {
          case Right(place) => HttpOps.ok(place)
          case Left(placeNotFoundError) => HttpOps.notFound(placeNotFoundError)
        }
      }
    }
  }

  def updatePlace: Route = path(v1 / PLACES / LongNumber) { placeId: Long =>
    authorizedPut(UserRole.Admin) { userInfo =>
      entity(as[PlaceUpdateModel]) { placeUpdateModel: PlaceUpdateModel =>
        complete {
          placeService.update(userInfo, placeId, placeUpdateModel).value.unsafeRun map {
            case Right(updatedPlace) => HttpOps.ok(updatedPlace)
            case Left(placeError) => placeError match {
              case placeError: PlaceNotFoundError.type => HttpOps.badRequest(placeError)
              case placeError: PlacePrivilegeError.type => HttpOps.badRequest(placeError)
              case _ => HttpOps.internalServerError()
            }
          }
        }
      }
    }
  }

  def deletePlace: Route = path(v1 / PLACES / LongNumber) { placeId: Long =>
    authorizedDelete(UserRole.Admin) { userInfo =>
      complete {
        placeService.delete(userInfo, placeId).value.unsafeRun map {
          case Right(true) => HttpOps.ok("Place deleted")
          case Right(false) => HttpOps.internalServerError()
          case Left(placeError) => placeError match {
            case placeError: PlaceNotFoundError.type => HttpOps.badRequest(placeError)
            case placeError: PlacePrivilegeError.type => HttpOps.badRequest(placeError)
            case _ => HttpOps.internalServerError()
          }
        }
      }
    }
  }

  def listPlaces: Route = path(v1 / PLACES) {
    authorizedGet(UserRole.User) { _ =>
      complete {
        placeService.list().unsafeRun map (HttpOps.ok(_))
      }
    }
  }

}


object PlaceController {
  val PLACES: String = "places"

  def apply(placeService: PlaceService[DBIO])(implicit ec: ExecutionContext, database: Database): PlaceController =
    new PlaceController(placeService)
}
