package com.pwos.api.domain.users


object UserRole extends Enumeration {
  type UserRole = Value

  val User: Value = Value("user")
  val Admin: Value = Value("admin")
}
