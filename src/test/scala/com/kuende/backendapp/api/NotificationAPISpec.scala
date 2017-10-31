package com.kuende.backendapp.api

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

    def addNotifToUser(profileId: UUID, _message: String = "msg", _avatar: String = "avatar", _kind: String = "kind"): Future[Unit] = {
      val notification = NotificationPublishRequest(
        message = _message,
        avatar = Some(_avatar),
        notifiableRefId = profileId.toString,
        kind = _kind
      )
      notificationsService.create(notification)
    }

    def insertNotifs(profileId: UUID, n: Int): Unit = {
      for (i <- 1 to n) addNotifToUser(profileId, i.toString, i.toString, "post_comment")
    }

    def checkLengthOfRequest(_path: String, per_page: Int): Unit = {
      var response = server.httpGetJson[List[NotificationEntity]](
        path = _path,
        andExpect = Ok,
        headers = authHeaders(profileId),
        suppress = true
      )
      response.length should equal(per_page)
    }

    it("should return only unseen notifications paginate") {
      for (i <- 1 to 20) addNotifToUser(profileId, i.toString, i.toString, "post_comment")
      for (per_page <- 1 to 10) {
        var response = server.httpGetJson[List[NotificationEntity]](
          path = s"/api/v1/notifications?unseen=true&per_page=$per_page",
          andExpect = Ok,
          headers = authHeaders(profileId),
          suppress = true
        )
        response.length should equal(per_page)
        for (i <- 0 until per_page) {
          response(i).message should equal((i + 10).toString)
          response(i).profile should equal((i + 10).toString)
          response(i).seen should equal(false)
        }
      }
    }

    it("should return only unseen notifications") {
      for (i <- 1 to 10) addNotifToUser(profileId, i.toString, i.toString, "post_comment")
      for (i <- 11 to 20) addNotifToUser(profileId, i.toString, i.toString, "post_comment")
      var response = server.httpGetJson[List[NotificationEntity]](
        path = "/api/v1/notifications?unseen=true&per_page=20",
        andExpect = Ok,
        headers = authHeaders(profileId),
        suppress = true
      )
      response.length should equal(10)
      for (i <- 0 until 10) {
        response(i).message should equal((i + 11).toString)
        response(i).profile should equal((i + 11).toString)
        response(i).seen should equal(false)
      }
    }

    it("should mark notification as seen") {
      addNotifToUser(profileId, "notif1", "profile1", "post_comment");
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
    }
  }
}
