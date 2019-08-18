package com.pwos.api

import akka.http.scaladsl.model.HttpEntity.Strict
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.Directives.mapRequest
import com.pwos.api.domain.users.UserInfo
import com.pwos.api.infrastructure.http.authentication.JwtAuth
import slick.dbio.DBIO
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext
import scala.concurrent.Future


package object infrastructure {

  object implicits {

    implicit class DBIOtoFuture[T](dbio: DBIO[T])(implicit ec: ExecutionContext, db: Database) {
      def unsafeRun: Future[T] = db.run(dbio)
    }

  }

  object RequestOps {

    def withRequestLogging: Directive0 = {
      mapRequest { request: HttpRequest =>
        val entity: String = request.entity match {
          case strict: Strict => strict.data.utf8String
          case _ => request.entity.toString
        }

        val extractUserInfoFromRequest: HttpRequest => Option[UserInfo] = request => {
          for {
            authHeader: String <- request.headers.find(_.lowercaseName == "authorization").map(_.value)
            token <- JwtAuth.extractToken(authHeader)
            userInfo <- JwtAuth.parseToken(token)
          } yield userInfo
        }

        val userInfoPrettyPrint: Option[UserInfo] => String = maybeUserInfo => {
          maybeUserInfo.map { ui =>
            s"""ID = ${ui.id}, userName = ${ui.userName}, email = ${ui.email}, role = ${ui.role}, banned = ${ui.banned}"""
          } getOrElse "Not provided"
        }

        val separator: (String, Int) => String = (s, times) => s * times
        val reqStartSeparator = s"""${separator("-", 20)}  REQUEST  ${separator("-", 20)}"""
        val reqEndSeparator = separator("-", 51)

        println(reqStartSeparator)
        println(s"User Info: ${userInfoPrettyPrint(extractUserInfoFromRequest(request))}")
        println(s"Request Path: ${request.uri.path}")
        println(s"Request Method: ${request.method}")
        println(s"Request Query: ${request.uri.rawQueryString.getOrElse("")}")
        println(s"Body: $entity")
        println(reqEndSeparator)
        request
      }
    }
  }

}
