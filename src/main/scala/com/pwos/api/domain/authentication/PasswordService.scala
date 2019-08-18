package com.pwos.api.domain.authentication

import org.mindrot.jbcrypt.BCrypt


object PasswordService {

  type Password = String

  def hash(plainPassword: String): Password = {
    BCrypt.hashpw(plainPassword, BCrypt.gensalt(12))
  }

  def compare(plainPassword: String, hashedPassword: Password): Boolean = {
    BCrypt.checkpw(plainPassword, hashedPassword)
  }

}
