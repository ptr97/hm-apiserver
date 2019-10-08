package com.pwos.api.infrastructure.dao.slick.opinions

import cats.implicits._
import com.github.tototoshi.slick.MySQLJodaSupport._
import com.pwos.api.PaginatedResult
import com.pwos.api.domain.PagingRequest
import com.pwos.api.domain.QueryParameters
import com.pwos.api.domain.opinions.Opinion
import com.pwos.api.domain.opinions.OpinionDAOAlgebra
import com.pwos.api.domain.opinions.tags.{Tag => HmTag}
import com.pwos.api.infrastructure.dao.slick.SlickImplicits._
import com.pwos.api.infrastructure.dao.slick.opinions.tags.TagTable
import slick.dbio.DBIO
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.ExecutionContext
import scala.language.postfixOps


class SlickOpinionDAOInterpreter(implicit ec: ExecutionContext) extends OpinionDAOAlgebra[DBIO] {

  private val opinions: TableQuery[OpinionTable] = TableQuery[OpinionTable]
  private val opinionsTags: TableQuery[OpinionTagTable] = TableQuery[OpinionTagTable]
  private val tags: TableQuery[TagTable] = TableQuery[TagTable]
  private val opinionsLikes: TableQuery[OpinionLikeTable] = TableQuery[OpinionLikeTable]


  private lazy val notDeletedOpinions = opinions filter (_.deleted === false)
  private lazy val activeOpinions = notDeletedOpinions filter (_.blocked === false)


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

  override def getOpinionView(opinionId: Long): DBIO[Option[(Opinion, List[String], List[Long])]] = {
    getOpinion(opinionId, allowBlocked = true)
  }

  override def getActiveOpinionView(opinionId: Long): DBIO[Option[(Opinion, List[String], List[Long])]] = {
    getOpinion(opinionId, allowBlocked = false)
  }

  override def getActiveOpinion(opinionId: Long): DBIO[Option[Opinion]] = {
    activeOpinions.filter(_.id === opinionId).result.headOption
  }

  private def getOpinion(opinionId: Long, allowBlocked: Boolean): DBIO[Option[(Opinion, List[String], List[Long])]] = {
    val opinionsBaseSet = if (allowBlocked) {
      notDeletedOpinions
    } else {
      activeOpinions
    }

    val opinionsWithJoins: Query[(OpinionTable, Rep[Option[String]], Rep[Option[Long]]), (Opinion, Option[String], Option[Long]), Seq] = for {
      (_, maybeOpinionTagTable) <- opinionsBaseSet filter (_.id === opinionId) joinLeft opinionsTags on (_.id === _.opinionId)
      tagId = maybeOpinionTagTable.map(_.tagId)
      (_, tagTable) <- opinionsTags filter (_.tagId === tagId) joinLeft tags on (_.tagId === _.id)
      (opinionTable, opinionLikesTable) <- opinionsBaseSet filter (_.id === opinionId) joinLeft opinionsLikes on (_.id === _.opinionId)
    } yield (opinionTable, tagTable.map(_.name), opinionLikesTable.map(_.userId))

    opinionsWithJoins.result map { rows =>
      val (opinions: Seq[Opinion], tagsNames: Seq[Option[String]], likedBy: Seq[Option[Long]]) = rows.unzip3
      opinions.headOption map { opinion =>
        (opinion, tagsNames.flatten.toList, likedBy.flatten.toList)
      }
    }
  }

  override def update(opinion: Opinion): DBIO[Boolean] = {
    activeOpinions
      .filter(_.id === opinion.id)
      .update(opinion)
      .map(_ === 1)
  }

  override def markDeleted(opinionId: Long): DBIO[Boolean] = {
    activeOpinions
      .filter(_.id === opinionId)
      .map(_.deleted)
      .update(true)
      .map(_ === 1)
  }

  override def listForPlace(placeId: Long, pagingRequest: PagingRequest): DBIO[PaginatedResult[(Opinion, List[String], List[Long])]] = {
    val opinionsWithJoinsQuery = opinionsWithJoins(placeId.some)

    for {
      opinionsResult <- opinionsWithJoinsQuery.paged(pagingRequest).result.map(aggregateOpinions)
      totalCount <- opinionsWithJoinsQuery.length.result
    } yield {
      PaginatedResult.build(opinionsResult, totalCount, pagingRequest)
    }
  }


  override def listAll(queryParameters: QueryParameters, pagingRequest: PagingRequest): DBIO[PaginatedResult[(Opinion, List[String], List[Long])]] = {
    val opinionsWithJoinsQuery = opinionsWithJoins(None)

    for {
      opinionsResult <- opinionsWithJoinsQuery.paged(pagingRequest).result.map(aggregateOpinions)
      totalCount <- opinionsWithJoinsQuery.length.result
    } yield {
      PaginatedResult.build(opinionsResult, totalCount, pagingRequest)
    }
  }

  private def opinionsWithJoins(maybePlaceId: Option[Long]): Query[(OpinionTable, Rep[Option[TagTable]], Rep[Option[OpinionLikeTable]]), (Opinion, Option[HmTag], Option[OpinionLike]), Seq] = {
    val opinionsForPlaceOrAll: Query[OpinionTable, Opinion, Seq] = maybePlaceId map { placeId =>
      activeOpinions filter (_.placeId === placeId)
    } getOrElse notDeletedOpinions

    val queryWithJoins = for {
      (_, maybeOpinionTagTable) <- opinionsForPlaceOrAll joinLeft opinionsTags on (_.id === _.opinionId)
      tagId = maybeOpinionTagTable.map(_.tagId)
      (_, tagTable) <- opinionsTags filter (_.tagId === tagId) joinLeft tags on (_.tagId === _.id)
      (opinionTable, opinionLikesTable) <- opinionsForPlaceOrAll joinLeft opinionsLikes on (_.id === _.opinionId)
    } yield (opinionTable, tagTable, opinionLikesTable)

    queryWithJoins.sortBy(_._1.referenceDate.desc)
  }

  private def aggregateOpinions(rows: Seq[(Opinion, Option[HmTag], Option[OpinionLike])]): List[(Opinion, List[String], List[Long])] = {
    rows groupBy(_._1) map { case (opinion, seq) =>
      (opinion, seq.flatMap(_._2).map(_.name).toList, seq.flatMap(_._3).map(_.userId).toList)
    } toList
  }

}

object SlickOpinionDAOInterpreter {
  def apply(implicit ec: ExecutionContext): SlickOpinionDAOInterpreter =
    new SlickOpinionDAOInterpreter()
}
