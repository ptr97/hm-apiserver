package com.pwos.api.infrastructure.http

import com.pwos.api.domain.opinions.reports.ReportCategory
import com.pwos.api.domain.opinions.tags.TagCategory
import com.pwos.api.domain.users.User
import com.pwos.api.domain.users.UserRole
import io.circe.Decoder
import io.circe.Encoder
import io.circe.generic.semiauto.deriveDecoder
import io.circe.generic.semiauto.deriveEncoder
import io.circe.syntax._
import org.joda.time.DateTime


object JsonImplicits {

  implicit val userRoleDecoder: Decoder[UserRole.UserRole] = Decoder.decodeEnumeration(UserRole)
  implicit val userRoleEncoder: Encoder[UserRole.UserRole] = Encoder.encodeEnumeration(UserRole)

  implicit val userDecoder: Decoder[User] = deriveDecoder
  implicit val userEncoder: Encoder[User] = deriveEncoder

  implicit val tagCategoryDecoder: Decoder[TagCategory.TagCategory] = Decoder.decodeEnumeration(TagCategory)
  implicit val tagCategoryEncoder: Encoder[TagCategory.TagCategory] = Encoder.encodeEnumeration(TagCategory)

  implicit val reportCategoryDecoder: Decoder[ReportCategory.ReportCategory] = Decoder.decodeEnumeration(ReportCategory)
  implicit val reportCategoryEncoder: Encoder[ReportCategory.ReportCategory] = Encoder.encodeEnumeration(ReportCategory)

  implicit val dateTimeEncoder: Encoder[DateTime] = Encoder.instance(_.getMillis.asJson)
  implicit val dateTimeDecoder: Decoder[DateTime] = Decoder.instance(_.as[Long].map(new DateTime(_)))
}
