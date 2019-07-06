package com.pwos.api.domain.users

case class User(
                 userName: String,
                 email: String,
                 id: Option[Long] = None
               )
