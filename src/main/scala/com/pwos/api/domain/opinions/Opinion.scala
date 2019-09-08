package com.pwos.api.domain.opinions

import com.pwos.api.domain.opinions.tags.Tag
import org.joda.time.DateTime


case class Opinion(
                    uuid: String,
                    placeId: Long,
                    authorId: Long,
                    body: Option[String],
                    tags: List[Tag],
                    likes: List[String],
                    referenceDate: DateTime,
                    lastModified: DateTime,
                    creationDate: DateTime = DateTime.now,
                    blocked: Boolean = false,
                    deleted: Boolean = false,
                    id: Option[Long] = None
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
                                 body: Option[String],
                                 tags: Option[List[Tag]],
                                 referenceDate: Option[DateTime]
                               )

}
