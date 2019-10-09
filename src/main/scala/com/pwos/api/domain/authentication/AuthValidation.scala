package com.pwos.api.domain.authentication

import cats.implicits._
import com.pwos.api.domain.users.UserInfo
import com.pwos.api.domain.users.UserRole


trait AuthValidation {

  def validateAdminAccess[E](userInfo: UserInfo)(error: E): Either[E, Unit] = {
    if (userInfo.role == UserRole.Admin) {
      ().asRight[E]
    } else {
      error.asLeft[Unit]
    }
  }

}
