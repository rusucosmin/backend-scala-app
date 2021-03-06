package com.kuende.backendapp.api

import java.time.Instant

import com.google.inject.{Inject, Singleton}
import com.kuende.backendapp.api.entities.NotificationEntity
import com.kuende.backendapp.api.filters.UserContext._
import com.kuende.backendapp.services.NotificationService
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.request.{QueryParam, RouteParam}
import com.twitter.finatra.validation.{Max, Min}

case class GetNotificationsRequest(
    @QueryParam @Max(1000) @Min(1) per_page: Int = 5,
    @QueryParam since: Long = 0,
    @QueryParam changed_at: Long = Instant.now().plusSeconds(10).getEpochSecond,
    @QueryParam unseen: Boolean = false,
    request: Request
)

case class MarkNotificationAsSeenRequest(
    @RouteParam id: Long,
    request: Request
)

@Singleton
class NotificationAPI @Inject()(notificationService: NotificationService) extends Controller {
  get("/api/v1/notifications") { request: GetNotificationsRequest =>
    val profileRefId = request.request.userId
    val perPage = request.per_page
    val changedAt = Instant.ofEpochSecond(request.changed_at)
    val since = Instant.ofEpochSecond(request.since)
    val seen = !request.unseen
    for {
      notifications <- notificationService.filter(profileRefId, perPage, changedAt, since, seen)
      unseenCount <- notificationService.unseenCount(profileRefId)
      entities = NotificationEntity(notifications)
    } yield response
        .ok(entities)
        .header("X-Notifications-Unseen", unseenCount)
        .response
  }

  put("/api/v1/notifications/:id") { request: MarkNotificationAsSeenRequest =>
    val profileRefId = request.request.userId
    val notificationId = request.id
    for {
      notification <- notificationService.markAsSeen(profileRefId, notificationId)
    } yield ()
  }
}
