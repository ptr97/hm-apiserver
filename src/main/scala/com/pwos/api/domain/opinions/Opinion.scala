package com.pwos.api.domain.opinions

import akka.http.scaladsl.model.DateTime
import com.pwos.api.domain.tags.Tag


case class Opinion(
                    uuid: String,
                    placeId: Long,
                    authorId: Long,
                    body: Option[String],
                    tags: List[Tag],
                    likes: List[String],
                    referenceDate: DateTime,
                    creationDate: DateTime = DateTime.now,
                    lastModified: DateTime,
                    blocked: Boolean = false,
                    deleted: Boolean = false,
                    id: Option[Long]
                  )

object OpinionModels {

  case class CreateOpinionModel(
                                 placeId: Long,
                                 authorId: Long,
                                 body: Option[String],
                                 tags: List[Tag],
                                 creationDate: DateTime = DateTime.now,
                                 referenceDate: Option[DateTime]
                               )

  case class UpdateOpinionModel(
                                 uuid: String,
                                 body: Option[String],
                                 tags: Option[List[Tag]],
                                 referenceDate: Option[DateTime]
                               )

}


case class OpinionLike(
                        opinionId: Long,
                        userId: Long
                      )


case class OpinionTag(
                       opinionId: Long,
                       tagId: Long
                     )
