package com.pwos.api.infrastructure.dao.memory

import cats.Id
import com.pwos.api.PaginatedResult
import com.pwos.api.domain.PagingRequest
import com.pwos.api.domain.QueryParameters
import com.pwos.api.domain.opinions.Opinion
import com.pwos.api.domain.opinions.OpinionDAOAlgebra


class MemoryOpinionDAOInterpreter extends OpinionDAOAlgebra[Id] {
  override def create(opinion: Opinion): Id[Opinion] = ???

  override def addTags(opinionId: Long, tagsIds: List[Long]): Id[Boolean] = ???

  override def removeTags(opinionId: Long): Id[Boolean] = ???

  override def addLike(opinionId: Long, userId: Long): Id[Boolean] = ???

  override def removeLike(opinionId: Long, userId: Long): Id[Boolean] = ???

  override def get(opinionId: Long): Id[Option[(Opinion, List[String], List[Long])]] = ???

  override def update(opinion: Opinion): Id[Boolean] = ???

  override def markDeleted(opinionId: Long): Id[Boolean] = ???

  override def listForPlace(placeId: Long, pagingRequest: PagingRequest): Id[PaginatedResult[(Opinion, List[String], List[Long])]] = ???

  override def listAll(queryParameters: QueryParameters, pagingRequest: PagingRequest): Id[PaginatedResult[(Opinion, List[String], List[Long])]] = ???
}

object MemoryOpinionDAOInterpreter {
  def apply(): MemoryOpinionDAOInterpreter =
    new MemoryOpinionDAOInterpreter()
}
