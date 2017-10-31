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

  def filter(profileRefId: UUID, perPage: Int, changedAt: Instant, since: Instant,
      seen: Boolean): Future[Seq[Notification]] = {
    // select * where notification.createdAt >= since && notification.changedAt <= changedAt
    //    && (seen || notification.seen == false limit perPage
    println("filter::" + changedAt.getEpochSecond)
    val q = quote {
      query[Notification]
          .filter(n => n.profileRefId == lift(profileRefId))       // filter notification from current user
          .filter(n => n.createdAt > lift(since))     // filter only notifications since date
          .filter(n => n.createdAt < lift(changedAt)) // filter only notifications before date
          .filter(n => lift(seen) || !n.seen)                      // if seen is set to true, return all
                                                                   // otherwise return only unseen notifs
          .sortBy(n => n.createdAt)                                // sort by
          .take(lift(perPage))                                     // limit
    }

    run(q)
  }

  def create(notification: Notification): Future[Notification] = {
    val q = quote {
      query[Notification].insert(lift(notification)).returning(_.id)
    }

    run(q).map(id => notification.copy(id = id))
  }

  def markAsSeen(notificationId: Long) = {
    val q = quote {
      query[Notification]
          .filter(n => n.id == lift(notificationId))
          .update(_.seen -> lift(true))
    }

    run(q)
  }

  def updateCreateAt(notificationId: Long, createdAt: Instant) = {
    val q = quote {
      query[Notification]
          .filter(n => n.id == lift(notificationId))
          .update(_.createdAt -> lift(createdAt))
    }

    run(q)
  }

  def getNotification(notificationId: Long): Future[Notification] = {
    val q = quote {
      query[Notification]
          .filter(n => n.id == lift(notificationId))
    }

    run(q).map(_.head)
  }

  def testTeardown(): Future[Long] = {
    val q = quote {
      query[Notification].delete
    }

    run(q)
  }
}
