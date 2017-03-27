package com.kuende.backendapp.api.filters

import com.google.inject.Inject
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.inject.Logging
import com.twitter.util.Future

object UserContext {
  private val UserField = Request.Schema.newField[String]()

  implicit class UserContextSyntax(val request: Request) extends AnyVal {
    def userId: String = request.ctx(UserField)
  }

  def setUserId(request: Request): Option[String] = {
    request.cookies.get("user_id") match {
      case None => Some("user is not logged in, please set cookie user_id to some value")
      case Some(cookie) => {
        request.ctx.update(UserField, cookie.value)
        None
      }
    }
  }
}

class HasUserFilter @Inject()(responseBuilder: ResponseBuilder) extends SimpleFilter[Request, Response] with Logging {
  override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    UserContext.setUserId(request) match {
      case None => service(request)
      case Some(errorMessage) => {
        Future.value(responseBuilder.badRequest(errorMessage))
      }
    }
  }
}
