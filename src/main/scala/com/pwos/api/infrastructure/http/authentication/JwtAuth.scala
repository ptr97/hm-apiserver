package com.pwos.api.infrastructure.http.authentication

import akka.http.scaladsl.model.DateTime
import com.pwos.api.config.JwtConfig
import com.pwos.api.domain.users.UserInfo
import com.pwos.api.domain.users.UserRole
import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json
import io.circe.ParsingFailure
import io.circe.generic.auto._
import io.circe.jawn.parse
import io.circe.syntax._
import pdi.jwt.JwtAlgorithm
import pdi.jwt.JwtCirce
import pdi.jwt.JwtClaim

import scala.language.postfixOps


case class JsonWebToken(token: String)


object JwtAuth {

  private val jwtConfig = JwtConfig.unsafeLoadJwtConfig

  private val secretKey: String = jwtConfig.jwt.secret
  private val validFor: Long = jwtConfig.jwt.validFor
  private val hashAlgorithm = JwtAlgorithm.HS256

  private implicit val userRoleDecoder: Decoder[UserRole.Value] = Decoder.decodeEnumeration(UserRole)
  private implicit val userRoleEncoder: Encoder[UserRole.Value] = Encoder.encodeEnumeration(UserRole)

  def extractToken(authHeader: String): Option[JwtClaim] = {
    val prefix: String = "Bearer "
    if (authHeader.startsWith(prefix)) {
      val token: String = authHeader.substring(prefix.length)
      JwtCirce.decode(token, secretKey, Seq(hashAlgorithm)).toOption
    } else {
      None
    }
  }

  def parseToken(jwtClaim: JwtClaim): Option[UserInfo] = {
    jwtClaim.expiration filter { expirationTimestamp: Long =>
      expirationTimestamp > DateTime.now.clicks
    } flatMap { _ =>
      val maybeToken: Either[ParsingFailure, Json] = parse(jwtClaim.content)
      maybeToken.flatMap { token =>
        token.as[UserInfo]
      } toOption
    }
  }

  def decodeJwt(userInfo: UserInfo): JsonWebToken = {
    val now: Long = DateTime.now.clicks / 1000

    val jwtClaim: JwtClaim = JwtClaim(
      expiration = Some(now + validFor),
      issuedAt = Some(now),
      content = userInfo.asJson.toString
    )
    val token: String = JwtCirce.encode(claim = jwtClaim, key = secretKey, algorithm = hashAlgorithm)
    JsonWebToken(token)
  }
}
