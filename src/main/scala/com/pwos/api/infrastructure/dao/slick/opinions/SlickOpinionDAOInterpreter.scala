package com.pwos.api.infrastructure.dao.slick.opinions

import cats.implicits._
import com.pwos.api.PaginatedResult
import com.pwos.api.domain.PagingRequest
import com.pwos.api.domain.QueryParameters
import com.pwos.api.domain.opinions.Opinion
import com.pwos.api.domain.opinions.OpinionDAOAlgebra
import com.pwos.api.domain.opinions.tags.{Tag => HmTag}
import com.pwos.api.infrastructure.dao.slick.opinions.tags.TagTable
import com.pwos.api.infrastructure.dao.slick.users.UserTable
import slick.dbio.DBIO
import slick.dbio.Effect
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.ExecutionContext


class SlickOpinionDAOInterpreter(implicit ec: ExecutionContext) extends OpinionDAOAlgebra[DBIO] {

  private val opinions: TableQuery[OpinionTable] = TableQuery[OpinionTable]
  private val opinionsTags: TableQuery[OpinionTagTable] = TableQuery[OpinionTagTable]
  private val tags: TableQuery[TagTable] = TableQuery[TagTable]
  private val opinionsLikes: TableQuery[OpinionLikeTable] = TableQuery[OpinionLikeTable]
  private val users: TableQuery[UserTable] = TableQuery[UserTable]


  override def create(opinion: Opinion): DBIO[Opinion] = {
    opinions returning opinions
      .map(_.id) into ((opinion, id) => opinion.copy(id = id.some)) += opinion
  }

  override def addTags(opinionId: Long, tagsIds: List[Long]): DBIO[Boolean] = {
    val listOfOpinionTags: List[OpinionTag] = tagsIds map { tagId =>
      OpinionTag(opinionId, tagId)
    }

    (opinionsTags ++= listOfOpinionTags).map(_.exists(_ === tagsIds.length))
  }

  override def removeTags(opinionId: Long): DBIO[Boolean] = {
    opinionsTags.filter(_.opinionId === opinionId).delete.map(_ >= 0)
  }

  override def addLike(opinionId: Long, userId: Long): DBIO[Boolean] = {
    (opinionsLikes += OpinionLike(opinionId, userId)).map(_ === 1)
  }

  override def removeLike(opinionId: Long, userId: Long): DBIO[Boolean] = {
    opinionsLikes
      .filter(_.opinionId === opinionId)
      .filter(_.userId === userId)
      .delete
      .map(_ === 1)
  }

  override def get(opinionId: Long): DBIO[Option[(Opinion, List[String], List[Long])]] = {

    val opinionsWithTagsQuery: Query[(OpinionTable, Rep[Option[TagTable]]), (Opinion, Option[HmTag]), Seq] = for {
      (opinionTable, maybeOpinionTagTable) <- opinions filter (_.id === opinionId) joinLeft opinionsTags on (_.id === _.opinionId)
      tagId = maybeOpinionTagTable.map(_.tagId)
      (_, tagTable) <- opinionsTags filter (_.tagId === tagId) joinLeft tags on (_.tagId === _.id)
    } yield (opinionTable, tagTable)

    val opinionWithTagsDBIO: DBIO[Option[(Opinion, Seq[HmTag])]] = opinionsWithTagsQuery.result map { rows =>
      val (opinions: Seq[Opinion], tags: Seq[Option[HmTag]]) = rows.unzip
      opinions.headOption map { opinion =>
        (opinion, tags.flatten)
      }
    }

    val opinionsWithLikesQuery: Query[(OpinionTable, Rep[Option[String]]), (Opinion, Option[String]), Seq] = for {
      (opinionTable, opinionLikesTable) <- opinions filter (_.id === opinionId) joinLeft opinionsLikes on (_.id === _.opinionId)
      userId = opinionLikesTable.map(_.userId)
      (_, usersTable) <- opinionsLikes filter (_.userId === userId) joinLeft users on (_.userId === _.id)
    } yield (opinionTable, usersTable.map(_.username))

    val opinionWithLikesDBIO: DBIO[Option[(Opinion, Seq[String])] = opinionsWithLikesQuery.result map { rows =>
      val (opinions: Seq[Opinion], userNames: Seq[Option[String]]) = rows.unzip
      opinions.headOption map { opinion =>
        (opinion, userNames.flatten)
      }
    }

    val allData: DBIOAction[Option[(Opinion, Seq[HmTag], Seq[String])], NoStream, Effect.All] = for {
      withTags <- opinionWithTagsDBIO
      withLikes <- opinionWithLikesDBIO
    } yield {
      for {
        (_, tags) <- withTags
        (opinion, likes) <- withLikes
      } yield (opinion, tags, likes)
    }

    ???
  }

  override def update(opinion: Opinion): DBIO[Boolean] = ???

  override def markDeleted(opinionId: Long): DBIO[Boolean] = {
    opinions
      .filter(_.id === opinionId)
      .map(_.deleted)
      .update(true)
      .map(_ === 1)
  }

  override def listForPlace(placeId: Long, pagingRequest: PagingRequest): DBIO[PaginatedResult[(Opinion, List[String], List[Long])]] = ???

  override def listAll(queryParameters: QueryParameters, pagingRequest: PagingRequest): DBIO[PaginatedResult[(Opinion, List[String], List[Long])]] = ???
}

object SlickOpinionDAOInterpreter {
  def apply(implicit ec: ExecutionContext): SlickOpinionDAOInterpreter =
    new SlickOpinionDAOInterpreter()
}
