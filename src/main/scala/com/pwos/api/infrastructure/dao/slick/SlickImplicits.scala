package com.pwos.api.infrastructure.dao.slick

import com.pwos.api.domain.PagingRequest
import slick.lifted.Query


object SlickImplicits {


  implicit class PagingQueryOps[E, U, C[_]](query: Query[E, U, C]) {
    def paged(pagingRequest: PagingRequest): Query[E, U, C] = {
      val withOffset: Query[E, U, C] = query.drop(pagingRequest.offset)
      pagingRequest.maybePageSize.map { pageSize =>
        withOffset.take(pageSize)
      } getOrElse withOffset
    }
  }

  implicit class PagingOps[T](items: List[T]) {
    def paged(pagingRequest: PagingRequest): List[T] = {
      val withOffset: List[T] = items.drop(pagingRequest.offset)
      pagingRequest.maybePageSize.map { pageSize =>
        withOffset.take(pageSize)
      } getOrElse withOffset
    }
  }

}

