package com.kuende.backendapp.models.mysql

import java.sql.Timestamp
import java.time.Instant
import java.util.TimeZone

import com.kuende.backendapp.util.MysqlContext
import com.twitter.finagle.mysql.{Parameter, TimestampValue}

trait DateEncoding {
  val db: MysqlContext
  import db._

  /**
    * Neither `java.util.Date` nor `quill` support <, >=...
    * So we manually interpolates sql/cql via quill's infix mechanism
    * @see http://getquill.io/#extending-quill-infix
    */
  protected implicit class ForInstant(f: Instant) {
    def <(right: Instant)  = quote(infix"$f < $right".as[Boolean])
    def >(right: Instant)  = quote(infix"$f > $right".as[Boolean])
    def <=(right: Instant) = quote(infix"$f <= $right".as[Boolean])
    def >=(right: Instant) = quote(infix"$f >= $right".as[Boolean])
  }

  protected val timestampValue =
    new TimestampValue(
      TimeZone.getTimeZone("UTC"),
      TimeZone.getTimeZone("UTC")
    )

  implicit val instantEncoder: Encoder[Instant] = encoder[Instant] { (i: Instant) =>
    {
      assert(i != null, "timestamp couldn't be null")
      timestampValue.apply(Timestamp.from(i)): Parameter
    }
  }

  implicit val instantDecoder: Decoder[Instant] = decoder[Instant] {
    case timestampValue(v) => v.toInstant
  }
}
