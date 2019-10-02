package com.pwos.api.domain.opinions

import com.pwos.api.domain.opinions.reports.ReportCategory
import org.joda.time.DateTime


case class Opinion(
  placeId: Long,
  authorId: Long,
  body: Option[String],
  referenceDate: DateTime = DateTime.now,
  lastModified: DateTime = DateTime.now,
  creationDate: DateTime = DateTime.now,
  blocked: Boolean = false,
  deleted: Boolean = false,
  id: Option[Long] = None
)

object Opinion {
  import OpinionModels.CreateOpinionModel

  def fromCreateOpinionModel(placeId: Long, userId: Long, createOpinionModel: CreateOpinionModel): Opinion = {
    Opinion(
      placeId = placeId,
      authorId = userId,
      body = createOpinionModel.body,
      referenceDate = createOpinionModel.referenceDate.getOrElse(DateTime.now())
    )
  }
}


object OpinionModels {

  case class OpinionView(
    opinion: Opinion,
    tags: List[String],
    likes: OpinionLikes
  )

  object OpinionView {

    def withoutLikes(opinion: Opinion, tagsNames: List[String]): OpinionView = {
      OpinionView(opinion, tagsNames, OpinionLikes.empty)
    }

  }

  case class OpinionLikes(
    likesCount: Int,
    likedByYou: Boolean
  )

  object OpinionLikes {

    def empty: OpinionLikes = {
      OpinionLikes(0, likedByYou = false)
    }

    def fromListOfIds(userId: Long, usersIds: List[Long]): OpinionLikes = {
      OpinionLikes(usersIds.length, usersIds.contains(userId))
    }

  }

  case class CreateOpinionModel(
    body: Option[String],
    tagsIds: List[Long],
    referenceDate: Option[DateTime]
  )

  case class UpdateOpinionModel(
    body: Option[String] = None,
    tagsIds: Option[List[Long]] = None,
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
