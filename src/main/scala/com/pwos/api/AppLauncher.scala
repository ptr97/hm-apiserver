package com.pwos.api

import com.pwos.api.config.Config


object AppLauncher extends App {
  val config: Config = Config.unsafeLoadConfig
  Server.run(config)
}
