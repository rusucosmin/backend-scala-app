package com.kuende.backendapp.services

import java.time.Instant
import java.util.UUID

import com.google.inject.{Inject, Singleton}
import com.kuende.backendapp.consumers.NotificationPublishRequest
import com.kuende.backendapp.models.enum.NotificationKind
import com.kuende.backendapp.models.{Notification, Notifications}
import com.twitter.util.Future

@Singleton
class NotificationService @Inject()(idService: IdService, notifications: Notifications) {
  def filter(profileRefId: UUID): Future[Seq[Notification]] = {
    notifications.getAll(profileRefId)
  }

  def create(req: NotificationPublishRequest): Future[Unit] = {
    val notification = Notification(
      id = 0,
      profileRefId = UUID.fromString(req.notifiableRefId),
      message = req.message,
      avatar = req.avatar,
      kind = NotificationKind(req.kind),
      seen = false,
      createdAt = Instant.now(),
      updatedAt = Instant.now()
    )

    notifications.create(notification).unit
  }
}
