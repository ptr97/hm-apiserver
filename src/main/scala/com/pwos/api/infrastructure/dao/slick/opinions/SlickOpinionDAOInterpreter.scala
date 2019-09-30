package com.pwos.api.infrastructure.dao.slick.opinions

import com.pwos.api.PaginatedResult
import com.pwos.api.domain.PagingRequest
import com.pwos.api.domain.QueryParameters
import com.pwos.api.domain.opinions.Opinion
import com.pwos.api.domain.opinions.OpinionDAOAlgebra
import slick.dbio.DBIO
import slick.dbio.Effect
import slick.jdbc
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery
import slick.sql.FixedSqlStreamingAction

import scala.concurrent.ExecutionContext


class SlickOpinionDAOInterpreter(implicit ec: ExecutionContext) extends OpinionDAOAlgebra[DBIO] {

  private val opinions: TableQuery[OpinionTable] = TableQuery[OpinionTable]
  private val opinionsTags: TableQuery[OpinionTagTable] = TableQuery[OpinionTagTable]
  private val opinionLikes: TableQuery[OpinionLikeTable] = TableQuery[OpinionLikeTable]


  override def create(opinion: Opinion): DBIO[Opinion] = {
    val opinionDTO: OpinionDTO = OpinionDTO.fromOpinion(opinion)
    val tagsIds: List[Long] = opinion.tags.flatMap(_.id)

    val newOpinionId: DBIO[Long] = opinions returning opinions.map(_.id) += opinionDTO

    newOpinionId flatMap { opinionId =>
      val opinionTags: List[OpinionTag] = tagsIds.map { tagId => OpinionTag(opinionId, tagId) }
      val insertOpinionTagsResult: DBIO[Option[Int]] = opinionsTags ++= opinionTags
      insertOpinionTagsResult map { _ =>
        opinion
      }
    }

  }

  override def get(uuid: String): DBIO[Option[Opinion]] = {

    val opinionsWithLeftJoinTags: Query[(OpinionTable, Rep[Option[OpinionTagTable]]), (OpinionDTO, Option[OpinionTag]), Seq] = opinions
//      .filter(_.deleted === false)
//      .filter(_.blocked === false)
      .filter(_.uuid === uuid)
      .joinLeft(opinionsTags)
      .on(_.id === _.opinionId)

    val opinionsWithLikesJoin: Query[((OpinionTable, jdbc.MySQLProfile.api.Rep[Option[OpinionTagTable]]), Rep[Option[OpinionLikeTable]]), ((OpinionDTO, Option[OpinionTag]), Option[OpinionLike]), Seq] = opinionsWithLeftJoinTags
      .joinLeft(opinionLikes)
      .on(_._1.id === _.opinionId)

    val res: FixedSqlStreamingAction[Seq[(OpinionDTO, Option[OpinionTag])], (OpinionDTO, Option[OpinionTag]), Effect.Read] = opinionsWithLeftJoinTags.result

    val t: DBIOAction[(Option[OpinionDTO], Seq[OpinionTag]), NoStream, Effect.Read] = res.map { seq: Seq[(OpinionDTO, Option[OpinionTag])] =>
      val tags: Seq[OpinionTag] = seq.flatMap(_._2)
      val maybeOpinion: Option[OpinionDTO] = seq.map(_._1).headOption

      maybeOpinion -> tags
    }

//    t map { tuple =>
//      tuple._1.map { opinionDTO =>
//        opinionDTO.toOpinion(tuple._2.toList, List.empty)
//      }
//    }

    ???
  }

  override def update(opinion: Opinion): DBIO[Option[Opinion]] = {
    opinions
      .filter(_.uuid === opinion.uuid)

    ???
  }

  override def markDeleted(uuid: String): DBIO[Boolean] = {
    opinions
      .filter(_.uuid === uuid)
      .map(_.deleted)
      .update(true)
      .map(_ > 0)
  }

  override def listForPlace(placeId: Long, pagingRequest: PagingRequest): DBIO[PaginatedResult[Opinion]] = ???

  override def listAll(queryParameters: QueryParameters, pagingRequest: PagingRequest): DBIO[PaginatedResult[Opinion]] = ???
}

object SlickOpinionDAOInterpreter {
  def apply(implicit ec: ExecutionContext): SlickOpinionDAOInterpreter =
    new SlickOpinionDAOInterpreter()
}
