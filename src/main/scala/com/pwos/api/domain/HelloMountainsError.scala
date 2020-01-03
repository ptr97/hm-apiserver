package com.pwos.api.domain

import com.pwos.api.domain.opinions.tags.Tag
import com.pwos.api.domain.places.Place


object HelloMountainsError {

  sealed trait HelloMountainsError extends Product with Serializable {
    def message: String
  }

  case object UnauthorizedError extends HelloMountainsError {
    override def message: String = "The supplied authentication is not authorized to access this resource."
  }

  case object RouteNotFoundError extends HelloMountainsError {
    override def message: String = "Bad request."
  }

  sealed trait PrivilegeError extends HelloMountainsError {
    override def message: String = "You do not have privileges to perform this action."
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
      s"Provided username: $incorrectUserName is incorrect. It should be at least 3 characters long."
  }

  case object IncorrectPasswordError extends UserValidationError {
    override def message: String =
      "Provided password is incorrect. Password should have at least one capital letter and digit."
  }

  case object PasswordsIncompatibleError extends UserValidationError {
    override def message: String = "Provided passwords are different."
  }

  case class UserWithSameNameAlreadyExistsError(name: String) extends UserValidationError {
    override def message: String = s"User with name: $name already exists."
  }

  case class UserWithSameEmailAlreadyExistsError(email: String) extends UserValidationError {
    override def message: String = s"User with email: $email already exists."
  }

  case object UserPrivilegeError extends UserValidationError with PrivilegeError


  sealed trait PlaceValidationError extends HelloMountainsError

  case object PlaceNotFoundError extends PlaceValidationError {
    override def message: String = "Place does not exist."
  }

  case class PlaceAlreadyExistsError(place: Place) extends PlaceValidationError {
    override def message: String = s"Place with name ${place.name} already exists."
  }

  case object PlacePrivilegeError extends PlaceValidationError with PrivilegeError


  sealed trait TagValidationError extends HelloMountainsError

  case object TagPrivilegeError extends TagValidationError with PrivilegeError

  case class TagAlreadyExistsError(tag: Tag) extends TagValidationError {
    override def message: String = s"Tag with name ${tag.name} already exists."
  }

  case object TagNotFoundError extends TagValidationError {
    override def message: String = "Tag does not exist."
  }


  sealed trait OpinionValidationError extends HelloMountainsError

  case object OpinionNotFoundError extends OpinionValidationError {
    override def message: String = "Opinion does not exist."
  }

  case object OpinionOwnershipError extends OpinionValidationError {
    override def message: String = "You cannot modify opinion which you do not own."
  }

  case object OpinionPrivilegeError extends OpinionValidationError with PrivilegeError


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
