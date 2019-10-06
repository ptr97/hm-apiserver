package com.pwos.api.infrastructure.dao.memory

import cats.Id
import cats.implicits._
import com.pwos.api.PaginatedResult
import com.pwos.api.domain.PagingRequest
import com.pwos.api.domain.QueryParameters
import com.pwos.api.domain.users.User
import com.pwos.api.domain.users.UserDAOAlgebra


class MemoryUserDAOInterpreter extends UserDAOAlgebra[Id] {

  private var users: List[User] = List.empty

  private var userIdAutoIncrement: Long = 1

  def getLastId: Long = userIdAutoIncrement

  override def create(user: User): Id[User] = {
    val userWithId: User = user.copy(id = Some(userIdAutoIncrement))
    this.userIdAutoIncrement += 1
    this.users = userWithId :: this.users
    userWithId
  }

  override def get(id: Long): Id[Option[User]] = {
    this.users.find(_.id === Option(id))
  }

  override def get(ids: List[Long]): Id[List[User]] = {
    this.users.filter { user =>
      ids.contains(user.id.get)
    }
  }

  override def findByName(name: String): Id[Option[User]] = {
    this.users.find(_.userName === name)
  }

  override def findByEmail(email: String): Id[Option[User]] = {
    this.users.find(_.email === email)
  }

  override def update(user: User): Id[Option[User]] = {
    for {
      found <- this.users.find(_.id === user.id)
      newList = user :: this.users.filterNot(_.id === found.id)
      _ = this.users = newList
      updated <- this.users.find(_.id === user.id)
    } yield updated
  }

  override def markDeleted(id: Long): Id[Boolean] = {
    (for {
      found <- get(id)
      deletedUser = found.copy(deleted = true)
      _ <- update(deletedUser)
    } yield true) getOrElse false
  }

  override def list(queryParameters: QueryParameters, pagingRequest: PagingRequest): Id[PaginatedResult[User]] = {
    val sortedUsers: List[User] = users.sortBy(_.id)
    val withOffset: List[User] = sortedUsers.drop(pagingRequest.offset)
    val withLimit: List[User] = pagingRequest.maybePageSize.map { pageSize =>
      withOffset.take(pageSize)
    } getOrElse withOffset

    PaginatedResult.build(withLimit, users.length, pagingRequest)
  }
}

object MemoryUserDAOInterpreter {
  def apply(): MemoryUserDAOInterpreter = new MemoryUserDAOInterpreter()
}
