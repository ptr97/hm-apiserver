package com.pwos.api.infrastructure.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cats.data.Validated.Invalid
import cats.data.Validated.Valid
import com.pwos.api.domain.users.UserModels._
import com.pwos.api.domain.users.UserRole
import com.pwos.api.domain.users.UserService
import com.pwos.api.infrastructure.http.authentication.SecuredAccess
import com.pwos.api.infrastructure.implicits._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import slick.dbio.DBIO
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext
import scala.language.postfixOps


class UserController(userService: UserService[DBIO])(implicit ec: ExecutionContext, database: Database) extends SecuredAccess {

  val userRoutes: Route = listUsers ~ getUser ~ getSelfData ~ addUser ~ updateSelf ~ updateUserStatus ~ updatePassword ~ deleteAccount

  import UserController.USERS


  def listUsers: Route = path(USERS) {
    authorizedGet(UserRole.Admin) { _ =>
      parameters('page.as[Int] ?, 'pageSize.as[Int] ?, 'sortBy.as[String] ?, 'filterBy.as[String] ?, 'search.as[String] ?) {
        (page, pageSize, sortBy, filterBy, search) =>
          println(s"User Page Params = $page, $pageSize, $sortBy, $filterBy, $search")
          complete {
            val offset: Option[Int] = pageSize.flatMap(ps => page.map(p => ps * p))
            userService.list(pageSize, offset).unsafeRun map (HttpOps.ok(_))
          }
      }
    }
  }

  def getUser: Route = path(USERS / LongNumber) { userId: Long =>
    authorizedGet(UserRole.Admin) { _ =>
      complete {
        userService.getFullData(userId).value.unsafeRun map {
          case Right(user) => HttpOps.ok(user)
          case Left(userNotFoundError) => HttpOps.notFound(userNotFoundError)
        }
      }
    }
  }

  def getSelfData: Route = path(USERS / "me") {
    authorizedGet(UserRole.User) { userInfo =>
      complete {
        userService.getSimpleView(userInfo.id).value.unsafeRun map {
          case Right(user) => HttpOps.ok(user)
          case Left(userNotFoundError) => HttpOps.notFound(userNotFoundError)
        }
      }
    }
  }

  def addUser: Route = path(USERS) {
    post {
      entity(as[CreateUserModel]) { createUserModel =>
        complete {
          userService.create(createUserModel).value.unsafeRun map {
            case Right(userView) => HttpOps.created(userView)
            case Left(errorList) => HttpOps.badRequestNel(errorList)
          }
        }
      }
    }
  }

  def updateSelf: Route = path(USERS / "me") {
    authorizedPut(UserRole.User) { userInfo =>
      entity(as[UpdateUserCredentialsModel]) { updateCredentialsModel =>
        complete {
          userService.updateCredentials(userInfo.id, updateCredentialsModel).value.unsafeRun map {
            case Right(userView) => HttpOps.ok(userView)
            case Left(errorList) => HttpOps.badRequestNel(errorList)
          }
        }
      }
    }
  }

  def updateUserStatus: Route = path(USERS / LongNumber) { userId: Long =>
    authorizedPut(UserRole.Admin) { _ =>
      entity(as[UpdateUserStatusModel]) { updateStatusModel =>
        complete {
          userService.updateStatus(userId, updateStatusModel).value.unsafeRun map {
            case Right(user) => HttpOps.ok(user)
            case Left(userNotFoundError) => HttpOps.badRequest(userNotFoundError)
          }
        }
      }
    }
  }

  def updatePassword: Route = path(USERS / "me" / "password") {
    authorizedPut(UserRole.User) { userInfo =>
      entity(as[ChangePasswordModel]) { changePasswordModel =>
        complete {
          userService.updatePassword(userInfo.id, changePasswordModel).value.unsafeRun map {
            case Right(success) => HttpOps.ok(success)
            case Left(errorList) => HttpOps.badRequestNel(errorList)
          }
        }
      }
    }
  }

  def deleteAccount: Route = path(USERS / "me") {
    authorizedDelete(UserRole.User) { userInfo =>
      complete {
        userService.delete(userInfo.id).value.unsafeRun map {
          case Right(success) => HttpOps.ok(success)
          case Left(userNotFoundError) => HttpOps.badRequest(userNotFoundError)
        }
      }
    }
  }

}


object UserController {
  val USERS: String = "users"

  def apply(userService: UserService[DBIO])(implicit ec: ExecutionContext, database: Database): UserController =
    new UserController(userService)
}
