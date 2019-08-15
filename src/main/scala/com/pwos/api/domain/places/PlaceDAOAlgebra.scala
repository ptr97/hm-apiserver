package com.pwos.api.domain.places


trait PlaceDAOAlgebra[F[_]] {

  def create(place: Place): F[Place]

  def get(id: Long): F[Option[Place]]

  def update(place: Place): F[Option[Place]]

  def delete(id: Long): F[Boolean]

  def all: F[List[Place]]

  def findByName(name: String): F[Option[Place]]

}
