package com.pwos.api.domain.users


object UserRole extends Enumeration {
  type UserRole = Value

  val User: UserRole = Value("user")
  val Admin: UserRole = Value("admin")
}
