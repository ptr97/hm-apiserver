package com.pwos.api.infrastructure.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.pwos.api.domain.places.Place
import com.pwos.api.domain.places.PlaceService
import com.pwos.api.domain.places.PlaceUpdateModel
import com.pwos.api.domain.users.UserInfo
import com.pwos.api.domain.users.UserRole
import com.pwos.api.infrastructure.dao.slick.DBIOMonad._
import com.pwos.api.infrastructure.http.authentication.SecuredAccess
import com.pwos.api.infrastructure.implicits._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import slick.dbio.DBIO
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext
import scala.language.postfixOps


class PlaceController(placeService: PlaceService[DBIO])(implicit ec: ExecutionContext, database: Database) extends SecuredAccess {

  val placeRoutes: Route = listPlaces ~ getPlace ~ addPlace ~ updatePlace ~ deletePlace

  import PlaceController.PLACES


  def addPlace: Route = path(PLACES) {
    authorizedPost(UserRole.Admin) { userInfo: UserInfo =>
      entity(as[Place]) { place: Place =>
        complete {
          placeService.create(place).value.unsafeRun map {
            case Right(createdPlace) => HttpOps.created(createdPlace)
            case Left(placeAlreadyExistsError) => HttpOps.badRequest(placeAlreadyExistsError)
          }
        }
      }
    }
  }

  def getPlace: Route = path(PLACES / LongNumber) { placeId: Long =>
    authorizedGet(UserRole.User) { userInfo =>
      complete {
        placeService.get(placeId).value.unsafeRun map {
          case Right(place) => HttpOps.ok(place)
          case Left(placeNotFoundError) => HttpOps.notFound(placeNotFoundError)
        }
      }
    }
  }

  def updatePlace: Route = path(PLACES / LongNumber) { placeId: Long =>
    authorizedPut(UserRole.Admin) { userInfo =>
      entity(as[PlaceUpdateModel]) { placeUpdateModel: PlaceUpdateModel =>
        complete {
          placeService.update(placeId, placeUpdateModel).value.unsafeRun map {
            case Right(updatedPlace) => HttpOps.ok(updatedPlace)
            case Left(placeNotFoundError) => HttpOps.notFound(placeNotFoundError)
          }
        }
      }
    }
  }

  def deletePlace: Route = path(PLACES / LongNumber) { placeId: Long =>
    authorizedDelete(UserRole.Admin) { userInfo =>
      complete {
        placeService.delete(placeId).value.unsafeRun map {
          case Right(true) => HttpOps.ok("Place deleted")
          case Right(false) => HttpOps.internalServerError("Something went wrong")
          case Left(placeNotFoundError) => HttpOps.notFound(placeNotFoundError)
        }
      }
    }
  }

  def listPlaces: Route = path(PLACES) {
    authorizedGet(UserRole.User) { userInfo =>
      parameters('page.as[Int] ?, 'pageSize.as[Int] ?, 'sortBy.as[String] ?, 'filterBy.as[String] ?, 'search.as[String] ?) {
        (page, pageSize, sortBy, filterBy, search) =>
          println(s"$page, $pageSize, $sortBy, $filterBy, $search")
          complete {
            val offset: Option[Int] = pageSize.flatMap(ps => page.map(p => ps * p))
            placeService.list(pageSize, offset).unsafeRun map (HttpOps.ok(_))
          }
      }
    }
  }
}


object PlaceController {
  val PLACES: String = "places"

  def apply(placeService: PlaceService[DBIO])(implicit ec: ExecutionContext, database: Database): PlaceController =
    new PlaceController(placeService)
}
