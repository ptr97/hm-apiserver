package com.pwos.api.infrastructure.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.pwos.api.domain.HelloMountainsError._
import com.pwos.api.domain.opinions.OpinionModels._
import com.pwos.api.domain.opinions.OpinionService
import com.pwos.api.domain.users.UserInfo
import com.pwos.api.domain.users.UserRole
import com.pwos.api.infrastructure.http.JsonImplicits._
import com.pwos.api.infrastructure.http.PagingOps._
import com.pwos.api.infrastructure.http.authentication.SecuredAccess
import com.pwos.api.infrastructure.http.versions._
import com.pwos.api.infrastructure.implicits._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import slick.dbio.DBIO
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext


class OpinionController(opinionService: OpinionService[DBIO])(implicit ec: ExecutionContext, database: Database) extends SecuredAccess {

  import OpinionController._
  import PlaceController.PLACES


  def listAllOpinions: Route = path(v1 / PLACES / OPINIONS) {
    authorizedGet(UserRole.Admin) { _ =>
      pagingParameters { (queryParameters, pagingRequest) =>
        complete {
          opinionService.list(None, queryParameters, pagingRequest).unsafeRun map (HttpOps.ok(_))
        }
      }
    }
  }

  def getOpinionReports: Route = path(v1 / PLACES / OPINIONS / LongNumber / REPORTS) { opinionId: Long =>
    authorizedGet(UserRole.Admin) { _ =>
      pagingParameters { (queryParameters, pagingRequest) =>
        complete {
          opinionService.reports(opinionId, queryParameters, pagingRequest).value.unsafeRun map {
            case Right(reports) => HttpOps.ok(reports)
            case Left(opinionNotFoundError) => HttpOps.badRequest(opinionNotFoundError)
          }
        }
      }
    }
  }

  def listOpinionsForPlace: Route = path(v1 / PLACES / LongNumber / OPINIONS) { placeId: Long =>
    authorizedGet(UserRole.User) { _ =>
      pagingParameters { (queryParameters, pagingRequest) =>
        complete {
          opinionService.list(Some(placeId), queryParameters, pagingRequest).unsafeRun map (HttpOps.ok(_))
        }
      }
    }
  }

  def addOpinion: Route = path(v1 / PLACES / LongNumber / OPINIONS) { placeId: Long =>
    authorizedPost(UserRole.User) { userInfo: UserInfo =>
      entity(as[CreateOpinionModel]) { createOpinionModel: CreateOpinionModel =>
        complete {
          opinionService.addOpinion(userInfo, placeId, createOpinionModel).value.unsafeRun map {
            case Right(opinion) => HttpOps.created(opinion)
            case Left(placeNotFoundError) => HttpOps.badRequest(placeNotFoundError)
          }
        }
      }
    }
  }

  def getOpinion: Route = path(v1 / OPINIONS / LongNumber) { opinionId: Long =>
    authorizedGet(UserRole.User) { _ =>
      complete {
        opinionService.getOpinion(opinionId).value.unsafeRun map {
          case Right(opinion) => HttpOps.created(opinion)
          case Left(opinionNotFoundError) => HttpOps.notFound(opinionNotFoundError)
        }
      }
    }
  }

  def deleteOpinion: Route = path(v1 / OPINIONS / LongNumber) { opinionId: Long =>
    authorizedDelete(UserRole.User) { userInfo: UserInfo =>
      complete {
        opinionService.deleteOpinion(userInfo, opinionId).value.unsafeRun map {
          case Right(true) => HttpOps.ok("Opinion deleted")
          case Right(false) => HttpOps.internalServerError("Something went wrong")
          case Left(opinionError) => opinionError match {
            case opinionNotFoundError: OpinionNotFoundError.type => HttpOps.notFound(opinionNotFoundError)
            case opinionDeletePrivilegeError: OpinionOwnershipError.type => HttpOps.forbidden(opinionDeletePrivilegeError)
          }
        }
      }
    }
  }

  def updateOpinion: Route = path(v1 / OPINIONS / LongNumber) { opinionId: Long =>
    authorizedPut(UserRole.User) { userInfo: UserInfo =>
      entity(as[UpdateOpinionModel]) { updateOpinionModel =>
        complete {
          opinionService.updateOpinion(userInfo, opinionId, updateOpinionModel).value.unsafeRun map {
            case Right(opinion) => HttpOps.ok(opinion)
            case Left(opinionValidationError) => opinionValidationError match {
              case opinionNotFoundError: OpinionNotFoundError.type => HttpOps.notFound(opinionNotFoundError)
              case opinionDeletePrivilegeError: OpinionOwnershipError.type => HttpOps.forbidden(opinionDeletePrivilegeError)
            }
          }
        }
      }
    }
  }

  def reportOpinion: Route = path(v1 / OPINIONS / LongNumber / REPORTS) { opinionId: Long =>
    authorizedPost(UserRole.User) { userInfo: UserInfo =>
      entity(as[ReportOpinionModel]) { reportOpinionModel =>
        complete {
          opinionService.reportOpinion(userInfo, opinionId, reportOpinionModel).value.unsafeRun map {
            case Right(true) => HttpOps.ok("Opinion reported")
            case Left(opinionNotFoundError) => HttpOps.badRequest(opinionNotFoundError)
          }
        }
      }
    }
  }

  def updateOpinionStatus: Route = path(v1 / OPINIONS / LongNumber / "status") { opinionId: Long =>
    authorizedPut(UserRole.Admin) { userInfo: UserInfo =>
      entity(as[UpdateOpinionStatusModel]) { updateOpinionStatusModel =>
        complete {
          opinionService.updateOpinionStatus(userInfo, opinionId, updateOpinionStatusModel).value.unsafeRun map {
            case Right(opinion) => HttpOps.ok(opinion)
            case Left(opinionNotFoundError) => HttpOps.badRequest(opinionNotFoundError)
          }
        }
      }
    }
  }

  def updateOpinionLikes: Route = path(v1 / PLACES / LongNumber / OPINIONS / LongNumber / "likes") { (_: Long, opinionId: Long) =>
    authorizedPut(UserRole.User) { userInfo =>
      entity(as[UpdateOpinionLikesModel]) { upUpdateOpinionLikesModel =>
        complete {
          opinionService.updateOpinionLikes(userInfo, opinionId, upUpdateOpinionLikesModel).value.unsafeRun map {
            case Right(opinion) => HttpOps.ok(opinion)
            case Left(opinionValidationError) => HttpOps.badRequest(opinionValidationError)
          }
        }
      }
    }
  }

}


object OpinionController {
  val OPINIONS = "opinions"
  val REPORTS = "reports"

  def apply(opinionService: OpinionService[DBIO])(implicit ec: ExecutionContext, database: Database): OpinionController =
    new OpinionController(opinionService)
}
