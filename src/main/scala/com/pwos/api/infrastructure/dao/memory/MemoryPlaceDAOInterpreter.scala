package com.pwos.api.infrastructure.dao.memory

import cats.Id
import com.pwos.api.domain.places.Place
import com.pwos.api.domain.places.PlaceDAOAlgebra


final class MemoryPlaceDAOInterpreter extends PlaceDAOAlgebra[Id] {

  private var places: List[Place] = List.empty

  private var placeId: Long = 1

  def getLastId: Long = placeId

  override def create(place: Place): Id[Place] = {
    val placeWithId = place.copy(id = Some(placeId))
    this.placeId += 1
    this.places = placeWithId :: this.places
    placeWithId
  }

  override def get(id: Long): Id[Option[Place]] = {
    this.places.find(_.id == Option(id))
  }

  override def update(place: Place): Id[Option[Place]] = {
    for {
      found <- this.places.find(_.id == place.id)
      newList = place :: this.places.filterNot(_.id == found.id)
      _ = this.places = newList
      updated <- this.places.find(_.id == place.id)
    } yield updated
  }

  override def delete(id: Long): Id[Boolean] = {
    val success = for {
      found <- get(id)
      newList = this.places.filterNot(_.id == found.id)
      _ = this.places = newList
    } yield true
    success getOrElse false
  }

  override def all: Id[List[Place]] = this.places

  override def findByName(name: String): Id[Option[Place]] = {
    this.places.find(_.name == name)
  }
}

object MemoryPlaceDAOInterpreter {
  def apply(): MemoryPlaceDAOInterpreter = new MemoryPlaceDAOInterpreter()
}
