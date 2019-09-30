package com.pwos.api.infrastructure.dao.slick.opinions

import com.github.tototoshi.slick.MySQLJodaSupport._
import com.pwos.api.domain.opinions.Opinion
import com.pwos.api.domain.opinions.tags.{Tag => HmTag}
import org.joda.time.DateTime
import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

import scala.language.postfixOps


case class OpinionDTO(
  uuid: String,
  placeId: Long,
  authorId: Long,
  body: Option[String],
  referenceDate: DateTime,
  lastModified: DateTime,
  creationDate: DateTime = DateTime.now,
  blocked: Boolean = false,
  deleted: Boolean = false,
  id: Option[Long] = None
) {
  def toOpinion(tags: List[HmTag], likes: List[String]): Opinion = {
    Opinion(
      id = id,
      uuid = uuid,
      placeId = placeId,
      authorId = authorId,
      body = body,
      tags = tags,
      likes = likes,
      referenceDate = referenceDate,
      lastModified = lastModified,
      creationDate = creationDate,
      blocked = blocked,
      deleted = deleted
    )
  }
}

object OpinionDTO {
  def fromOpinion(opinion: Opinion): OpinionDTO = {
    OpinionDTO(
      uuid = opinion.uuid,
      placeId = opinion.placeId,
      authorId = opinion.authorId,
      body = opinion.body,
      referenceDate = opinion.referenceDate,
      lastModified = opinion.lastModified,
      creationDate = opinion.creationDate,
      blocked = opinion.blocked,
      deleted = opinion.deleted,
      id = opinion.id
    )
  }
}


class OpinionTable(tag: Tag) extends Table[OpinionDTO](tag, "OPINION") {

  def id: Rep[Long]                   = column[Long]("ID", O.PrimaryKey, O.AutoInc)
  def uuid: Rep[String]               = column[String]("UUID")
  def placeId: Rep[Long]              = column[Long]("PLACE_ID")
  def authorId: Rep[Long]             = column[Long]("AUTHOR_ID")
  def body: Rep[Option[String]]       = column[Option[String]]("BODY")
  def referenceDate: Rep[DateTime]    = column[DateTime]("REFERENCE_DATE")
  def creationDate: Rep[DateTime]     = column[DateTime]("CREATION_DATE")
  def lastModified: Rep[DateTime]     = column[DateTime]("LAST_MODIFIED")
  def blocked: Rep[Boolean]           = column[Boolean]("BLOCKED")
  def deleted: Rep[Boolean]           = column[Boolean]("DELETED")


  override def * : ProvenShape[OpinionDTO] = (
    uuid,
    placeId,
    authorId,
    body,
    referenceDate,
    lastModified,
    creationDate,
    blocked,
    deleted,
    id.?,
  ) <> (OpinionDTO.apply _ tupled, OpinionDTO.unapply)
}
