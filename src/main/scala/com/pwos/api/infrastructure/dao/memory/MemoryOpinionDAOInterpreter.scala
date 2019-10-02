package com.pwos.api.infrastructure.dao.memory

import cats.Id
import com.pwos.api.PaginatedResult
import com.pwos.api.domain.PagingRequest
import com.pwos.api.domain.QueryParameters
import com.pwos.api.domain.opinions.Opinion
import com.pwos.api.domain.opinions.OpinionDAOAlgebra


class MemoryOpinionDAOInterpreter extends OpinionDAOAlgebra[Id] {

  override def create(opinion: Opinion): Id[Opinion] = ???

  override def get(opinionId: Long): Id[Option[Opinion]] = ???

  override def update(opinion: Opinion): Id[Option[Opinion]] = ???

  override def markDeleted(opinionId: Long): Id[Boolean] = ???

  override def listForPlace(placeId: Long, pagingRequest: PagingRequest): Id[PaginatedResult[Opinion]] = ???

  override def listAll(queryParameters: QueryParameters, pagingRequest: PagingRequest): Id[PaginatedResult[Opinion]] = ???
}

object MemoryOpinionDAOInterpreter {
  def apply(): MemoryOpinionDAOInterpreter =
    new MemoryOpinionDAOInterpreter()
}
