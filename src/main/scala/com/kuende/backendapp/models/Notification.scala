package com.kuende.backendapp.models

import java.time.Instant
import java.util.UUID

import com.google.inject.{Inject, Singleton}
import com.kuende.backendapp.models.enum.NotificationKind
import com.kuende.backendapp.models.mysql.DateEncoding
import com.kuende.backendapp.util.MysqlContext
import com.twitter.util.Future

case class Notification(id: Long,
                        profileRefId: UUID,
                        message: String,
                        avatar: Option[String],
                        kind: NotificationKind,
                        seen: Boolean,
                        createdAt: Instant,
                        updatedAt: Instant)

@Singleton
class Notifications @Inject()(val db: MysqlContext) extends DateEncoding {
  import db._

  implicit val notificationsSchemaMeta = schemaMeta[Notification]("notifications")

  implicit val encodeNotificationKind = NotificationKind.encoder
  implicit val decodeNotificationKind = NotificationKind.decoder

  def getAll(profileRefId: UUID): Future[Seq[Notification]] = {
    val q = quote {
      query[Notification].filter(n => n.profileRefId == lift(profileRefId))
    }

    run(q)
  }

  def create(notification: Notification): Future[Notification] = {
    val q = quote {
      query[Notification].insert(lift(notification)).returning(_.id)
    }

    db.run(q).map(id => notification.copy(id = id))
  }

  def testTeardown() = {
    val q = quote {
      query[Notification].delete
    }

    db.run(q)
  }
}
