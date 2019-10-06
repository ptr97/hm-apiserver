package com.pwos.api.infrastructure.dao.memory

import cats.Id
import cats.implicits._
import com.pwos.api.domain.opinions.reports.Report
import com.pwos.api.domain.opinions.reports.ReportDAOAlgebra


class MemoryReportDAOInterpreter extends ReportDAOAlgebra[Id] {

  private var reports: List[Report] = List.empty
  private var reportIdAutoIncrement: Long = 1

  override def create(report: Report): Id[Report] = {
    val reportWithId: Report = report.copy(id = Some(reportIdAutoIncrement))
    this.reportIdAutoIncrement += 1
    this.reports = reportWithId :: this.reports
    reportWithId
  }

  override def list(opinionId: Long): Id[List[Report]] = {
    this.reports.filter(_.opinionId === opinionId)
  }

}

object MemoryReportDAOInterpreter {
  def apply(): MemoryReportDAOInterpreter =
    new MemoryReportDAOInterpreter()
}
