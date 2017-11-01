package com.kuende.backendapp.consumers

import com.google.inject.{Inject, Singleton}
import com.kuende.backendapp.services.NotificationService

@Singleton
class NotificationClearConsumer @Inject()(notificationService: NotificationService) {
  def perform() = {
    notificationService.clearOldNotifications()
  }
}
