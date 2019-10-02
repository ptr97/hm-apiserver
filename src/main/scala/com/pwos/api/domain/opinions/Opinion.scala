package com.pwos.api.domain.opinions

import com.pwos.api.domain.opinions.OpinionModels.CreateOpinionModel
import com.pwos.api.domain.opinions.reports.ReportCategory
import com.pwos.api.domain.opinions.tags.Tag
import org.joda.time.DateTime


case class Opinion(
  placeId: Long,
  authorId: Long,
  body: Option[String],
  tags: List[Tag],
  likes: List[String] = List.empty,
  referenceDate: DateTime = DateTime.now,
  lastModified: DateTime = DateTime.now,
  creationDate: DateTime = DateTime.now,
  blocked: Boolean = false,
  deleted: Boolean = false,
  id: Option[Long] = None
)

object Opinion {
  def fromCreateOpinionModel(placeId: Long, userId: Long, createOpinionModel: CreateOpinionModel): Opinion = {
    Opinion(
      placeId = placeId,
      authorId = userId,
      body = createOpinionModel.body,
      tags = createOpinionModel.tags,
      referenceDate = createOpinionModel.referenceDate.getOrElse(DateTime.now())
    )
  }
}


object OpinionModels {

  case class CreateOpinionModel(
    body: Option[String],
    tags: List[Tag],
    creationDate: DateTime = DateTime.now,
    referenceDate: Option[DateTime]
  )

  case class UpdateOpinionModel(
    body: Option[String] = None,
    tags: Option[List[Tag]] = None,
    referenceDate: Option[DateTime] = None
  )

  case class UpdateOpinionStatusModel(
    blocked: Boolean
  )

  case class UpdateOpinionLikesModel(
    like: Boolean
  )

  case class ReportOpinionModel(
    body: Option[String],
    reportCategory: ReportCategory.ReportCategory
  )

}
