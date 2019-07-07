package com.pwos.api.domain

import com.pwos.api.domain.places.Place


sealed trait ValidationError extends Product with Serializable
case object PlaceNotFoundError extends ValidationError
case class PlaceAlreadyExistsError(place: Place) extends ValidationError
