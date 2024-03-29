package com.pwos.api.domain.opinions.tags

trait TagDAOAlgebra[F[_]] {

  def list(active: Boolean): F[List[Tag]]

  def create(tag: Tag): F[Tag]

  def update(tag: Tag): F[Boolean]

  def get(tagId: Long): F[Option[Tag]]

  def findByName(tagName: String): F[Option[Tag]]

}
