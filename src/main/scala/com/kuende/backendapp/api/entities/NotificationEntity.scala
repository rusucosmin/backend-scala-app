package com.kuende.backendapp.api.entities

import java.util.UUID

import com.kuende.backendapp.models.Notification

case class NotificationEntity(id: Long,
                              profile: UUID,
                              message: String,
                              avatar: Option[String],
                              kind: String,
                              seen: Boolean,
                              createdAt: Long)

object NotificationEntity {
  def apply(notification: Notification): NotificationEntity = {
    NotificationEntity(
      id = notification.id,
      profile = notification.profileRefId,
      message = notification.message,
      avatar = notification.avatar,
      kind = notification.kind.publicValue,
      seen = notification.seen,
      createdAt = notification.createdAt.getEpochSecond
    )
  }

  def apply(notifications: Seq[Notification]): Seq[NotificationEntity] = {
    notifications.map(NotificationEntity(_))
  }
}
