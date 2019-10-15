package com.pwos.api.infastructure

import akka.http.scaladsl.model.headers.RawHeader
import cats.data.OptionT
import com.pwos.api.domain.authentication.AuthService
import com.pwos.api.domain.opinions.OpinionService
import com.pwos.api.domain.opinions.OpinionValidationAlgebra
import com.pwos.api.domain.opinions.OpinionValidationInterpreter
import com.pwos.api.domain.opinions.tags.Tag
import com.pwos.api.domain.opinions.tags.TagCategory
import com.pwos.api.domain.opinions.tags.TagService
import com.pwos.api.domain.opinions.tags.TagValidationAlgebra
import com.pwos.api.domain.opinions.tags.TagValidationInterpreter
import com.pwos.api.domain.places.Place
import com.pwos.api.domain.places.PlaceService
import com.pwos.api.domain.places.PlaceValidationInterpreter
import com.pwos.api.domain.users.User
import com.pwos.api.domain.users.UserInfo
import com.pwos.api.domain.users.UserModels.CreateUserModel
import com.pwos.api.domain.users.UserRole
import com.pwos.api.domain.users.UserService
import com.pwos.api.domain.users.UserValidationInterpreter
import com.pwos.api.infrastructure.dao.slick.DBIOMonad._
import com.pwos.api.infrastructure.dao.slick.opinions.SlickOpinionDAOInterpreter
import com.pwos.api.infrastructure.dao.slick.opinions.reports.SlickReportDAOInterpreter
import com.pwos.api.infrastructure.dao.slick.opinions.tags.SlickTagDAOInterpreter
import com.pwos.api.infrastructure.dao.slick.places.SlickPlaceDAOInterpreter
import com.pwos.api.infrastructure.dao.slick.users.SlickUserDAOInterpreter
import com.pwos.api.infrastructure.http.OpinionController
import com.pwos.api.infrastructure.http.PlaceController
import com.pwos.api.infrastructure.http.TagController
import com.pwos.api.infrastructure.http.UserController
import com.pwos.api.infrastructure.http.authentication.AuthController
import com.pwos.api.infrastructure.http.authentication.JwtAuth
import slick.dbio.DBIO
import slick.jdbc.MySQLProfile.backend.Database

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration


object Mocks {

  object Users {
    val admin: User = User("admin", "admin@hm.com", "Password123", UserRole.Admin, banned = false, Some(1L))
    val user1: User = User("Test User 1", "test1@test.com", "TestPass123", UserRole.User, banned = false, Some(2L))
    val user2: User = User("Test User 2", "test2@test.com", "TestPass123", UserRole.User, banned = false, Some(3L))
    val user3: User = User("Test User 3", "test3@test.com", "TestPass123", UserRole.User, banned = false, Some(4L))
    val user4: User = User("Test User 4", "test4@test.com", "TestPass123", UserRole.User, banned = false, Some(5L))
  }

  object Places {
    val place1: Place = Place("Place One", 50.067106, 19.913587, 203, Some(1L))
    val place2: Place = Place("Place Two", 37.7825062, 20.8950319, 13, Some(2L))
    val place3: Place = Place("Place Three", -0.617644, 73.093730, 7, Some(3L))
  }

  object Tags {
    val tag1 = Tag("Tag 1", TagCategory.THREATS, id = Some(1L))
    val tag2 = Tag("Tag 2", TagCategory.EQUIPMENT, id = Some(2L))
    val tag3 = Tag("Tag 3", TagCategory.SUBSOIL, id = Some(3L))
    val tag4Disabled = Tag("Tag 3", TagCategory.SUBSOIL, enabled = false, id = Some(4L))
  }

  def createAdmin(admin: User, userService: UserService[DBIO], userDAO: SlickUserDAOInterpreter)(implicit ec: ExecutionContext, db: Database): String = {
    val createAdminModel = CreateUserModel(admin.userName, admin.email, admin.password, admin.password)
    val action = for {
      userView <- userService.create(createAdminModel).toOption
      userFromDb <- OptionT(userDAO.get(userView.id))
      adminFromDb <- OptionT(userDAO.update(userFromDb.copy(role = UserRole.Admin)))
      adminInfo = UserInfo.forUser(adminFromDb)
    } yield JwtAuth.decodeJwt(adminInfo)

    Await.result(db.run(action.value), Duration.Inf).map(_.token).get
  }

