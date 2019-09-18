package com.pwos.api

import scala.collection.GenTraversableOnce


case class PaginatedResult[T](totalCount: Int, items: List[T], hasNextPage: Boolean) {

  def mapResult[U](f: T => U): PaginatedResult[U] = {
    this.copy(items = items.map(f))
  }

  def flatMapResult[U](f: T => GenTraversableOnce[U]): PaginatedResult[U] = {
    this.copy(items = items.flatMap(f))
  }

}
