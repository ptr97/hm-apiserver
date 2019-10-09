package com.pwos.api.infrastructure.dao.slick.users

import cats.implicits._
import com.pwos.api.PaginatedResult
import com.pwos.api.domain.PagingRequest
import com.pwos.api.domain.QueryParameters
import com.pwos.api.domain.users.User
import com.pwos.api.domain.users.UserDAOAlgebra
import com.pwos.api.domain.users.UserRole
import com.pwos.api.infrastructure.dao.slick.SlickImplicits._
import slick.dbio.DBIO
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.ExecutionContext


final class SlickUserDAOInterpreter(implicit ec: ExecutionContext) extends UserDAOAlgebra[DBIO] {
  val users: TableQuery[UserTable] = TableQuery[UserTable]

  private def activeUsers = users filter (_.banned === false) filter (_.deleted === false)

  override def create(user: User): DBIO[User] = {
    users returning users
      .map(_.id) into((user, id) => user.copy(id = id.some)) += user
  }

  override def get(id: Long): DBIO[Option[User]] = {
    activeUsers.filter(_.id === id).result.headOption
  }

  override def get(ids: List[Long]): DBIO[List[User]] = {
    activeUsers.filter(_.id inSet ids).result.map(_.toList)
  }

  override def findByName(name: String): DBIO[Option[User]] = {
    activeUsers.filter(_.username === name).result.headOption
  }

  override def findByEmail(email: String): DBIO[Option[User]] = {
    activeUsers.filter(_.email === email).result.headOption
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

    val usersWithSearch: Query[UserTable, User, Seq] = queryParameters.search map { searchTerm: String =>
      activeUsers filter { user: UserTable =>
        user.username.like(s"%$searchTerm%") || user.email.like(s"%$searchTerm%")
      }
    } getOrElse activeUsers

    val usersWithFilter: Query[UserTable, User, Seq] = queryParameters.filterBy map { filters: Map[String, String] =>
      val maybeFilterByBannedStatus: Option[Boolean] = filters.get("banned").flatMap {
        case "true" => Some(true)
        case "false" => Some(false)
        case _ => None
      }

      val maybeFilterByRole: Option[String] = filters.get("role")

      val usersFilteredByStatus: Query[UserTable, User, Seq] = maybeFilterByBannedStatus map { status =>
        usersWithSearch.filter(_.banned === status)
      } getOrElse usersWithSearch

      implicit val enumMapping = UserTable.enumMapping

      maybeFilterByRole map { role: String =>
        usersFilteredByStatus.filter(_.role === UserRole.withName(role))
      } getOrElse usersFilteredByStatus

    } getOrElse usersWithSearch


    val sortedUsers: Query[UserTable, User, Seq] = (pagingRequest.sortBy, pagingRequest.asc) match {
      case (Some("id"), true) => usersWithFilter.sortBy(_.id.asc)
      case (Some("id"), false) => usersWithFilter.sortBy(_.id.desc)

      case (Some("username"), true) => usersWithFilter.sortBy(_.username.asc)
      case (Some("username"), false) => usersWithFilter.sortBy(_.username.desc)

      case (Some("email"), true) => usersWithFilter.sortBy(_.email.asc)
      case (Some("email"), false) => usersWithFilter.sortBy(_.email.desc)

      case (Some("banned"), true) => usersWithFilter.sortBy(_.banned.asc)
      case (Some("banned"), false) => usersWithFilter.sortBy(_.banned.desc)

      case (_, _) => usersWithFilter.sortBy(_.id.asc)
    }

    val allUsersCount: DBIO[Int] = users.length.result

    for {
      usersResult <- sortedUsers.paged(pagingRequest).result
      totalCount <- allUsersCount
    } yield PaginatedResult.build(usersResult.toList, totalCount, pagingRequest)
  }

}

object SlickUserDAOInterpreter {
  def apply(implicit executionContext: ExecutionContext): SlickUserDAOInterpreter =
    new SlickUserDAOInterpreter()
}
