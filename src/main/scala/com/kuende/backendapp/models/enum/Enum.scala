package com.kuende.backendapp.util

import io.getquill.MappedEncoding

class EnumException(message: String) extends Exception(message)

class EnumValue(val dbValue: Byte, val publicValue: String)

trait EnumCompanion[T <: EnumValue] {
  val types: List[T]

  def apply(id: Byte): T = {
    def loop(remain: List[T]): T = remain match {
      case head :: tail => if (head.dbValue == id) head else loop(tail)
      case Nil          => throw new EnumException(s"Invalid enum value $id")
    }
    loop(types)
  }

  def apply(str: String): T = {
    val dcase = str.toLowerCase
    def loop(remain: List[T]): T = remain match {
      case head :: tail => if (head.publicValue == dcase) head else loop(tail)
      case Nil          => throw new EnumException(s"Invalid enum value $str")
    }
    loop(types)
  }

  def encoder = {
    MappedEncoding[T, Byte](_.dbValue)
  }

  def decoder = {
    MappedEncoding[Byte, T](apply(_))
  }
}
