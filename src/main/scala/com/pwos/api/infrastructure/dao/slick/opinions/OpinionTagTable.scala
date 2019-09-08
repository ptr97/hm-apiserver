package com.pwos.api.infrastructure.dao.slick.opinions

import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

import scala.language.postfixOps


case class OpinionTag(
                      opinionId: Long,
                      tagId: Long
                     )


class OpinionTagTable(tag: Tag) extends Table[OpinionTag](tag, "OPINION_TAG") {

  def opinionId: Rep[Long]  = column[Long]("OPINION_ID")
  def tagId: Rep[Long]      = column[Long]("TAG_ID")


  override def * : ProvenShape[OpinionTag] = (
    opinionId,
    tagId,
  ) <> (OpinionTag.apply _ tupled, OpinionTag.unapply)
}
