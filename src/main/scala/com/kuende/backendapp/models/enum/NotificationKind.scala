package com.kuende.backendapp.models.enum

import com.kuende.backendapp.util.{EnumCompanion, EnumValue}

sealed abstract class NotificationKind(dbValue: Byte, publicValue: String) extends EnumValue(dbValue, publicValue)

object NotificationKind extends EnumCompanion[NotificationKind] {
  case object PostRating   extends NotificationKind(1, "post_comment")
  case object PostComment  extends NotificationKind(2, "post_rating")
  case object CommentReply extends NotificationKind(3, "comment_reply")

  val types: List[NotificationKind] = List(PostRating, PostComment, CommentReply)
}
