package com.pwos.api.infrastructure.dao.slick.opinions.reports

import com.github.tototoshi.slick.MySQLJodaSupport._
import com.pwos.api.domain.opinions.reports.Report
import com.pwos.api.domain.opinions.reports.ReportCategory
import org.joda.time.DateTime
import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

import scala.language.postfixOps


class ReportTable(tag: Tag) extends Table[Report](tag, "REPORT") {

  implicit val enumMapping = MappedColumnType.base[ReportCategory.Value, String](_.toString, s => ReportCategory.withName(s))

  def id: Rep[Long]                               = column[Long]("ID", O.PrimaryKey, O.AutoInc)
  def authorId: Rep[Long]                         = column[Long]("AUTHOR_ID")
  def opinionId: Rep[Long]                        = column[Long]("OPINION_ID")
  def body: Rep[Option[String]]                   = column[Option[String]]("BODY")
  def reportCategory: Rep[ReportCategory.Value]   = column[ReportCategory.Value]("REPORT_CATEGORY")
  def creationDate: Rep[DateTime]                 = column[DateTime]("CREATION_DATE")


  override def * : ProvenShape[Report] = (
    authorId,
    opinionId,
    body,
    reportCategory,
    creationDate,
    id.?
  ) <> (Report.apply _ tupled, Report.unapply)
}
