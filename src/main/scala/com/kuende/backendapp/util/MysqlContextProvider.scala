package com.kuende.backendapp.util

import com.google.inject.{Provides, Singleton}
import com.twitter.inject.{Injector, TwitterModule}
import com.typesafe.config.ConfigFactory
import io.getquill.{FinagleMysqlContext, SnakeCase}

object MysqlContextProvider extends TwitterModule {
  @Singleton
  @Provides
  def providesMysqlContext(injector: Injector): MysqlContext = {
    val config = ConfigFactory.load().getConfig("mysql")
    new FinagleMysqlContext[SnakeCase](config)
  }
}
