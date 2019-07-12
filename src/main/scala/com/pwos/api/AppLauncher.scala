package com.pwos.api

import com.pwos.api.config.ApplicationConfig


object AppLauncher extends App {
  val config: ApplicationConfig = ApplicationConfig.unsafeLoadConfig
  Server.run(config)
}
