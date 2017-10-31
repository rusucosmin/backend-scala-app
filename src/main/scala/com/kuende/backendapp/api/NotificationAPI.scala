package com.kuende.backendapp.api

import java.time.Instant

import com.google.inject.{Inject, Singleton}
import com.kuende.backendapp.api.entities.NotificationEntity
import com.kuende.backendapp.services.NotificationService
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.request.{QueryParam, RouteParam}
import com.twitter.finatra.validation.{Max, Min}
import com.kuende.backendapp.api.filters.UserContext._
import com.twitter.finatra.http.exceptions.BadRequestException

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
    println("get request")
    println("per_page= " + request.per_page)
    println("since = " + request.since)
    println("changed_at = " + request.changed_at)
    println("unseen = " + request.unseen)
    println("userId = " + request.request.userId)
    val profileRefId = request.request.userId
    val perPage = request.per_page
    val changedAt = Instant.ofEpochSecond(request.changed_at)
    val since = Instant.ofEpochSecond(request.since)
    val seen = !request.unseen
    println("profileRefId = " + profileRefId)
    println("perPage = " + perPage)
    println("changedAt = " + changedAt)
    println("since = " + since)
    println("seen = " + seen)
    for {
      notifications <- notificationService.filter(profileRefId, perPage, changedAt, since, seen)
      entities = NotificationEntity(notifications)
    } yield entities
  }

  put("/api/v1/notifications/:id") { request: MarkNotificationAsSeenRequest =>
    println("put request")
    val profileRefId = request.request.userId
    val notificationId = request.id
    println("profileRefId = " + profileRefId)
    println("notificationId = " + notificationId)
    for {
      notification <- notificationService.markAsSeen(profileRefId, notificationId)
    } yield ()
  }
}
