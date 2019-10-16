package com.pwos.api.infrastructure.dao.slick.opinions

import cats.implicits._
import com.github.tototoshi.slick.MySQLJodaSupport._
import com.pwos.api.PaginatedResult
import com.pwos.api.domain.PagingRequest
import com.pwos.api.domain.QueryParameters
import com.pwos.api.domain.opinions.Opinion
import com.pwos.api.domain.opinions.OpinionDAOAlgebra
import com.pwos.api.domain.opinions.tags.TagCategory
import com.pwos.api.domain.opinions.tags.{Tag => HmTag}
import com.pwos.api.infrastructure.dao.slick.SlickImplicits._
import slick.dbio.DBIO
import slick.jdbc.GetResult
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.PositionedResult
import slick.lifted.TableQuery

import scala.concurrent.ExecutionContext
import scala.language.postfixOps


class SlickOpinionDAOInterpreter(implicit ec: ExecutionContext) extends OpinionDAOAlgebra[DBIO] {

  private val opinions: TableQuery[OpinionTable] = TableQuery[OpinionTable]
  private val opinionsTags: TableQuery[OpinionTagTable] = TableQuery[OpinionTagTable]
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
    val activeOpinionClause: String = if (allowBlocked) {
      ""
    } else {
      "and OPINION.BLOCKED = 0"
    }

    val queryString =
      s"""
          select
            OPINION.ID, OPINION.PLACE_ID, OPINION.AUTHOR_ID, OPINION.BODY, OPINION.REFERENCE_DATE, OPINION.LAST_MODIFIED, OPINION.CREATION_DATE, OPINION.BLOCKED, OPINION.DELETED,
            TAG.ID, TAG.NAME, TAG.TAG_CATEGORY, TAG.ENABLED,
            OPINION_LIKE.OPINION_ID, OPINION_LIKE.USER_ID
          from OPINION
          left join OPINION_TAG on OPINION_TAG.OPINION_ID = OPINION.ID
          left join TAG on TAG.ID = OPINION_TAG.TAG_ID
          left join OPINION_LIKE on OPINION_LIKE.OPINION_ID = OPINION.ID
          where OPINION.ID = $opinionId and OPINION.DELETED = 0
          $activeOpinionClause
       """


    val opinionWithJoinsDBIO: DBIO[Vector[(Opinion, Option[HmTag], Option[OpinionLike])]] = sql"#$queryString".as[(Opinion, Option[HmTag], Option[OpinionLike])] {
      GetResult { r: PositionedResult =>
        (OpinionSlickMapper(r), TagSlickMapper(r), OpinionLikeSlickMapper(r))
      }
    }

    opinionWithJoinsDBIO map { opinionWithJoins =>
      aggregateOpinions(opinionWithJoins).headOption
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
    val opinionsWithJoinsQuery: DBIO[Vector[(Opinion, Option[HmTag], Option[OpinionLike])]] = opinionsWithJoins(placeId.some)

    for {
      opinionsResult <- opinionsWithJoinsQuery.map(aggregateOpinions)
      pagedResult = opinionsResult.paged(pagingRequest)
      totalCount = opinionsResult.length
    } yield {
      PaginatedResult.build(pagedResult, totalCount, pagingRequest)
    }
  }


  override def listAll(queryParameters: QueryParameters, pagingRequest: PagingRequest): DBIO[PaginatedResult[(Opinion, List[String], List[Long])]] = {
    val opinionsWithJoinsQuery: DBIO[Vector[(Opinion, Option[HmTag], Option[OpinionLike])]] = opinionsWithJoins(None)

    val onlyBlocked: Boolean = queryParameters.filterBy flatMap { filters: Map[String, String] =>
      filters.get("blocked") map {
        case "true" => true
        case _ => false
      }
    } getOrElse false

    val filtered: DBIO[Vector[(Opinion, Option[HmTag], Option[OpinionLike])]] = opinionsWithJoinsQuery map (_.filter { case (opinion, _, _) =>
      opinion.blocked === onlyBlocked
    })

    for {
      opinionsResult <- filtered.map(aggregateOpinions)
      pagedResult = opinionsResult.paged(pagingRequest)
      totalCount = opinionsResult.length
    } yield {
      PaginatedResult.build(pagedResult, totalCount, pagingRequest)
    }
  }

