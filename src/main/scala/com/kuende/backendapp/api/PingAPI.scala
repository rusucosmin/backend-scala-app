package com.kuende.backendapp.api

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller

class PingAPI extends Controller {
  get("/api/v1/ping") { request: Request =>
    Map("ping" -> "pong")
  }
}
