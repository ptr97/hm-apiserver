package com.pwos.api.infrastructure.http.authentication

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.pwos.api.domain.authentication.AuthService
import com.pwos.api.domain.users.UserModels.LoginModel
import com.pwos.api.infrastructure.dao.slick.DBIOMonad._
import com.pwos.api.infrastructure.http.HttpOps
import com.pwos.api.infrastructure.http.versions._
import com.pwos.api.infrastructure.implicits._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import slick.dbio.DBIO
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext


class AuthController(authService: AuthService[DBIO])(implicit ec: ExecutionContext, database: Database) {

  val authRoutes: Route = logIn

  import AuthController.LOG_IN

  def logIn: Route = path(v1 / LOG_IN) {
    post {
      entity(as[LoginModel]) { loginModel: LoginModel =>
        complete {
          authService.logIn(loginModel).value.unsafeRun map {
            case Right(jwt) => HttpOps.ok(jwt)
            case Left(incorrectCredentials) => HttpOps.unauthorized(incorrectCredentials)
          }
        }
      }
    }
  }

}

object AuthController {
  val LOG_IN = "logIn"

  def apply(authService: AuthService[DBIO])(implicit ec: ExecutionContext, database: Database): AuthController =
    new AuthController(authService)
}
