package com.pwos.api.domain.opinions

import com.pwos.api.PaginatedResult
import com.pwos.api.domain.PagingRequest
import com.pwos.api.domain.QueryParameters


trait OpinionDAOAlgebra[F[_]] {

  def create(opinion: Opinion): F[Opinion]

  def get(opinionId: Long): F[Option[Opinion]]

  def update(opinion: Opinion): F[Option[Opinion]]

  def markDeleted(opinionId: Long): F[Boolean]

  def listForPlace(placeId: Long, pagingRequest: PagingRequest): F[PaginatedResult[Opinion]]

  def listAll(queryParameters: QueryParameters, pagingRequest: PagingRequest): F[PaginatedResult[Opinion]]
}
