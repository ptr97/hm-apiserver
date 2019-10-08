package com.pwos.api.domain.opinions

import com.pwos.api.PaginatedResult
import com.pwos.api.domain.PagingRequest
import com.pwos.api.domain.QueryParameters


trait OpinionDAOAlgebra[F[_]] {

  type OpinionWithTagsAndLikes = (Opinion, List[String], List[Long])

  def create(opinion: Opinion): F[Opinion]

  def addTags(opinionId: Long, tagsIds: List[Long]): F[Boolean]

  def removeTags(opinionId: Long): F[Boolean]

  def addLike(opinionId: Long, userId: Long): F[Boolean]

  def removeLike(opinionId: Long, userId: Long): F[Boolean]

  def getOpinionView(opinionId: Long): F[Option[OpinionWithTagsAndLikes]]

  def getActiveOpinionView(opinionId: Long): F[Option[OpinionWithTagsAndLikes]]

  def getActiveOpinion(opinionId: Long): F[Option[Opinion]]

  def update(opinion: Opinion): F[Boolean]

  def markDeleted(opinionId: Long): F[Boolean]

  def listForPlace(placeId: Long, pagingRequest: PagingRequest): F[PaginatedResult[OpinionWithTagsAndLikes]]

  def listAll(queryParameters: QueryParameters, pagingRequest: PagingRequest): F[PaginatedResult[OpinionWithTagsAndLikes]]
}
