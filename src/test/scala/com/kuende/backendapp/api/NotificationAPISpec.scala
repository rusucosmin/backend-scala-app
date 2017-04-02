package com.kuende.backendapp.api

import java.util.UUID

import com.kuende.backendapp.api.entities.NotificationEntity
import com.kuende.backendapp.consumers.NotificationPublishRequest
import com.kuende.backendapp.models.Notifications
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
  }
}
