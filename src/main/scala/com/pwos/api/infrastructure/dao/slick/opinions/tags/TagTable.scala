package com.pwos.api.infrastructure.dao.slick.opinions.tags

import com.pwos.api.domain.opinions.tags.TagCategory
import com.pwos.api.domain.opinions.tags.{Tag => HmTag}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

import scala.language.postfixOps


class TagTable(tag: Tag) extends Table[HmTag](tag, "TAG") {

  implicit val enumMapping = MappedColumnType.base[TagCategory.Value, String](_.toString, s => TagCategory.withName(s))

  def id: Rep[Long]                             = column[Long]("ID", O.PrimaryKey, O.AutoInc)
  def name: Rep[String]                         = column[String]("NAME")
  def tagCategory: Rep[TagCategory.Value]       = column[TagCategory.Value]("REPORT_CATEGORY")
  def enabled: Rep[Boolean]                     = column[Boolean]("ENABLED")


  override def * : ProvenShape[HmTag] = (
    name,
    tagCategory,
    enabled,
    id.?
  ) <> (HmTag.apply _ tupled, HmTag.unapply)
}
