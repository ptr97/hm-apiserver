package com.pwos.api.infrastructure.dao.slick

import com.pwos.api.domain.places.{Place, PlaceDAOAlgebra}
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext


final class SlickPlaceDAOInterpreter(implicit ec: ExecutionContext) extends PlaceDAOAlgebra[DBIO] {
  override def create(place: Place): DBIO[Place] = ???

  override def get(id: Long): DBIO[Option[Place]] = ???

  override def update(place: Place): DBIO[Option[Place]] = ???

  override def delete(id: Long): DBIO[Option[Place]] = ???
}

object SlickPlaceDAOInterpreter {
  def apply(implicit executionContext: ExecutionContext): SlickPlaceDAOInterpreter = new SlickPlaceDAOInterpreter()
}
