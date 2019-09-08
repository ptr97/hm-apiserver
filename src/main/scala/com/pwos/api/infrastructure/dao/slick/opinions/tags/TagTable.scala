package com.pwos.api.infrastructure.dao.slick.opinions.tags

import com.pwos.api.domain.opinions.tags.TagCategory
import com.pwos.api.domain.opinions.tags.{Tag => HmTag}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

import scala.language.postfixOps


class TagTable(tag: Tag) extends Table[HmTag](tag, "TAG") {

  implicit val enumMapping = MappedColumnType.base[TagCategory.Value, String](_.toString, s => TagCategory.withName(s))

  def id: Rep[Long]                             = column[Long]("ID", O.PrimaryKey, O.AutoInc)
  def uuid: Rep[String]                         = column[String]("UUID")
  def name: Rep[String]                         = column[String]("NAME")
  def enabled: Rep[Boolean]                     = column[Boolean]("ENABLED")
  def tagCategory: Rep[TagCategory.Value]       = column[TagCategory.Value]("REPORT_CATEGORY")


  override def * : ProvenShape[HmTag] = (
    uuid,
    name,
    enabled,
    tagCategory,
    id.?
  ) <> (HmTag.apply _ tupled, HmTag.unapply)
}