  private def opinionsWithJoins(maybePlaceId: Option[Long]): DBIO[Vector[(Opinion, Option[HmTag], Option[OpinionLike])]] = {

    val maybePlaceIdWhereClause: String = maybePlaceId map { placeId =>
      s"and OPINION.BLOCKED = 0 and OPINION.PLACE_ID = $placeId"
    } getOrElse {
      ""
    }

    val queryString =
      s"""
          select
            OPINION.ID, OPINION.PLACE_ID, OPINION.AUTHOR_ID, OPINION.BODY, OPINION.REFERENCE_DATE, OPINION.LAST_MODIFIED, OPINION.CREATION_DATE, OPINION.BLOCKED, OPINION.DELETED,
            TAG.ID, TAG.NAME, TAG.TAG_CATEGORY, TAG.ENABLED,
            OPINION_LIKE.OPINION_ID, OPINION_LIKE.USER_ID
          from OPINION
          left join OPINION_TAG on OPINION_TAG.OPINION_ID = OPINION.ID
          left join TAG on TAG.ID = OPINION_TAG.TAG_ID
          left join OPINION_LIKE on OPINION_LIKE.OPINION_ID = OPINION.ID
          where OPINION.DELETED = 0
          $maybePlaceIdWhereClause
          order by OPINION.ID desc
       """

    sql"#$queryString".as[(Opinion, Option[HmTag], Option[OpinionLike])] {
      GetResult { r: PositionedResult =>
        (OpinionSlickMapper(r), TagSlickMapper(r), OpinionLikeSlickMapper(r))
      }
    }
  }

  private def aggregateOpinions(rows: Seq[(Opinion, Option[HmTag], Option[OpinionLike])]): List[(Opinion, List[String], List[Long])] = {
    val notSorted = rows groupBy (_._1) map { case (opinion, seq) =>
      (opinion, seq.flatMap(_._2).map(_.name).toList, seq.flatMap(_._3).map(_.userId).toList)
    } toList

    notSorted.sortBy(_._1.id.get)(Ordering[Long].reverse)
  }

  private def OpinionSlickMapper(r: PositionedResult): Opinion = {
    Opinion(
      id = r.nextLongOption,
      placeId = r.nextLong,
      authorId = r.nextLong,
      body = r.nextStringOption,
      referenceDate = r.<<,
      lastModified = r.<<,
      creationDate = r.<<,
      blocked = r.nextBoolean,
      deleted = r.nextBoolean,
    )
  }

  private def TagSlickMapper(r: PositionedResult): Option[HmTag] = {
    val maybeTagId: Option[Long] = r.nextLongOption
    val maybeTagName: Option[String] = r.nextStringOption
    val maybeTagCategoryName: Option[String] = r.nextStringOption
    val maybeEnabled: Option[Boolean] = r.nextBooleanOption

    for {
      tagId <- maybeTagId
      tagName <- maybeTagName
      tagCategoryName <- maybeTagCategoryName
      enabled <- maybeEnabled
    } yield HmTag(
      id = Some(tagId),
      name = tagName,
      tagCategory = TagCategory.withName(tagCategoryName),
      enabled = enabled
    )
  }

  private def OpinionLikeSlickMapper(r: PositionedResult): Option[OpinionLike] = {
    val maybeOpinionId: Option[Long] = r.nextLongOption
    val maybeUserId: Option[Long] = r.nextLongOption

    for {
      opinionId <- maybeOpinionId
      userId <- maybeUserId
    } yield OpinionLike(opinionId = opinionId, userId = userId)
  }


}

object SlickOpinionDAOInterpreter {
  def apply(implicit ec: ExecutionContext): SlickOpinionDAOInterpreter =
    new SlickOpinionDAOInterpreter()
}
