package com.pwos.api.infrastructure.http.authentication

import akka.http.scaladsl.server.AuthorizationFailedRejection
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import cats.data.OptionT
import com.pwos.api.domain.users.User
import com.pwos.api.domain.users.UserInfo
import com.pwos.api.domain.users.UserRole
import com.pwos.api.infrastructure.dao.slick.DBIOMonad._
import com.pwos.api.infrastructure.dao.slick.users.SlickUserDAOInterpreter
import com.pwos.api.infrastructure.implicits._
import slick.dbio.DBIO
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.language.postfixOps


abstract class SecuredAccess(implicit ec: ExecutionContext, database: Database) {

  private val userDAO: SlickUserDAOInterpreter = SlickUserDAOInterpreter(ec)

  def authorizedGet(requiredRole: UserRole.Value): Directive1[UserInfo] = authorizedMethod(get)(requiredRole)

  def authorizedPost(requiredRole: UserRole.Value): Directive1[UserInfo] = authorizedMethod(post)(requiredRole)

  def authorizedPut(requiredRole: UserRole.Value): Directive1[UserInfo] = authorizedMethod(put)(requiredRole)

  def authorizedDelete(requiredRole: UserRole.Value): Directive1[UserInfo] = authorizedMethod(delete)(requiredRole)


  private def authorizedMethod(method: Directive0): UserRole.Value => Directive1[UserInfo] =
    requiredRole => method & authorized(requiredRole)

  private def authorized(requiredRole: UserRole.Value): Directive1[UserInfo] = {
    optionalHeaderValueByName("Authorization") flatMap { maybeAuthHeader: Option[String] =>
      maybeAuthHeader.flatMap(JwtAuth.extractToken).flatMap(JwtAuth.parseToken) match {
        case Some(userInfo) if hasApiAccess(userInfo, requiredRole) => provide(userInfo)
        case _ => reject(AuthorizationFailedRejection)
      }
    }
  }

  private def hasApiAccess(userInfoFromToken: UserInfo, requiredRole: UserRole.Value): Boolean = {
    validateTokenUserInfo(userInfoFromToken) &&
    isNotBanned(userInfoFromToken) &&
    validateRequiredRole(userInfoFromToken.role, requiredRole)
  }

  private def validateTokenUserInfo(userInfoFromToken: UserInfo): Boolean = {
    val userFromDb: OptionT[DBIO, User] = OptionT(userDAO.get(userInfoFromToken.id))

    val isValidFuture: Future[Boolean] = userFromDb.filter { user: User =>
      compareUsersFromDbAndToken(user, userInfoFromToken)
    } exists { _ => true } unsafeRun

    Await.result(isValidFuture, Duration.Inf)
  }

  private def compareUsersFromDbAndToken(user: User, userInfo: UserInfo): Boolean = {
    user.id == Option(userInfo.id) && user.userName == userInfo.userName && user.email == userInfo.email &&
      user.banned == userInfo.banned && user.role == userInfo.role
  }

  private def validateRequiredRole(actualRole: UserRole.Value, requiredRole: UserRole.Value): Boolean = {
    actualRole == requiredRole || actualRole == UserRole.Admin
  }

  private def isNotBanned(userInfo: UserInfo): Boolean = {
    !userInfo.banned
  }

}
