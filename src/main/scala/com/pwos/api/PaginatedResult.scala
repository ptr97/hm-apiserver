package com.pwos.api

import com.pwos.api.domain.PagingRequest
import scala.collection.GenTraversableOnce


case class PaginatedResult[T](totalCount: Int, items: List[T], hasNextPage: Boolean) {

  def mapResult[U](f: T => U): PaginatedResult[U] = {
    this.copy(items = items.map(f))
  }

  def flatMapResult[U](f: T => GenTraversableOnce[U]): PaginatedResult[U] = {
    this.copy(items = items.flatMap(f))
  }

}

object PaginatedResult {
  def build[T](items: List[T], totalCount: Int, pagingRequest: PagingRequest): PaginatedResult[T] = {
    PaginatedResult(
      totalCount = totalCount,
      items = items,
      hasNextPage = totalCount > pagingRequest.offset + items.length
    )
  }
}
