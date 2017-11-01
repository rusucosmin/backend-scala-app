package com.kuende.backendapp.api

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

import com.kuende.backendapp.api.entities.NotificationEntity
import com.kuende.backendapp.consumers.NotificationPublishRequest
import com.kuende.backendapp.models.{Notification, Notifications}
import com.kuende.backendapp.servers.Server
import com.kuende.backendapp.services.NotificationService
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.finagle.http.Status._
import com.twitter.inject.TestMixin
import com.twitter.inject.server.FeatureTestMixin
import com.twitter.util.{Await, Awaitable, Future}
import com.twitter.conversions.time._
import org.scalatest.{BeforeAndAfterAll, FunSpec}

class NotificationAPISpec extends FunSpec with BeforeAndAfterAll with TestMixin with FeatureTestMixin {
  val server               = new EmbeddedHttpServer(new Server, disableTestLogging = true)
  val notifications        = injector.instance[Notifications]
  val notificationsService = injector.instance[NotificationService]

  var profileId = UUID.randomUUID()

  override def beforeEach(): Unit = {
    profileId = UUID.randomUUID()
  }

  override def afterAll(): Unit = {
    await(notifications.testTeardown())
  }

  def await[T](f: Future[T]): T = {
    Await.result(f, 2.seconds)
  }

  def authHeaders(profileId: UUID): Map[String, String] = {
    Map("Cookie" -> s"user_id=${profileId.toString}")
  }

