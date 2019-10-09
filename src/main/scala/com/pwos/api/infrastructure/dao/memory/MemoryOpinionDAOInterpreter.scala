package com.pwos.api.infrastructure.dao.memory

import cats.Id
import cats.implicits._
import com.pwos.api.PaginatedResult
import com.pwos.api.domain.PagingRequest
import com.pwos.api.domain.QueryParameters
import com.pwos.api.domain.opinions.Opinion
import com.pwos.api.domain.opinions.OpinionDAOAlgebra
import com.pwos.api.domain.opinions.tags.Tag
import com.pwos.api.infrastructure.dao.slick.opinions.OpinionLike
import com.pwos.api.infrastructure.dao.slick.opinions.OpinionTag


class MemoryOpinionDAOInterpreter(tags: List[Tag]) extends OpinionDAOAlgebra[Id] {

  private var opinions: List[Opinion] = List.empty
  private var opinionIdAutoIncrement: Long = 1

  private var opinionsTags: List[OpinionTag] = List.empty
  private var opinionsLikes: List[OpinionLike] = List.empty

  def getLastOpinionId: Long = opinionIdAutoIncrement

  private def notDeletedOpinions: List[Opinion] = {
    this.opinions.filter(_.deleted === false)
  }

  private def activeOpinions: List[Opinion] = {
    notDeletedOpinions.filter(_.blocked === false)
  }

  override def create(opinion: Opinion): Id[Opinion] = {
    val opinionWithId: Opinion = opinion.copy(id = Some(opinionIdAutoIncrement))
    this.opinionIdAutoIncrement += 1
    this.opinions = opinionWithId :: this.opinions
    opinionWithId
  }

  override def addTags(opinionId: Long, tagsIds: List[Long]): Id[Boolean] = {
    val opinionTagsValues: List[OpinionTag] = tagsIds.map { tagId =>
      OpinionTag(opinionId, tagId)
    }

    this.opinionsTags = opinionTagsValues ::: this.opinionsTags
    true
  }

  override def removeTags(opinionId: Long): Id[Boolean] = {
    this.opinionsTags = this.opinionsTags.filterNot(_.opinionId == opinionId)
    true
  }

  override def addLike(opinionId: Long, userId: Long): Id[Boolean] = {
    this.opinionsLikes = OpinionLike(opinionId, userId) :: this.opinionsLikes
    true
  }

  override def removeLike(opinionId: Long, userId: Long): Id[Boolean] = {
    this.opinionsLikes = this.opinionsLikes.filterNot { opinionLike =>
      opinionLike.userId == userId && opinionLike.opinionId == opinionId
    }
    true
  }

  override def getOpinionView(opinionId: Long): Id[Option[(Opinion, List[String], List[Long])]] = {
    getOpinion(opinionId, allowBlocked = true)
  }

  override def getActiveOpinionView(opinionId: Long): Id[Option[(Opinion, List[String], List[Long])]] = {
    getOpinion(opinionId, allowBlocked = false)
  }

  override def getActiveOpinion(opinionId: Long): Id[Option[Opinion]] = {
    activeOpinions.find(_.id === opinionId.some)
  }

  private def getOpinion(opinionId: Long, allowBlocked: Boolean): Id[Option[(Opinion, List[String], List[Long])]] = {
    val opinionBaseSet: List[Opinion] = if (allowBlocked) {
      notDeletedOpinions
    } else {
      activeOpinions
    }

    val maybeOpinion: Option[Opinion] = opinionBaseSet.find(_.id === opinionId.some)
    val tagsIds: List[Long] = this.opinionsTags.filter(_.opinionId === opinionId).map(_.tagId)
    val tagsNames: List[String] = this.tags.filter(tag => tagsIds.contains(tag.id.get)).map(_.name)

    val likesUserIds: List[Long] = this.opinionsLikes.filter(_.opinionId == opinionId).map(_.userId)

    maybeOpinion map { opinion =>
      (opinion, tagsNames, likesUserIds)
    }

  }

  override def update(opinion: Opinion): Id[Boolean] = {
    val updatedOpinion: Option[Opinion] = for {
      found <- activeOpinions.find(_.id === opinion.id)
      newList = opinion :: this.opinions.filterNot(_.id === found.id)
      _ = this.opinions = newList
      updated <- this.opinions.find(_.id === opinion.id)
    } yield updated

    updatedOpinion.isDefined
  }

  override def markDeleted(opinionId: Long): Id[Boolean] = {
    (for {
      found <- getActiveOpinion(opinionId)
      deletedOpinion = found.copy(deleted = true)
      updateResult <- update(deletedOpinion).some
    } yield updateResult) getOrElse false
  }

  override def listForPlace(placeId: Long, pagingRequest: PagingRequest): Id[PaginatedResult[(Opinion, List[String], List[Long])]] = {
    val opinionsList: List[Opinion] = activeOpinions.filter(_.placeId === placeId)

    val items: List[(Opinion, List[String], List[Long])] = opinionsList map { opinion =>
      val tagsIds: List[Long] = this.opinionsTags.filter(_.opinionId === opinion.id.get).map(_.tagId)
      val tagsNames: List[String] = this.tags.filter(tag => tagsIds.contains(tag.id.get)).map(_.name)

      val likesUserIds: List[Long] = this.opinionsLikes.filter(_.opinionId == opinion.id.get).map(_.userId)

      (opinion, tagsNames, likesUserIds)
    }

    PaginatedResult.build(items, items.length, pagingRequest)
  }

  override def listAll(queryParameters: QueryParameters, pagingRequest: PagingRequest): Id[PaginatedResult[(Opinion, List[String], List[Long])]] = {

    val filteredOpinions = notDeletedOpinions

    val items: List[(Opinion, List[String], List[Long])] = filteredOpinions map { opinion =>
      val tagsIds: List[Long] = this.opinionsTags.filter(_.opinionId === opinion.id.get).map(_.tagId)
      val tagsNames: List[String] = this.tags.filter(tag => tagsIds.contains(tag.id.get)).map(_.name)

      val likesUserIds: List[Long] = this.opinionsLikes.filter(_.opinionId == opinion.id.get).map(_.userId)

      (opinion, tagsNames, likesUserIds)
    }

    PaginatedResult.build(items, items.length, pagingRequest)
  }

}

object MemoryOpinionDAOInterpreter {
  def apply(tags: List[Tag]): MemoryOpinionDAOInterpreter =
    new MemoryOpinionDAOInterpreter(tags)
}
