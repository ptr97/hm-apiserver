package com.pwos.api.domain.users


trait UserDAOAlgebra[F[_]] {

  def create(user: User): F[Option[User]]

  def get(id: Long): F[Option[User]]

  def findByName(name: String): F[Option[User]]

  def findByEmail(email: String): F[Option[User]]

  def update(user: User): F[Option[User]]

  def delete(id: Long): F[Boolean]

  def all: F[List[User]]

}
