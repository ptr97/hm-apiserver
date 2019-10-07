package com.pwos.api.infrastructure.dao.slick.opinions.reports

import cats.implicits._
import com.pwos.api.domain.opinions.reports.Report
import com.pwos.api.domain.opinions.reports.ReportDAOAlgebra
import slick.dbio.DBIO
import slick.lifted.TableQuery
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext


class SlickReportDAOInterpreter(implicit ec: ExecutionContext) extends ReportDAOAlgebra[DBIO] {

  private val reports: TableQuery[ReportTable] = TableQuery[ReportTable]

  override def create(report: Report): DBIO[Report] = {
    reports returning reports
      .map(_.id) into((report, id) => report.copy(id = id.some)) += report
  }

  override def list(opinionId: Long): DBIO[List[Report]] = {
    reports.filter(_.opinionId === opinionId).result.map(_.toList)
  }

}

object SlickReportDAOInterpreter {
  def apply(implicit ec: ExecutionContext): SlickReportDAOInterpreter =
    new SlickReportDAOInterpreter()
}
