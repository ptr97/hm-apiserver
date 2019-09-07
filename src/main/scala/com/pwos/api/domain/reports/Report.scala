package com.pwos.api.domain.reports

import akka.http.scaladsl.model.DateTime


object ReportCategories extends Enumeration {
  type ReportCategory = Value

  val MISLEADING: ReportCategory = Value("misleading")
  val VULGAR: ReportCategory = Value("vulgar")
  val FAULTY: ReportCategory = Value("faulty")
}


case class Report(
                   authorId: Long,
                   opinionId: Long,
                   body: Option[String],
                   reportCategory: ReportCategories.ReportCategory,
                   creationDate: DateTime = DateTime.now,
                   id: Option[Long]
                 )

