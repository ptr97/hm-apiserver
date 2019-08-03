package com.pwos.api.domain.users


case class User(
                 id: Long,
                 userName: String,
                 email: String,
                 password: User.Password,
                 role: UserRole.Value = UserRole.User,
                 banned: Boolean = false,
                 deleted: Boolean = false
               ) {
  def toView: UserView = UserView(id, userName, email)
}

object User {
  type Password = String
}


object UserModels {

  case class RegistrationModel(userName: String, email: String, password: String, passwordCheck: String)

  case class LoginModel(userNameOrEmail: String, password: String)

  case class UpdateUserCredentialsModel(userName: Option[String], email: Option[String])

  case class UpdatePasswordModel(oldPassword: String, newPassword: String, newPasswordCheck: String)
}

case class UserInfo(id: Long, userName: String, email: String, role: UserRole.Value, banned: Boolean, deleted: Boolean)

case class UserView(id: Long, userName: String, email: String)
