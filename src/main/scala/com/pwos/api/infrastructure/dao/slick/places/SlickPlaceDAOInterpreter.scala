package com.pwos.api.infrastructure.dao.slick.places

import com.pwos.api.domain.places.Place
import com.pwos.api.domain.places.PlaceDAOAlgebra
import slick.dbio.DBIO
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.ExecutionContext


final class SlickPlaceDAOInterpreter(implicit ec: ExecutionContext) extends PlaceDAOAlgebra[DBIO] {

  private val places: TableQuery[PlaceTable] = TableQuery[PlaceTable]

  override def create(place: Place): DBIO[Place] = {
    val newPlaceId: DBIO[Long] = places returning places.map(_.id) += place
    newPlaceId.flatMap { id =>
      places.filter(_.id === id).result.head
    }
  }

  override def get(id: Long): DBIO[Option[Place]] = {
    places.filter(_.id === id).result.headOption
  }

  override def update(place: Place): DBIO[Option[Place]] = {
    places.filter(_.id === place.id).update(place).map { count =>
      if (count > 0) Some(place) else None
    }
  }

  override def delete(id: Long): DBIO[Boolean] = {
    places.filter(_.id === id).delete.map(_ == 1)
  }

  override lazy val all: DBIO[List[Place]] = {
    places.result.map(_.toList)
  }

  override def findByName(name: String): DBIO[Option[Place]] = {
    places.filter(_.name === name).result.headOption
  }
}


object SlickPlaceDAOInterpreter {
  def apply(implicit executionContext: ExecutionContext): SlickPlaceDAOInterpreter =
    new SlickPlaceDAOInterpreter()
}
