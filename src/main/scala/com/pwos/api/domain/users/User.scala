package com.pwos.api.domain.users


case class User(
                 userName: String,
                 email: String,
                 password: User.Password,
                 role: UserRole.Value = UserRole.User,
                 banned: Boolean = false,
                 deleted: Boolean = false,
                 id: Option[Long]
               ) {
  def toView: Option[UserView] = id.map { id => UserView(id, userName, email) }

  def buildUserInfo: Option[UserInfo] = id.map { id => UserInfo(id, userName, email, role, banned, deleted) }
}

object User {
  type Password = String
}


object UserModels {

  case class CreateUserModel(userName: String, email: String, password: String, passwordCheck: String)

  case class LoginModel(userNameOrEmail: String, password: String)

  case class UpdateUserCredentialsModel(userName: Option[String], email: Option[String])

  case class UpdateUserStatusModel(banned: Boolean)

  case class ChangePasswordModel(oldPassword: String, newPassword: String, newPasswordCheck: String)
}

case class UserInfo(id: Long, userName: String, email: String, role: UserRole.Value, banned: Boolean, deleted: Boolean)

case class UserView(id: Long, userName: String, email: String)
