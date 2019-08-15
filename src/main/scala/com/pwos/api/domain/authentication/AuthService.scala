package com.pwos.api.domain.authentication

import cats.Functor
import cats.Monad
import cats.data.EitherT
import com.pwos.api.domain.UserNotFoundError
import com.pwos.api.domain.users.User
import com.pwos.api.domain.users.UserDAOAlgebra
import com.pwos.api.domain.users.UserModels.LoginModel
import com.pwos.api.domain.users.UserValidationAlgebra
import com.pwos.api.infrastructure.http.authentication.JsonWebToken
import com.pwos.api.infrastructure.http.authentication.JwtAuth


class AuthService[F[_]](userDAO: UserDAOAlgebra[F], userValidation: UserValidationAlgebra[F]) {

  def get(id: Long)(implicit F: Functor[F]): EitherT[F, UserNotFoundError.type, User] = {
    EitherT.fromOptionF(userDAO.get(id), UserNotFoundError)
  }

  def logIn(loginModel: LoginModel)(implicit M: Monad[F]): EitherT[F, UserNotFoundError.type, JsonWebToken] = {
    val userByNameOrByEmail: String => EitherT[F, UserNotFoundError.type, User] = userNameOrEmail =>
      EitherT.fromOptionF(userDAO.findByName(userNameOrEmail), UserNotFoundError) orElse
        EitherT.fromOptionF(userDAO.findByEmail(userNameOrEmail), UserNotFoundError)

    for {
      user <- userByNameOrByEmail(loginModel.userNameOrEmail)
      userInfo = user.buildUserInfo
      token <- EitherT.fromOption(userInfo.map(info => JwtAuth.decodeJwt(info)), UserNotFoundError)
    } yield token
  }

//  def refreshToken() = ???

}

object AuthService {
  def apply[F[_]](userDAO: UserDAOAlgebra[F], userValidation: UserValidationAlgebra[F]): AuthService[F] =
    new AuthService(userDAO, userValidation)
}