  describe("GET /api/v1/notifications") {
    it("should returns bad request if user is not logged in") {
      server.httpGet(
        path = "/api/v1/notifications",
        andExpect = BadRequest,
        withJsonBody = "user is not logged in, please set cookie user_id to some value",
        suppress = true
      )
    }

    it("should return empty list when no notifications") {
      server.httpGet(
        path = "/api/v1/notifications",
        andExpect = Ok,
        withJsonBody = "[]",
        headers = authHeaders(profileId),
        suppress = true
      )
    }

    it("should correctly return primary interests") {
      val notification = NotificationPublishRequest(
        message = "Teodor Pripoae commented on your post",
        avatar = Some("https://img.kuende.com/avatars/foo/bar.jpg"),
        notifiableRefId = profileId.toString,
        kind = "post_comment"
      )
      await(notificationsService.create(notification))

      val response = server.httpGetJson[List[NotificationEntity]](
        path = "/api/v1/notifications",
        andExpect = Ok,
        headers = authHeaders(profileId),
        suppress = true
      )

      response.length should equal(1)
      response(0).message should equal("Teodor Pripoae commented on your post")
      response(0).avatar should equal(Some("https://img.kuende.com/avatars/foo/bar.jpg"))
      response(0).kind should equal("post_comment")
      response(0).seen should equal(false)
    }

    def addNotifToUser(profileId: UUID, _message: String = "msg", _avatar: String = "avatar",
        _kind: String = "kind"): Future[Notification] = {
      val notification = NotificationPublishRequest(
        message = _message,
        avatar = Some(_avatar),
        notifiableRefId = profileId.toString,
        kind = _kind
      )
      notificationsService.create(notification)
    }

    def insertNotifs(profileId: UUID, n: Int): Unit = {
      for (i <- 1 to n) await(addNotifToUser(profileId, i.toString, i.toString, "post_comment"))
    }

    def checkLengthOfRequest(_path: String, n: Int): Unit = {
      var response = server.httpGetJson[List[NotificationEntity]](
        path = _path,
        andExpect = Ok,
        headers = authHeaders(profileId),
        suppress = true
      )
      response.length should equal(n)
    }

    def markAllAsSeen(profileID: UUID): Unit = {
      var list = await(notifications.getAll(profileId))
      list.foreach(u => await(notifications.markAsSeen(u.id)))
    }

    it("should return only unseen notifications paginate") {
      for (i <- 1 to 10) await(addNotifToUser(profileId, i.toString, i.toString, "post_comment"))
      checkLengthOfRequest("/api/v1/notifications?unseen=true&per_page=10", 10)
      markAllAsSeen(profileId)
      checkLengthOfRequest("/api/v1/notifications?unseen=true&per_page=10", 0)
      for (i <- 11 to 20) await(addNotifToUser(profileId, i.toString, i.toString, "post_comment"))
      for (per_page <- 1 to 10) {
        var response = server.httpGetJson[List[NotificationEntity]](
          path = s"/api/v1/notifications?unseen=true&per_page=$per_page",
          andExpect = Ok,
          headers = authHeaders(profileId),
          suppress = true
        )
        response.length should equal(per_page)
        response.zipWithIndex.foreach { per =>
          per._1.message should equal((per._2 + 11).toString)
          per._1.avatar should equal(Some((per._2 + 11).toString))
          per._1.seen should equal(false)
        }
      }
      markAllAsSeen(profileId)
      checkLengthOfRequest("/api/v1/notifications?unseen=true", 0)
    }

    it("should return only unseen notifications") {
      for (i <- 1 to 10) await(addNotifToUser(profileId, i.toString, i.toString, "post_comment"))
      checkLengthOfRequest("/api/v1/notifications?unseen=true&per_page=10", 10)
      markAllAsSeen(profileId)
      checkLengthOfRequest("/api/v1/notifications?unseen=true", 0)
      for (i <- 11 to 20) await(addNotifToUser(profileId, i.toString, i.toString, "post_comment"))
      var response = server.httpGetJson[List[NotificationEntity]](
        path = "/api/v1/notifications?unseen=true&per_page=20",
        andExpect = Ok,
        headers = authHeaders(profileId),
        suppress = true
      )
      response.length should equal(10)
      response.zipWithIndex.foreach { per =>
        per._1.message should equal((per._2 + 11).toString)
        per._1.avatar should equal(Some((per._2 + 11).toString))
        per._1.seen should equal(false)
      }
    }

    it("should mark notification as seen") {
      await(addNotifToUser(profileId, "notif1", "profile1", "post_comment"))
      val notList = await(notifications.getAll(profileId))
      await(notifications.getAll(profileId)).count(_.seen) should equal(0)
      await(notifications.markAsSeen(notList.head.id))
      await(notifications.getAll(profileId)).count(_.seen) should equal(1)
    }

    it("should return exactly per_page notifications") {
      insertNotifs(profileId, 10)
      for (per_page <- 1 to 10) {
        checkLengthOfRequest(s"/api/v1/notifications?per_page=$per_page", per_page)
      }
    }

    it("should return empty request for changed_at at 0") {
      insertNotifs(profileId, 10)
      val response = server.httpGetJson[List[NotificationEntity]](
        path = "/api/v1/notifications?changed_at=0",
        andExpect = Ok,
        headers = authHeaders(profileId),
        suppress = true
      )
      response.length should equal(0)
    }

    it("should update created_at value of notification") {
      val notif = await(addNotifToUser(profileId, "notif", "profile", "post_comment"))
      notifications.updateCreateAt(notif.id, Instant.ofEpochSecond(0))
      val opt = await(notifications.getNotification(notif.id, profileId)).map(_.createdAt)
      opt should equal(Some(Instant.ofEpochSecond(0)))
    }

    it("should filter correctly the intervals (since, changed_at)") {
      val notif0 = await(addNotifToUser(profileId, "notif0", "profile0", "post_comment"))
      val notif1 = await(addNotifToUser(profileId, "notif1", "profile1", "post_comment"))
      val notif2 = await(addNotifToUser(profileId, "notif2", "profile2", "post_comment"))
      val notif3 = await(addNotifToUser(profileId, "notif3", "profile3", "post_comment"))
      val notif4 = await(addNotifToUser(profileId, "notif4", "profile4", "post_comment"))
      await(notificationsService.updateCreateAt(notif0.id, Instant.ofEpochSecond(0)))
      await(notificationsService.updateCreateAt(notif1.id, Instant.ofEpochSecond(1)))
      await(notificationsService.updateCreateAt(notif2.id, Instant.ofEpochSecond(2)))
      await(notificationsService.updateCreateAt(notif3.id, Instant.ofEpochSecond(3)))
      await(notificationsService.updateCreateAt(notif4.id, Instant.ofEpochSecond(4)))
      for (
        a <- notifications.getNotification(notif0.id, profileId);
        b <- notifications.getNotification(notif1.id, profileId);
        c <- notifications.getNotification(notif2.id, profileId);
        d <- notifications.getNotification(notif3.id, profileId);
        e <- notifications.getNotification(notif4.id, profileId)
      ) {
        a.map(_.createdAt) should equal(0)
        b.map(_.createdAt) should equal(1)
        c.map(_.createdAt) should equal(2)
        d.map(_.createdAt) should equal(3)
        e.map(_.createdAt) should equal(4)
        // should select 1, 2, 3 and 4
        checkLengthOfRequest("/api/v1/notifications?since=0&changed_at=5", 4)
        checkLengthOfRequest("/api/v1/notifications?since=0&changed_at=4", 3)
        checkLengthOfRequest("/api/v1/notifications?since=0&changed_at=3", 2)
        checkLengthOfRequest("/api/v1/notifications?since=0&changed_at=2", 1)
        checkLengthOfRequest("/api/v1/notifications?since=0&changed_at=1", 0)
        checkLengthOfRequest("/api/v1/notifications?since=1&changed_at=0", 0)

        checkLengthOfRequest("/api/v1/notifications?since=0&changed_at=5", 4)
        checkLengthOfRequest("/api/v1/notifications?since=1&changed_at=5", 3)
        checkLengthOfRequest("/api/v1/notifications?since=2&changed_at=5", 2)
        checkLengthOfRequest("/api/v1/notifications?since=3&changed_at=5", 1)
        checkLengthOfRequest("/api/v1/notifications?since=4&changed_at=5", 0)
        checkLengthOfRequest("/api/v1/notifications?since=6&changed_at=5", 0)
      }
    }
    it("should get notifications by id") {
      val notif = await(addNotifToUser(profileId, "notif1", "profile1", "post_comment"))
      val id = await(notifications.getNotification(notif.id, profileId)).map(_.id)
      id should equal(Some(notif.id))
    }

    it("should get notification response as seen") {
      val notif = await(addNotifToUser(profileId, "notif1", "profile1", "post_comment"))
      var response = server.httpPut(
        path = "/api/v1/notifications/" + notif.id,
        putBody = "{}",
        andExpect = Ok,
        headers = authHeaders(profileId),
        suppress = true
      )
    }

    it("should throw bad request when notification does not exist") {
      var response = server.httpPut(
        path = "/api/v1/notifications/1",
        putBody = "{}",
        andExpect = BadRequest,
        headers = authHeaders(profileId),
        suppress = true
      )
    }

    it("should throw bad request when trying to mark as seen a notification not owned by used") {
      val notif = await(addNotifToUser(UUID.randomUUID(), "notif", "profile", "post_comment"))
      val response = server.httpPut(
        path = "/api/v1/notifications/" + notif.id,
        putBody = "{}",
        andExpect = BadRequest,
        headers = authHeaders(profileId),
        suppress = true
      )
    }

    it("should correctly compute unseen notifications count") {
      val notif1 = await(addNotifToUser(profileId, "notif1", "profile1", "post_comment"))
      var response = server.httpGet(
        path = "/api/v1/notifications",
        andExpect = Ok,
        headers = authHeaders(profileId),
        suppress = true
      )
      assert(response.headerMap.contains("X-Notifications-Unseen"))
      response.headerMap.get("X-Notifications-Unseen") should equal(Some("1"))
      markAllAsSeen(profileId)
      response = server.httpGet(
        path = "/api/v1/notifications",
        andExpect = Ok,
        headers = authHeaders(profileId),
        suppress = true
      )
      assert(response.headerMap.contains("X-Notifications-Unseen"))
      response.headerMap.get("X-Notifications-Unseen") should equal(Some("0"))

    }

    it("should add header x-notification-unseen to response") {
      val response = server.httpGet(
        path = "/api/v1/notifications",
        andExpect = Ok,
        headers = authHeaders(profileId),
        suppress = true
      )
      assert(response.headerMap.contains("X-Notifications-Unseen"))
      val jsonResponse = server.httpGetJson[List[NotificationEntity]](
        path = "/api/v1/notifications",
        andExpect = Ok,
        headers = authHeaders(profileId),
        suppress = true
      )
      jsonResponse.length should equal(0)
    }

    it("should remove old unseen notifications") {
      val notif = await(addNotifToUser(profileId, "notif1", "profile1", "post_comment"))
      val _8daysAgo = Instant.now().minus(8, ChronoUnit.DAYS)
      await(notificationsService.updateCreateAt(notif.id, _8daysAgo))
      await(notificationsService.clearOldNotifications())
      checkLengthOfRequest("/api/v1/notifications", 0)
    }

    it("should not remove old seen notifications") {
      val notif = await(addNotifToUser(profileId, "notif1", "profile1", "post_comment"))
      val _8daysAgo = Instant.now().minus(8, ChronoUnit.DAYS)
      await(notificationsService.updateCreateAt(notif.id, _8daysAgo))
      await(notificationsService.markAsSeen(profileId, notif.id))
      await(notificationsService.clearOldNotifications())
      checkLengthOfRequest("/api/v1/notifications", 1)

    }
  }
}
