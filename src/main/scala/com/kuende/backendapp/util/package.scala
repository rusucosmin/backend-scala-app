package com.kuende.backendapp

import io.getquill.{FinagleMysqlContext, SnakeCase}

package object util {
  type MysqlContext = FinagleMysqlContext[SnakeCase]
}
