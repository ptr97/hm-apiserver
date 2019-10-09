package com.pwos.api.infrastructure.dao.slick.opinions.tags

import cats.implicits._
import com.pwos.api.domain.opinions.tags.{Tag => HmTag}
import com.pwos.api.domain.opinions.tags.TagDAOAlgebra
import slick.dbio.DBIO
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.ExecutionContext


class SlickTagDAOInterpreter(implicit ec: ExecutionContext) extends TagDAOAlgebra[DBIO] {

  val tags: TableQuery[TagTable] = TableQuery[TagTable]

  override def list(active: Boolean): DBIO[List[HmTag]] = {
    tags.filter(_.enabled === active).result.map(_.toList)
  }

  override def create(tag: HmTag): DBIO[HmTag] = {
    tags returning tags
      .map(_.id) into ((tag, id) => tag.copy(id = id.some)) += tag
  }

  override def update(tag: HmTag): DBIO[Boolean] = {
    tags
      .filter(_.id === tag.id)
      .update(tag)
      .map(_ === 1)
  }

  override def get(tagId: Long): DBIO[Option[HmTag]] = {
    tags.filter(_.id === tagId.some).result.headOption
  }

  override def findByName(tagName: String): DBIO[Option[HmTag]] = {
    tags.filter(_.name === tagName).result.headOption
  }
}
