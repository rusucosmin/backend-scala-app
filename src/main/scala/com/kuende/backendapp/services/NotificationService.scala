package com.kuende.backendapp.services

import java.time.Instant
import java.util.UUID

import com.google.inject.{Inject, Singleton}
import com.kuende.backendapp.consumers.NotificationPublishRequest
import com.kuende.backendapp.models.enum.NotificationKind
import com.kuende.backendapp.models.{Notification, Notifications}
import com.twitter.finatra.http.exceptions.BadRequestException
import com.twitter.util.Future

@Singleton
class NotificationService @Inject()(notifications: Notifications) {
  def filter(profileRefId: UUID, perPage: Int, changedAt: Instant, since: Instant,
      seen: Boolean): Future[Seq[Notification]] = {
    notifications.filter(profileRefId, perPage, changedAt, since, seen)
  }

  def create(req: NotificationPublishRequest): Future[Notification] = {
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

    notifications.create(notification)
  }

  def markAsSeen(profileRefId: UUID, notificationId: Long): Future[Unit] = {
    notifications.getNotification(notificationId, profileRefId).flatMap {
      case Some(_) => notifications.markAsSeen(notificationId).unit
      case None => Future.exception(new BadRequestException("Notification not found")).unit
    }
  }

  def updateCreateAt(notificationId: Long, createdAt: Instant) = {
    notifications.updateCreateAt(notificationId, createdAt)
  }

  def unseenCount(profileRefId: UUID): Future[Int] = {
    notifications.getUnreadNotificationsCount(profileRefId)
  }

  def clearOldNotifications() = {
    notifications.clearOldNotifications
  }
}
