package com.pwos.api

package domain {

  case class QueryParameters(filterBy: Option[String], search: Option[String])

  object QueryParameters {
    val empty = QueryParameters(None, None)
  }

  case class PagingRequest(page: Int, pageSize: Int, sortBy: Option[String])

  object PagingRequest {
    val DefaultPageSize = 25
  }

}
