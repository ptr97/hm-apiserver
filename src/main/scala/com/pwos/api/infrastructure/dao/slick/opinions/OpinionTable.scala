package com.pwos.api.infrastructure.dao.slick.opinions

import com.github.tototoshi.slick.MySQLJodaSupport._
import com.pwos.api.domain.opinions.Opinion
import org.joda.time.DateTime
import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

import scala.language.postfixOps


class OpinionTable(tag: Tag) extends Table[Opinion](tag, "OPINION") {

  def id: Rep[Long]                   = column[Long]("ID", O.PrimaryKey, O.AutoInc)
  def placeId: Rep[Long]              = column[Long]("PLACE_ID")
  def authorId: Rep[Long]             = column[Long]("AUTHOR_ID")
  def body: Rep[Option[String]]       = column[Option[String]]("BODY")
  def referenceDate: Rep[DateTime]    = column[DateTime]("REFERENCE_DATE")
  def creationDate: Rep[DateTime]     = column[DateTime]("CREATION_DATE")
  def lastModified: Rep[DateTime]     = column[DateTime]("LAST_MODIFIED")
  def blocked: Rep[Boolean]           = column[Boolean]("BLOCKED")
  def deleted: Rep[Boolean]           = column[Boolean]("DELETED")


  override def * : ProvenShape[Opinion] = (
    placeId,
    authorId,
    body,
    referenceDate,
    lastModified,
    creationDate,
    blocked,
    deleted,
    id.?,
  ) <> (Opinion.apply _ tupled, Opinion.unapply)
}