  def createUser(user: User, userService: UserService[DBIO], userDAO: SlickUserDAOInterpreter)(implicit ec: ExecutionContext, db: Database): String = {
    val createUserModel = CreateUserModel(user.userName, user.email, user.password, user.password)
    val action = for {
      userView <- userService.create(createUserModel).toOption
      userFromDb <- OptionT(userDAO.get(userView.id))
      userInfo = UserInfo.forUser(userFromDb)
    } yield JwtAuth.decodeJwt(userInfo)

    Await.result(db.run(action.value), Duration.Inf).map(_.token).get
  }

  def authHeader(token: String): RawHeader = {
    RawHeader("Authorization", s"Bearer $token")
  }


  def authResources(implicit ec: ExecutionContext, database: Database): (SlickUserDAOInterpreter, AuthController) = {
    lazy val userDAO: SlickUserDAOInterpreter = SlickUserDAOInterpreter(ec)
    lazy val authService: AuthService[DBIO] = AuthService(userDAO)
    lazy val authController: AuthController = AuthController(authService)

    (userDAO, authController)
  }

  def userResources(implicit ec: ExecutionContext, database: Database): (SlickUserDAOInterpreter, UserService[DBIO], UserController) = {
    lazy val userDAO: SlickUserDAOInterpreter = SlickUserDAOInterpreter(ec)
    lazy val userValidation: UserValidationInterpreter[DBIO] = UserValidationInterpreter[DBIO](userDAO)
    lazy val userService: UserService[DBIO] = UserService(userDAO, userValidation)
    lazy val userController: UserController = UserController(userService)

    (userDAO, userService, userController)
  }

  def placeResources(implicit ec: ExecutionContext, database: Database): (SlickPlaceDAOInterpreter, PlaceController) = {
    lazy val placeDAO: SlickPlaceDAOInterpreter = SlickPlaceDAOInterpreter(ec)
    lazy val placeValidation: PlaceValidationInterpreter[DBIO] = PlaceValidationInterpreter[DBIO](placeDAO)
    lazy val placeService: PlaceService[DBIO] = PlaceService[DBIO](placeDAO, placeValidation)
    lazy val placeController: PlaceController = PlaceController(placeService)

    (placeDAO, placeController)
  }

  def tagResources(implicit ec: ExecutionContext, database: Database): (SlickTagDAOInterpreter, TagController) = {
    lazy val tagDAO: SlickTagDAOInterpreter = SlickTagDAOInterpreter(ec)
    lazy val tagValidation: TagValidationAlgebra[DBIO] = TagValidationInterpreter(tagDAO)
    lazy val tagService: TagService[DBIO] = TagService(tagDAO, tagValidation)
    lazy val tagController: TagController = TagController(tagService)

    (tagDAO, tagController)
  }

  def opinionResources(implicit ec: ExecutionContext, database: Database): (SlickOpinionDAOInterpreter, OpinionController) = {
    lazy val opinionDAO: SlickOpinionDAOInterpreter = SlickOpinionDAOInterpreter(ec)
    lazy val reportDAO: SlickReportDAOInterpreter = SlickReportDAOInterpreter(ec)
    lazy val userDAO: SlickUserDAOInterpreter = SlickUserDAOInterpreter(ec)
    lazy val placeDAO: SlickPlaceDAOInterpreter = SlickPlaceDAOInterpreter(ec)
    lazy val opinionValidation: OpinionValidationAlgebra[DBIO] = OpinionValidationInterpreter[DBIO](opinionDAO)
    lazy val placeValidation: PlaceValidationInterpreter[DBIO] = PlaceValidationInterpreter[DBIO](placeDAO)
    lazy val opinionService: OpinionService[DBIO] = OpinionService[DBIO](opinionDAO, reportDAO, userDAO, opinionValidation, placeValidation)
    lazy val opinionController: OpinionController = OpinionController(opinionService)

    (opinionDAO, opinionController)
  }

}
