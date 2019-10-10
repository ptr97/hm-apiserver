package com.pwos.api.domain.authentication

import cats.Monad
import cats.data.EitherT
import com.pwos.api.domain.HelloMountainsError._
import com.pwos.api.domain.authentication.PasswordService.Password
import com.pwos.api.domain.users.User
import com.pwos.api.domain.users.UserDAOAlgebra
import com.pwos.api.domain.users.UserModels.LoginModel
import com.pwos.api.infrastructure.http.authentication.JsonWebToken
import com.pwos.api.infrastructure.http.authentication.JwtAuth


class AuthService[F[_]](userDAO: UserDAOAlgebra[F]) {

  def logIn(loginModel: LoginModel)(implicit M: Monad[F]): EitherT[F, IncorrectCredentials.type, JsonWebToken] = {
    val userByNameOrByEmail: String => EitherT[F, IncorrectCredentials.type, User] = userNameOrEmail =>
      EitherT.fromOptionF(userDAO.findByName(userNameOrEmail), IncorrectCredentials) orElse
        EitherT.fromOptionF(userDAO.findByEmail(userNameOrEmail), IncorrectCredentials)

    val validatePassword: (String, Password) => EitherT[F, IncorrectCredentials.type, Unit] = (plainPassword, hashedPassword) => {
      if (PasswordService.compare(plainPassword, hashedPassword)) {
        EitherT.rightT(())
      } else {
        EitherT.leftT(IncorrectCredentials)
      }
    }

    for {
      user <- userByNameOrByEmail(loginModel.userNameOrEmail)
      _ <- validatePassword(loginModel.password, user.password)
      userInfo = user.buildUserInfo
      token <- EitherT.fromOption(userInfo.map(info => JwtAuth.decodeJwt(info)), IncorrectCredentials)
    } yield token
  }

}

object AuthService {
  def apply[F[_]](userDAO: UserDAOAlgebra[F]): AuthService[F] =
    new AuthService(userDAO)
}
