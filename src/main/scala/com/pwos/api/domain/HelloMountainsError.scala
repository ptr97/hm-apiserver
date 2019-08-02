package com.pwos.api.domain

import com.pwos.api.domain.places.Place


sealed trait HelloMountainsError extends Product with Serializable {
  def message: String
}


sealed trait ValidationError extends HelloMountainsError

case object PlaceNotFoundError extends ValidationError {
  override def message: String = "Place does not exist"
}

case class PlaceAlreadyExistsError(place: Place) extends ValidationError {
  override def message: String = s"""Place with name ${place.name} already exists"""
}
