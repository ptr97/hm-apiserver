package com.pwos.api.domain.users

import com.pwos.api.PaginatedResult
import com.pwos.api.domain.PagingRequest
import com.pwos.api.domain.QueryParameters


trait UserDAOAlgebra[F[_]] {

  def create(user: User): F[User]

  def get(id: Long): F[Option[User]]

  def get(ids: List[Long]): F[List[User]]

  def findByName(name: String): F[Option[User]]

  def findByEmail(email: String): F[Option[User]]

  def update(user: User): F[Option[User]]

  def markDeleted(id: Long): F[Boolean]

  def list(queryParameters: QueryParameters, pagingRequest: PagingRequest): F[PaginatedResult[User]]

}
