package com.pwos.api.domain.opinions.reports

import com.pwos.api.domain.opinions.OpinionModels.ReportOpinionModel
import org.joda.time.DateTime


object ReportCategory extends Enumeration {
  type ReportCategory = Value

  val MISLEADING: ReportCategory = Value("misleading")
  val VULGAR: ReportCategory = Value("vulgar")
  val FAULTY: ReportCategory = Value("faulty")
}


case class Report(
  authorId: Long,
  opinionId: Long,
  body: Option[String],
  reportCategory: ReportCategory.ReportCategory,
  creationDate: DateTime = DateTime.now,
  id: Option[Long]
)

object Report {
  def fromReportOpinionModel(authorId: Long, opinionId: Long, reportOpinionModel: ReportOpinionModel): Report = {
    Report(
      authorId = authorId,
      opinionId = opinionId,
      body = reportOpinionModel.body,
      reportCategory = reportOpinionModel.reportCategory,
      creationDate = DateTime.now,
      id = None
    )
  }
}
