package com.kuende.backendapp.servers

import com.kuende.backendapp.api.{NotificationAPI, PingAPI}
import com.kuende.backendapp.api.filters.HasUserFilter
import com.kuende.backendapp.api.mappers.CaseClassMappingExceptionMapper
import com.kuende.backendapp.util.MysqlContextProvider
import com.twitter.finagle.Http
import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.finagle.tracing.NullTracer
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.routing.HttpRouter

class Server extends HttpServer {

  override val modules =
    Seq(
      MysqlContextProvider
    )

  override def configureHttpServer(server: Http.Server) = {
    server
      .withCompressionLevel(0)
      .withStatsReceiver(NullStatsReceiver)
      .withTracer(NullTracer)
  }

  override def configureHttp(router: HttpRouter): Unit = {
    router
      .filter[CommonFilters]
      .add[PingAPI]
      .add[HasUserFilter, NotificationAPI]
      .exceptionMapper[CaseClassMappingExceptionMapper]
  }
}
