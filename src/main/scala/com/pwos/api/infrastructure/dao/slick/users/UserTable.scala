package com.pwos.api.infrastructure.dao.slick.users

import com.pwos.api.domain.users.User
import com.pwos.api.domain.users.UserRole
import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

import scala.language.postfixOps


class UserTable(tag: Tag) extends Table[User](tag, "USER") {

  implicit val enumMapping = UserTable.enumMapping

  def id: Rep[Long]               = column[Long]("ID", O.PrimaryKey, O.AutoInc)
  def username: Rep[String]       = column[String]("USERNAME")
  def email: Rep[String]          = column[String]("EMAIL")
  def password: Rep[String]       = column[String]("PASSWORD")
  def role: Rep[UserRole.Value]   = column[UserRole.Value]("ROLE")
  def banned: Rep[Boolean]        = column[Boolean]("BANNED")


  override def * : ProvenShape[User] = (
    username,
    email,
    password,
    role,
    banned,
    id.?
  ) <> (User.apply _ tupled, User.unapply)
}

object UserTable {
  val enumMapping = MappedColumnType.base[UserRole.Value, String](_.toString, s => UserRole.withName(s))
}
