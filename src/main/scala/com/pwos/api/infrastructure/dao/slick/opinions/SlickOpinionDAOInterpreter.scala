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

  override def get(opinionId: Long): DBIO[Option[Opinion]] = {

    //    val opinionsWithLeftJoinTags: Query[(OpinionTable, Rep[Option[OpinionTagTable]]), (OpinionDTO, Option[OpinionTag]), Seq] = opinions
    ////      .filter(_.deleted === false)
    ////      .filter(_.blocked === false)
    //      .filter(_.uuid === uuid)
    //      .joinLeft(opinionsTags)
    //      .on(_.id === _.opinionId)
    //
    //    val opinionsWithLikesJoin: Query[((OpinionTable, jdbc.MySQLProfile.api.Rep[Option[OpinionTagTable]]), Rep[Option[OpinionLikeTable]]), ((OpinionDTO, Option[OpinionTag]), Option[OpinionLike]), Seq] = opinionsWithLeftJoinTags
    //      .joinLeft(opinionLikes)
    //      .on(_._1.id === _.opinionId)
    //
    //    val res: FixedSqlStreamingAction[Seq[(OpinionDTO, Option[OpinionTag])], (OpinionDTO, Option[OpinionTag]), Effect.Read] = opinionsWithLeftJoinTags.result
    //
    //    val t: DBIOAction[(Option[OpinionDTO], Seq[OpinionTag]), NoStream, Effect.Read] = res.map { seq: Seq[(OpinionDTO, Option[OpinionTag])] =>
    //      val tags: Seq[OpinionTag] = seq.flatMap(_._2)
    //      val maybeOpinion: Option[OpinionDTO] = seq.map(_._1).headOption
    //
    //      maybeOpinion -> tags
    //    }
    //
    ////    t map { tuple =>
    ////      tuple._1.map { opinionDTO =>
    ////        opinionDTO.toOpinion(tuple._2.toList, List.empty)
    ////      }
    ////    }

    // # 2



    // # 3
    for {
      maybeOpinionDTO <- opinions.filter(_.id === opinionId).result.headOption
      opinionTagsIds: List[Long] <- maybeOpinionDTO map { opinionDTO =>
        opinionsTags.filter(_.opinionId === opinionDTO.id).map(_.tagId).result.map(_.toList)
      } getOrElse {
        DBIO.successful(List.empty)
      }
      tags <- tags.filter(_.id inSet opinionTagsIds).result.map(_.toList)
      likesIds: List[Long] <- maybeOpinionDTO map { opinionDTO =>
        opinionLikes.filter(_.opinionId === opinionDTO.id).map(_.userId).result.map(_.toList)
      } getOrElse {
        DBIO.successful(List.empty)
      }
      likes: List[String] <- users.filter(_.id inSet likesIds).map(_.username).result.map(_.toList)
    } yield {
      maybeOpinionDTO map { opinionDTO =>
        opinionDTO.toOpinion(tags, likes)
      }
    }
  }

  override def update(opinion: Opinion): DBIO[Option[Opinion]] = {
    opinions
      .filter(_.id === opinion.id)

    ???
  }

  override def markDeleted(opinionId: Long): DBIO[Boolean] = {
    opinions
      .filter(_.id === opinionId)
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
