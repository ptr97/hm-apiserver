package com.pwos.api.infrastructure.dao.memory

import cats.Id
import cats.implicits._
import com.pwos.api.domain.opinions.tags.Tag
import com.pwos.api.domain.opinions.tags.TagDAOAlgebra


class MemoryTagDAOInterpreter extends TagDAOAlgebra[Id] {

  private var tags: List[Tag] = List.empty

  private var tagId: Long = 1

  def getLastId: Long = tagId


  override def listActiveTags: Id[List[Tag]] = {
    listAllTags(active = true)
  }

  override def listAllTags(active: Boolean): Id[List[Tag]] = {
    this.tags.filter(_.enabled === active)
  }

  override def create(tag: Tag): Id[Tag] = {
    val tagWithId: Tag = tag.copy(id = Some(tagId))
    this.tagId += 1
    this.tags = tagWithId :: this.tags
    tagWithId
  }

  override def update(tag: Tag): Id[Boolean] = {
    (for {
      found <- this.tags.find(_.id === tag.id)
      newList = tag :: this.tags.filterNot(_.id === found.id)
      _ = this.tags = newList
    } yield true) getOrElse false
  }

  override def get(tagId: Long): Id[Option[Tag]] = {
    this.tags.find(_.id === tagId.some)
  }

  override def findByName(tagName: String): Id[Option[Tag]] = {
    this.tags.find(_.name === tagName)
  }

}
