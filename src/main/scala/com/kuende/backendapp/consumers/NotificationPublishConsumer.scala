package com.kuende.backendapp.consumers

import com.google.inject.{Inject, Singleton}
import com.kuende.backendapp.models.Notification
import com.kuende.backendapp.services.NotificationService
import com.twitter.util.Future
import org.json4s._
import org.json4s.jackson.JsonMethods._

case class NotificationPublishRequest(message: String, avatar: Option[String], notifiableRefId: String, kind: String)

object NotificationPublishRequest {
  implicit val formats = DefaultFormats

  def apply(jsonData: String): NotificationPublishRequest = {
    val json = parse(jsonData).camelizeKeys
    json.extract[NotificationPublishRequest]
  }
}

@Singleton
class NotificationPublishConsumer @Inject()(notificationService: NotificationService) {
  def perform(message: NotificationPublishRequest): Future[Notification] = {
    notificationService.create(message)
  }
}
