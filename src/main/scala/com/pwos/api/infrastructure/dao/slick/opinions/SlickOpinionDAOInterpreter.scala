package com.pwos.api.infrastructure.dao.slick.opinions

import com.pwos.api.PaginatedResult
import com.pwos.api.domain.PagingRequest
import com.pwos.api.domain.QueryParameters
import com.pwos.api.domain.opinions.Opinion
import com.pwos.api.domain.opinions.OpinionDAOAlgebra
import com.pwos.api.infrastructure.dao.slick.opinions.tags.TagTable
import com.pwos.api.infrastructure.dao.slick.users.UserTable
import slick.dbio.DBIO
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.ExecutionContext


class SlickOpinionDAOInterpreter(implicit ec: ExecutionContext) extends OpinionDAOAlgebra[DBIO] {

  private val opinions: TableQuery[OpinionTable] = TableQuery[OpinionTable]
  private val opinionsTags: TableQuery[OpinionTagTable] = TableQuery[OpinionTagTable]
  private val tags: TableQuery[TagTable] = TableQuery[TagTable]
  private val opinionLikes: TableQuery[OpinionLikeTable] = TableQuery[OpinionLikeTable]
  private val users: TableQuery[UserTable] = TableQuery[UserTable]

  override def create(opinion: Opinion): DBIO[Opinion] = ???

  override def addTags(opinionId: Long, tagsIds: List[Long]): DBIO[Boolean] = ???

  override def removeTags(opinionId: Long): DBIO[Boolean] = ???

  override def addLike(opinionId: Long, userId: Long): DBIO[Boolean] = ???

  override def removeLike(opinionId: Long, userId: Long): DBIO[Boolean] = ???

  override def get(opinionId: Long): DBIO[Option[(Opinion, List[String], List[Long])]] = ???

  override def update(opinion: Opinion): DBIO[Boolean] = ???

  override def markDeleted(opinionId: Long): DBIO[Boolean] = ???

  override def listForPlace(placeId: Long, pagingRequest: PagingRequest): DBIO[PaginatedResult[(Opinion, List[String], List[Long])]] = ???

  override def listAll(queryParameters: QueryParameters, pagingRequest: PagingRequest): DBIO[PaginatedResult[(Opinion, List[String], List[Long])]] = ???
}

object SlickOpinionDAOInterpreter {
  def apply(implicit ec: ExecutionContext): SlickOpinionDAOInterpreter =
    new SlickOpinionDAOInterpreter()
}
