package com.pwos.api.infrastructure

import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.HttpEntity.Strict
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directive
import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Directives.mapRequest
import cats.data.NonEmptyList
import com.pwos.api.domain.HelloMountainsError._
import com.pwos.api.domain.PagingRequest
import com.pwos.api.domain.QueryParameters
import com.pwos.api.domain.users.UserInfo
import com.pwos.api.infrastructure.http.authentication.JwtAuth
import io.circe.Encoder
import io.circe.generic.auto._
import io.circe.syntax._

import scala.language.postfixOps


package object http {

  object HttpResponses {

    trait ResponseModel {
      def toJson: String
    }

    case class SuccessResponse[T: Encoder](data: T, version: String = "0.1", success: Boolean = true) extends ResponseModel {
      override def toJson: String = this.asJson.spaces2
    }

    case class ErrorResponse[T: Encoder](error: T, version: String = "0.1", success: Boolean = false) extends ResponseModel {
      override def toJson: String = this.asJson.spaces2
    }

  }

  object versions {
    val v1 = "v1"
  }


  object HttpOps {

    import HttpResponses._

    private def logResponse[T: Encoder](entity: T, statusCode: StatusCode): Unit = {
      val resStartSeparator: String = s"""${separator("-", 20)}  RESPONSE  ${separator("-", 19)}"""
      val resEndSeparator: String = separator("-", 51)

      println(resStartSeparator)
      println(s"""code: $statusCode""")
      println(s"""${entity.asJson}""")
      println(resEndSeparator)
    }

    private def successResponse[T: Encoder](value: T, statusCode: StatusCode = StatusCodes.OK): HttpResponse = {
      logResponse(value, statusCode)
      HttpResponse(status = statusCode,
        entity = HttpEntity(`application/json`, SuccessResponse(value).toJson),
        headers = List(RawHeader("Access-Control-Allow-Origin", "*")))
    }

    private def clientErrorResponse[T <: HelloMountainsError : Encoder](value: T, statusCode: StatusCode = StatusCodes.BadRequest): HttpResponse = {
      logResponse(value, statusCode)
      HttpResponse(status = statusCode,
        entity = HttpEntity(`application/json`, ErrorResponse(value.message).toJson),
        headers = List(RawHeader("Access-Control-Allow-Origin", "*")))
    }

    private def clientErrorResponseNel[T <: NonEmptyList[HelloMountainsError] : Encoder](value: T, statusCode: StatusCode = StatusCodes.BadRequest): HttpResponse = {
      logResponse(value, statusCode)

      val error: NonEmptyList[String] = value.map(_.message)

      HttpResponse(status = statusCode,
        entity = HttpEntity(`application/json`, ErrorResponse(error).toJson),
        headers = List(RawHeader("Access-Control-Allow-Origin", "*")))
    }

    private def internalErrorResponse[T: Encoder](value: T, statusCode: StatusCode = StatusCodes.InternalServerError): HttpResponse = {
      logResponse(value, statusCode)
      HttpResponse(status = statusCode,
        entity = HttpEntity(`application/json`, ErrorResponse(value).toJson),
        headers = List(RawHeader("Access-Control-Allow-Origin", "*")))
    }


    def ok[T: Encoder](value: T): HttpResponse = successResponse(value)

    def created[T: Encoder](value: T): HttpResponse = successResponse(value, StatusCodes.Created)

    def badRequest[T <: HelloMountainsError : Encoder](value: T): HttpResponse = clientErrorResponse(value)

    def badRequestNel[T <: NonEmptyList[HelloMountainsError] : Encoder](value: T): HttpResponse = clientErrorResponseNel(value)

    def forbidden[T <: HelloMountainsError : Encoder](value: T): HttpResponse = clientErrorResponse(value, StatusCodes.Forbidden)

    def notFound[T <: HelloMountainsError : Encoder](value: T): HttpResponse = clientErrorResponse(value, StatusCodes.NotFound)

    def internalServerError[T: Encoder](value: T): HttpResponse = internalErrorResponse(value)


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

        val reqStartSeparator = s"""${separator("-", 20)}  REQUEST  ${separator("-", 20)}"""
        val reqEndSeparator: String = separator("-", 51)

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

  private def separator(s: String, times: Int): String = s * times

  object PagingOps {

    def pagingParameters: Directive[(QueryParameters, PagingRequest)] = {
      parameters('page.as[Int] ?, 'pageSize.as[Int] ?, 'sortBy.as[String] ?, 'filterBy.as[String] ?, 'search.as[String] ?).tmap {
        case (page, pageSize, sortBy, filterBy, search) =>
          val queryParameters: QueryParameters = QueryParameters.fromRequest(filterBy, search)
          val pagingRequest: PagingRequest = PagingRequest.fromRequest(page, pageSize, sortBy)
          (queryParameters, pagingRequest)
      }
    }

  }

}
