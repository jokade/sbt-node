//     Project: SBT NPM
//      Module:
// Description:
package de.surfice.sbtnpm.utils.test

import de.surfice.sbtnpm.utils.{JsonNode, JsonWriter}
import utest._

object JsonTests extends TestSuite {
  import JsonNode._
  val tree = Obj(
    'obj -> Obj(
      'str -> "String",
      'empty -> Obj(),
      'bool -> true,
      'int -> 1
    ),
    'arr -> Arr("Hello",42.0),
    'bool -> false
  )

  val tests = TestSuite {
    'toJson-{
//      println(tree.toJson)
      val res =  """{
          |  'obj': {
          |    'str': 'String',
          |    'empty': {},
          |    'bool': true,
          |    'int': 1
          |  },
          |  'arr': ['Hello', 42.0],
          |  'bool': false
          |}""".stripMargin
      assert(res == tree.toJson )
    }
    'iterable-{
      val obj = Obj(Seq(
        "str" -> "String",
        "bool" -> true,
        "int" -> 42,
        "dbl" -> 123.456
      ))
      assert(obj.toJson ==
        """{
          |  'str': 'String',
          |  'bool': true,
          |  'int': 42,
          |  'dbl': 123.456
          |}""".stripMargin)
    }
  }
}
