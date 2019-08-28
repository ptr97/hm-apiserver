package com.pwos.api.domain.users

import com.pwos.api.domain.authentication.PasswordService.Password
import io.circe.Decoder
import io.circe.Encoder
import io.circe.generic.semiauto._


case class User(
                 userName: String,
                 email: String,
                 password: Password,
                 role: UserRole.Value = UserRole.User,
                 banned: Boolean = false,
                 deleted: Boolean = false,
                 id: Option[Long] = None
               ) {
  def toView: Option[UserView] = id.map { id => UserView(id, userName, email) }

  def buildUserInfo: Option[UserInfo] = id.map { id => UserInfo(id, userName, email, role, banned, deleted) }
}

object User {
  implicit val userRoleDecoder: Decoder[UserRole.Value] = Decoder.decodeEnumeration(UserRole)
  implicit val userRoleEncoder: Encoder[UserRole.Value] = Encoder.encodeEnumeration(UserRole)
  implicit val userDecoder: Decoder[User] = deriveDecoder
  implicit val userEncoder: Encoder[User] = deriveEncoder
}


object UserModels {

  case class CreateUserModel(userName: String, email: String, password: String, passwordCheck: String)

  case class LoginModel(userNameOrEmail: String, password: String)

  case class UpdateUserCredentialsModel(userName: Option[String] = None, email: Option[String] = None)

  case class UpdateUserStatusModel(banned: Boolean)

  case class ChangePasswordModel(oldPassword: String, newPassword: String, newPasswordCheck: String)

}

case class UserInfo(id: Long, userName: String, email: String, role: UserRole.Value, banned: Boolean, deleted: Boolean)

case class UserView(id: Long, userName: String, email: String)
