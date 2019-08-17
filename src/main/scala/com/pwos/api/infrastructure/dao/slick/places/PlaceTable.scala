package com.pwos.api.infrastructure.dao.slick.places

import com.pwos.api.domain.places.Place
import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape


final class PlaceTable(tag: Tag) extends Table[Place](tag, "PLACE") {

  def id: Rep[Long]          = column[Long]("ID", O.PrimaryKey, O.AutoInc)
  def name: Rep[String]      = column[String]("NAME")
  def latitude: Rep[Double]  = column[Double]("LATITUDE")
  def longitude: Rep[Double] = column[Double]("LONGITUDE")
  def elevation: Rep[Double] = column[Double]("ELEVATION")

  override def * : ProvenShape[Place] = (
    name,
    latitude,
    longitude,
    elevation,
    id.?
  ) <> (Place.tupled, Place.unapply)
}
