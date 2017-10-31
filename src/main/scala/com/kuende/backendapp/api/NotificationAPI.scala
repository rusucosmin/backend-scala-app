package com.kuende.backendapp.api

import java.time.Instant

import com.google.inject.{Inject, Singleton}
import com.kuende.backendapp.api.entities.NotificationEntity
import com.kuende.backendapp.services.NotificationService
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.request.QueryParam
import com.twitter.finatra.validation.{Max, Min}
import com.kuende.backendapp.api.filters.UserContext._

case class GetNotificationsRequest(
  @QueryParam @Max(1000) @Min(1) per_page: Int = 5,
  @QueryParam changed_at: String = "0",
  @QueryParam since: String = Instant.MAX.getEpochSecond.toString,
  @QueryParam unseen: Boolean = false,
  request: Request
)


@Singleton
class NotificationAPI @Inject()(notificationService: NotificationService) extends Controller {
  get("/api/v1/notifications") { request: GetNotificationsRequest =>
    val profileRefId = request.request.userId
    val perPage = request.per_page
    val changedAt = Instant.ofEpochSecond(request.changed_at.toLong)
    val since = Instant.ofEpochSecond(request.since.toLong)
    val seen = !request.unseen
    for {
      notifications <- notificationService.filter(profileRefId, perPage, changedAt, since, seen)
      entities = NotificationEntity(notifications)
    } yield entities
  }
}
