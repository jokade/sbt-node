//     Project: SBT NPM
//      Module:
// Description:
package de.surfice.sbtnpm.utils

import sbt.Logger

import scala.language.implicitConversions

sealed trait JsonNode

object JsonNode {

  protected[this] sealed trait Value[T] extends JsonNode {
    def value: T
  }
  case class Str(value: String) extends Value[String]
  case class Bool(value: Boolean) extends Value[Boolean]
  sealed trait Num[T] extends Value[T]
  object Num {
    def apply(int: Int) = INum(int)
    def apply(dbl: Double) = DNum(dbl)
  }
  case class DNum(value: Double) extends Num[Double]
  case class INum(value: Int) extends Num[Int]

  implicit def strToNode(value: String): JsonNode = Str(value)
  implicit def boolToNoe(value: Boolean): JsonNode = Bool(value)
  implicit def numToNode(value: Double): JsonNode = Num(value)

  implicit def symProp(kv: (Symbol,JsonNode)): (String,JsonNode) = kv._1.name -> kv._2
  implicit def strProp(kv: (Symbol,String)): (String,Str) = kv._1.name -> Str(kv._2)
  implicit def boolProp(kv: (Symbol,Boolean)): (String,Bool) = kv._1.name -> Bool(kv._2)
  implicit def dnumProp(kv: (Symbol,Double)): (String,DNum) = kv._1.name -> Num(kv._2)
  implicit def inumProp(kv: (Symbol,Int)): (String,INum) = kv._1.name -> Num(kv._2)
//  implicit def nodesToObj(values: Iterable[(String,JsonNode)]): Obj = Obj(values)
  implicit def iterable(values: Iterable[(String,Any)]): Iterable[(String,JsonNode)] = values map {
    case (key,str:String) => (key,Str(str))
    case (key,bool:Boolean) => (key,Bool(bool))
    case (key,int:Int) => (key,INum(int))
    case (key,dbl:Double) => (key,DNum(dbl))
  }

  case class Obj(nodes: Iterable[(String,JsonNode)]) extends JsonNode
  object Obj {
    def apply(nodes: (String,JsonNode)*): Obj = Obj(nodes)
  }

  case class Arr(values: Iterable[JsonNode]) extends JsonNode
  object Arr {
    def apply(values: JsonNode*): Arr = Arr(values)
  }

  implicit final class RichJsonNode(val node: JsonNode) extends AnyVal {
    def toJson: String = {
      val w = new JsonWriter.StringWriter
      w.write(node)
      w.builder.toString()
    }
  }
}

trait JsonWriter {
  def write(node: JsonNode, prefix: String, suffix: String): Unit
}

object JsonWriter {
  import JsonNode._
  class StringWriter extends JsonWriter {
    val builder = StringBuilder.newBuilder

    override def write(node: JsonNode, prefix: String = "", suffix: String = ""): Unit = node match {
      case Str(value) =>
        builder ++= "'" + value + "'" + suffix
      case Bool(value) =>
        builder ++= value.toString + suffix
      case INum(value) =>
        builder ++= value + suffix
      case DNum(value) =>
        builder ++= value + suffix
      case Obj(nodes) if nodes.isEmpty =>
        builder ++= "{}"
      case Obj(nodes) =>
        builder ++= "{\n"
        val incPrefix = prefix + "  "
        nodes.zipWithIndex.foreach{ p =>
          val kn = p._1
          if(p._2 > 0)
            builder ++= ",\n"
          builder ++= incPrefix + "'" + kn._1 + "': "
          write(kn._2,incPrefix)
        }
        builder ++= "\n" + prefix + "}"
        builder ++= suffix
      case Arr(values) =>
        builder ++= "["
        values.headOption.foreach(write(_,prefix,suffix))
        values.tail.foreach {node =>
          builder ++= ", "
          write(node,prefix,suffix)
        }
        builder ++= "]"
    }
  }
}

abstract class JsonFile {
  def path: sbt.File
  def json: JsonNode
  def writeFile(overrideFile: Boolean = true)(implicit logger: Logger): Unit = {
    logger.debug(s"writing $path")
    sbt.IO.write(path,json.toJson)
  }
}