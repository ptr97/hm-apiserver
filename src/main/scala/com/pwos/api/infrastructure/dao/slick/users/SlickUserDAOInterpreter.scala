package com.pwos.api.infrastructure.dao.slick.users

import com.pwos.api.PaginatedResult
import com.pwos.api.domain.PagingRequest
import com.pwos.api.domain.QueryParameters
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

  override def list(queryParameters: QueryParameters, pagingRequest: PagingRequest): DBIO[PaginatedResult[User]] = {

    val sortedUsers: Query[UserTable, User, Seq] = (pagingRequest.sortBy, pagingRequest.asc) match {
      case (Some("id"), true) => users.sortBy(_.id.asc)
      case (Some("id"), false) => users.sortBy(_.id.desc)

      case (Some("username"), true) => users.sortBy(_.username.asc)
      case (Some("username"), false) => users.sortBy(_.username.desc)

      case (Some("email"), true) => users.sortBy(_.email.asc)
      case (Some("email"), false) => users.sortBy(_.email.desc)

      case (Some("banned"), true) => users.sortBy(_.banned.asc)
      case (Some("banned"), false) => users.sortBy(_.banned.desc)

      case (_, _) => users.sortBy(_.id.asc)
    }

    val withOffset: Query[UserTable, User, Seq] = sortedUsers.drop(pagingRequest.offset)
    val withLimit: Query[UserTable, User, Seq] = pagingRequest.maybePageSize.map { pageSize =>
      withOffset.take(pageSize)
    } getOrElse withOffset

    val allUsersCount: DBIO[Int] = users.length.result

    for {
      users <- withLimit.result
      totalCount <- allUsersCount
    } yield {
      PaginatedResult(
        totalCount = totalCount,
        items = users.toList,
        hasNextPage = totalCount > pagingRequest.offset + users.length
      )
    }
  }

}

object SlickUserDAOInterpreter {
  def apply(implicit executionContext: ExecutionContext): SlickUserDAOInterpreter =
    new SlickUserDAOInterpreter()
}
