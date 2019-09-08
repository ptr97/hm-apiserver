package com.pwos.api.infrastructure.dao.slick.opinions

import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

import scala.language.postfixOps


case class OpinionLike(
                       opinionId: Long,
                       userId: Long
                     )


class OpinionLikeTable(tag: Tag) extends Table[OpinionLike](tag, "OPINION_LIKE") {

  def opinionId: Rep[Long]    = column[Long]("OPINION_ID")
  def userId: Rep[Long]       = column[Long]("USER_ID")


  override def * : ProvenShape[OpinionLike] = (
    opinionId,
    userId,
  ) <> (OpinionLike.apply _ tupled, OpinionLike.unapply)
}

