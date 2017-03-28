package com.kuende.backendapp.api

import com.google.inject.{Inject, Singleton}
import com.kuende.backendapp.api.entities.NotificationEntity
import com.kuende.backendapp.services.NotificationService
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.kuende.backendapp.api.filters.UserContext._

@Singleton
class NotificationAPI @Inject()(notificationService: NotificationService) extends Controller {
  get("/api/v1/notifications") { request: Request =>
    val profileRefId = request.userId
    for {
      notifications <- notificationService.filter(profileRefId)
      entities = NotificationEntity(notifications)
    } yield entities
  }
}
