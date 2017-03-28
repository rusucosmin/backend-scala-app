package com.kuende.backendapp.api

import com.google.inject.Inject
import com.kuende.backendapp.consumers.{NotificationPublishConsumer, NotificationPublishRequest}
import com.twitter.finatra.http.Controller
import sun.misc.BASE64Decoder

case class Message(data: String, attributes: Map[String, String])
case class MessageWrapper(subscription: String, message: Message)

class PubsubAPI @Inject()(npc: NotificationPublishConsumer) extends Controller {
  post("/_pubsub/task/main.notifications.publish") { ctx: MessageWrapper =>
    val jsonData = decodePubsubMessage(ctx)

    npc.perform(NotificationPublishRequest(jsonData))
  }

  private def decodePubsubMessage(ctx: MessageWrapper): String = {
    val base64Data = ctx.message.data
    val jsonData   = new BASE64Decoder().decodeBuffer(base64Data)
    new String(jsonData)
  }
}
