package com.pwos.api.domain.users

import com.pwos.api.domain.authentication.PasswordService.Password


case class User(
  userName: String,
  email: String,
  password: Password,
  role: UserRole.UserRole = UserRole.User,
  banned: Boolean = false,
  id: Option[Long] = None
) {
  def toView: Option[UserView] = id.map { id => UserView(id, userName, email) }

  def buildUserInfo: Option[UserInfo] = id.map { id => UserInfo(id, userName, email, role, banned) }
}


object UserModels {

  case class CreateUserModel(userName: String, email: String, password: String, passwordCheck: String)

  case class LoginModel(userNameOrEmail: String, password: String)

  case class UpdateUserCredentialsModel(userName: Option[String] = None, email: Option[String] = None)

  case class UpdateUserStatusModel(banned: Boolean)

  case class ChangePasswordModel(oldPassword: String, newPassword: String, newPasswordCheck: String)

}

case class UserInfo(id: Long, userName: String, email: String, role: UserRole.UserRole, banned: Boolean)

object UserInfo {
  def forUser(user: User): UserInfo = {
    UserInfo(
      id = user.id.get,
      userName = user.userName,
      email = user.email,
      role = user.role,
      banned = user.banned
    )
  }
}

case class UserView(id: Long, userName: String, email: String)
