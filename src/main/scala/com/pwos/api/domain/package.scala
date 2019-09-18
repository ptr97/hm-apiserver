package com.pwos.api

import scala.language.postfixOps


package domain {

  case class QueryParameters(filterBy: Option[Map[String, String]], search: Option[String])

  object QueryParameters {
    def empty: QueryParameters = QueryParameters(None, None)

    def fromRequest(filterByPlain: Option[String], search: Option[String]): QueryParameters = {
      QueryParameters(filterBy = createFilterBy(filterByPlain), search = search)
    }

    private def createFilterBy(maybePlain: Option[String]): Option[Map[String, String]] = {
      maybePlain.map { plain =>
        plain.split(";") map { item =>
          item.split(":") toList
        } collect { case name :: value :: Nil if name.trim.nonEmpty =>
          name.trim -> value
        } toMap
      } filter (_.nonEmpty)
    }
  }

  case class PagingRequest(page: Int, maybePageSize: Option[Int], sortBy: Option[String], asc: Boolean = true) {
    def offset: Int = maybePageSize.map(pageSize => page * pageSize).getOrElse(0)
  }

  object PagingRequest {
    def empty: PagingRequest = PagingRequest(0, None, None)

    def fromRequest(maybePage: Option[Int], maybePageSize: Option[Int], sortByPlain: Option[String]): PagingRequest = {
      val (sortBy, order) = parsePlainSortBy(sortByPlain)
      PagingRequest(maybePage.getOrElse(0), maybePageSize, sortBy, order)
    }

    def parsePlainSortBy(maybeStr: Option[String]): (Option[String], Boolean) = {
      val emptyOrdering: (Option[String], Boolean) = (None, true)

      maybeStr.map { str =>
        str.split(":").toList.map(_.trim) match {
          case field :: ordering :: Nil if field.nonEmpty => (Some(field), sortToBoolean(ordering))
          case field :: _ if field.nonEmpty => (Some(field), true)
          case _ => emptyOrdering
        }
      } getOrElse emptyOrdering
    }

    private def sortToBoolean(str: String): Boolean = {
      str.toLowerCase match {
        case "asc" => true
        case "desc" => false
        case _ => true
      }
    }
  }

}
