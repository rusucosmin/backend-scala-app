package com.kuende.backendapp.api

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller

class NotificationAPI extends Controller {
  get("/api/v1/notifications") { request: Request =>
    List[String]()
  }
}
