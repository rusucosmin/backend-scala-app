package com.kuende.backendapp.api.mappers

import com.google.inject.Inject
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.exceptions.ExceptionMapper
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.finatra.json.internal.caseclass.exceptions.CaseClassMappingException
import com.twitter.inject.Logging

import scala.collection.mutable

class CaseClassMappingExceptionMapper @Inject()(response: ResponseBuilder)
    extends ExceptionMapper[CaseClassMappingException]
    with Logging {

  override def toResponse(request: Request, ex: CaseClassMappingException): Response = {
    response
      .badRequest(
        Map(
          "errors"   -> getErrors(ex),
          "messages" -> getMessages(ex)
        ))
  }

  def getMessages(ex: CaseClassMappingException): Seq[String] = {
    ex.errors.flatMap { error =>
      error.path.names.map { name =>
        s"${name} ${error.reason.message}"
      }
    }
  }

  def getErrors(ex: CaseClassMappingException): mutable.Map[String, Seq[String]] = {
    val m = mutable.Map[String, Seq[String]]()

    ex.errors.foreach { error =>
      val errors = error.path.names.foreach { name =>
        m(name) = List(error.reason.message)
      }
    }

    m
  }
}
