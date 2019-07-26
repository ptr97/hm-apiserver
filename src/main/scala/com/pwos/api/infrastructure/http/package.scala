package com.pwos.api.infrastructure

import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import io.circe.Encoder
import io.circe.generic.auto._
import io.circe.syntax._


package object http {

  object HttpResponses {

    trait ResponseModel {
      def toJson: String
    }

    case class SuccessResponse[T : Encoder](data: T, version: String = "0.1", success: Boolean = true) extends ResponseModel {
      override def toJson: String = this.asJson.spaces2
    }

    case class ErrorResponse[T : Encoder](error: T, version: String = "0.1", success: Boolean = false) extends ResponseModel {
      override def toJson: String = this.asJson.spaces2
    }
  }



  object HttpOps {

    import HttpResponses._

    private def successResponse[T : Encoder](value: T, statusCode: StatusCode = StatusCodes.OK): HttpResponse =
      HttpResponse(status = statusCode,
        entity = HttpEntity(`application/json`, SuccessResponse(value).toJson),
        headers = List(RawHeader("Access-Control-Allow-Origin", "*")))

    private def clientErrorResponse[T : Encoder](value: T, statusCode: StatusCode = StatusCodes.BadRequest): HttpResponse =
      HttpResponse(status = statusCode,
        entity = HttpEntity(`application/json`, ErrorResponse(value).toJson),
        headers = List(RawHeader("Access-Control-Allow-Origin", "*")))

    private def internalErrorResponse[T : Encoder](value: T, statusCode: StatusCode = StatusCodes.InternalServerError): HttpResponse =
      HttpResponse(status = statusCode,
        entity = HttpEntity(`application/json`, ErrorResponse(value).toJson),
        headers = List(RawHeader("Access-Control-Allow-Origin", "*")))


    def ok[T : Encoder](value: T): HttpResponse = successResponse(value)

    def created[T : Encoder](value: T): HttpResponse = successResponse(value, StatusCodes.Created)

    def badRequest[T : Encoder](value: T): HttpResponse = clientErrorResponse(value)

    def forbidden[T : Encoder](value: T): HttpResponse = clientErrorResponse(value, StatusCodes.Forbidden)

    def notFound[T : Encoder](value: T): HttpResponse = clientErrorResponse(value, StatusCodes.NotFound)

    def internalServerError[T: Encoder](value : T): HttpResponse = internalErrorResponse(value)
  }

}
