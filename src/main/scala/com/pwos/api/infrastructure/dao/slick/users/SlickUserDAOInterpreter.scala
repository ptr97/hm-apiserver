package com.pwos.api.infrastructure.dao.slick.users

import com.pwos.api.domain.users.User
import com.pwos.api.domain.users.UserDAOAlgebra
import slick.dbio.DBIO
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.ExecutionContext


final class SlickUserDAOInterpreter(implicit ec: ExecutionContext) extends UserDAOAlgebra[DBIO] {
  val users: TableQuery[UserTable] = TableQuery[UserTable]

  override def create(user: User): DBIO[User] = {
    val newUserId: DBIO[Long] = users returning users.map(_.id) += user
    newUserId.flatMap { id =>
      users.filter(_.id === id).result.head
    }
  }

  override def get(id: Long): DBIO[Option[User]] = {
    users.filter(_.id === id).result.headOption
  }

  override def findByName(name: String): DBIO[Option[User]] = {
    users.filter(_.username === name).result.headOption
  }

  override def findByEmail(email: String): DBIO[Option[User]] = {
    users.filter(_.email === email).result.headOption
  }

  override def update(user: User): DBIO[Option[User]] = {
    users.filter(_.id === user.id).update(user).map { count =>
      if (count > 0) Some(user) else None
    }
  }

  override def markDeleted(id: Long): DBIO[Boolean] = {
    users.filter(_.id === id).map(_.deleted).update(true).map(_ == 1)
  }

  override lazy val all: DBIO[List[User]] = {
    users.result.map(_.toList)
  }

}

object SlickUserDAOInterpreter {
  def apply(implicit executionContext: ExecutionContext): SlickUserDAOInterpreter =
    new SlickUserDAOInterpreter()
}
