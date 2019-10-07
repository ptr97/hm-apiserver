package com.pwos.api.domain

import com.pwos.api.domain.places.Place

object HelloMountainsError {

  sealed trait HelloMountainsError extends Product with Serializable {
    def message: String
  }


  sealed trait UserValidationError extends HelloMountainsError

  case object UserNotFoundError extends UserValidationError {
    override def message: String = "User does not exist."
  }

  case object IncorrectCredentials extends UserValidationError {
    override def message: String = "Provided credentials are invalid."
  }

  case class IncorrectEmailError(incorrectEmail: String) extends UserValidationError {
    override def message: String = s"Provided email: $incorrectEmail is incorrect email address."
  }

  case class IncorrectUserNameError(incorrectUserName: String) extends UserValidationError {
    override def message: String =
      s"""Provided username: $incorrectUserName is incorrect.
         |It should be at least 3 character long.""".stripMargin
  }

  case object IncorrectPasswordError extends UserValidationError {
    override def message: String =
      """Provided password is incorrect.
        |Password should have at least one capital letter and digit."""
  }

  case object PasswordsIncompatibleError extends UserValidationError {
    override def message: String = """Provided passwords are different."""
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


  sealed trait OpinionValidationError extends HelloMountainsError

  case object OpinionNotFoundError extends OpinionValidationError {
    override def message: String = "Opinion does not exist."
  }

  case object OpinionOwnershipError extends OpinionValidationError {
    override def message: String = "You cannot modify opinion which you do not own."
  }

  case object OpinionAlreadyLikedError extends OpinionValidationError {
    override def message: String = "You cannot like opinion twice."
  }

  case object OpinionWasNotLikedError extends OpinionValidationError {
    override def message: String = "You cannot unlike opinion which you do not like."
  }

  case object OpinionAlreadyReportedError extends OpinionValidationError {
    override def message: String = "You have already reported this Opinion."
  }

}
