package com.pwos.api.domain

import com.pwos.api.domain.places.Place


sealed trait HelloMountainsError extends Product with Serializable {
  def message: String
}


sealed trait UserValidationError extends HelloMountainsError

case object UserNotFoundError extends UserValidationError {
  override def message: String = "User does not exist."
}

case class IncorrectEmailError(incorrectEmail: String) extends UserValidationError {
  override def message: String = s"Provided email: $incorrectEmail is incorrect."
}

case class IncorrectUserNameError(incorrectUserName: String) extends UserValidationError {
  override def message: String =
    s"""Provided email: $incorrectUserName is incorrect.
       |It should be at least 3 character long.""".stripMargin
}

case object IncorrectPasswordError extends UserValidationError {
  override def message: String =
    """Provided password is incorrect.
    "|Password should have at least one capital letter and digit."""
}

case class UserWithSameNameAlreadyExistsError(name: String) extends UserValidationError {
  override def message: String = s"User with name: $name already exists."
}

case class UserWithSameEmailAlreadyExistsError(email: String) extends UserValidationError {
  override def message: String = s"User with email: $email already exists."
}


sealed trait PlaceValidationError extends HelloMountainsError

case object PlaceNotFoundError extends PlaceValidationError {
  override def message: String = "Place does not exist."
}

case class PlaceAlreadyExistsError(place: Place) extends PlaceValidationError {
  override def message: String = s"""Place with name ${place.name} already exists."""
}
