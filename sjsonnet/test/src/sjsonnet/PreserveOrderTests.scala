package sjsonnet

import utest._

object PreserveOrderTests extends TestSuite {
  def eval(s: String, preserveOrder: Boolean) = {
    new Interpreter(
      SjsonnetMain.createParseCache(),
      Map(),
      Map(),
      DummyPath(),
      (_, _) => None,
      preserveOrder
    ).interpret(s, DummyPath("(memory)")) match {
      case Right(x) => x
      case Left(e) => throw new Exception(e)
    }
  }

  def tests = Tests {
    test("preserveOrder") {
      eval(
        """{
             "z": "z",
             "a": "a",
           }""", true).toString() ==> """{"z":"z","a":"a"}"""

      eval(
        """[
             {
               "a": "a"
             },
             {
               "z": "z",
               "a": "a"
             }
           ][1]""", true).toString() ==> """{"z":"z","a":"a"}"""

      eval(
        """[{b: null},
           {
             "z": null,
             "a": "2",
             "d": {},
             "b": "3"
           },
           []]""", true).toString() ==> """[{"b":null},{"z":null,"a":"2","d":{},"b":"3"},[]]"""

      eval(
        """{
             "z": "z",
             "a": [5, {
               "s": "s",
               c: "c"
             }],
           }""", true).toString() ==> """{"z":"z","a":[5,{"s":"s","c":"c"}]}"""

      eval(
        """{
             "z": "z",
             "a": "a",
           }""", false).toString() ==> """{"a":"a","z":"z"}"""

    }

    test("preserveOrderCombined") {
      eval(
        """{
             "z": "z",
             "a": "a",
           } + {
             "a": "a1",
             "z": "z1",
             "b": "b"
           }""", true).toString() ==> """{"z":"z1","a":"a1","b":"b"}"""

      eval(
        """{
             "z": "z",
             "a": "a",
           } + {
             "c": "c", // new, should be after z and a
             "a": "a1", // z and a should maintain previous order
             "z": "z1",
             "b": "b", // new, should be after c
             "q": "q" // new, should be after b
           }""", true).toString() ==> """{"z":"z1","a":"a1","c":"c","b":"b","q":"q"}"""
    }

    test("preserveOrderHidden") {
      eval(
        """{
             "z": "z",
             "a": "a",
             "b": "b"
           } + {
             "b": "b2",
             "a":: "hidden"
           }""", true).toString() ==> """{"z":"z","b":"b2"}"""
    }

    test("preserveOrderUnhidden") {
      eval(
        """{
             "z": "z",
             "a": "a",
             "b": "b"
           } + {
             "b": "b2",
             "a":: "hidden"
           } + {
             "a"::: "unhidden",
             "b": "b3"
           }""", true).toString() ==> """{"z":"z","a":"unhidden","b":"b3"}"""
    }

    test("preserveOrderMergePatch") {
      eval(
        """std.mergePatch({
             "z": "z",
             "a": "a",
             "b": "b"
           }, {
             "a": null
           })""", true).toString() ==> """{"z":"z","b":"b"}"""

      eval(
        """std.mergePatch({
             "z": "z",
             "a": "a",
             "b": "b"
           }, {
             "b": "b2",
             "a": null
           })""", true).toString() ==> """{"z":"z","b":"b2"}"""

      eval(
        """std.mergePatch({
             "z": "z",
             "a": "a",
             "b": "b"
           }, {
             "b": "b2",
             "z": "z2"
           })""", true).toString() ==> """{"z":"z2","a":"a","b":"b2"}"""
    }

    test("preserveOrderObjectFields") {
      eval(
        """std.objectFields({
             "z": "z",
             "a": "a",
             "b": "b"
           })""", true).toString() ==> """["z","a","b"]"""
    }

    test("preserveOrderObjectFieldsAll") {
      eval(
        """std.objectFieldsAll({
             "z": "z",
             "a": "a",
             "b": "b"
           } + {
             "c": "c",
             "b": "b2",
             "a":: "hidden",
           })""", true).toString() ==> """["z","a","b","c"]"""
    }

    test("preserveOrderMapWithKey") {
      eval(
        """std.mapWithKey(function(k, v) k + v,
           {
             "z": "1",
             "a": "2",
             "b": "3"
           })""", true).toString() ==> """{"z":"z1","a":"a2","b":"b3"}"""
    }

    test("preserveOrderPrune") {
      eval(
        """std.prune([{b: null},
           {
             "z": null,
             "a": "2",
             "d": {},
             "b": "3"
           },
           []])""", true).toString() ==> """[{"a":"2","b":"3"}]"""
    }

    test("preserveOrderManifestIni") {
      eval(
        """std.manifestIni({
             main: { b: "1", a: 2, c: true, e: null, d: [1, {"2": 2}, [3]], f: {"hello": "world"} },
             sections: {}
          })""", true) ==>
        ujson.Str("b = 1\na = 2\nc = true\ne = null\nd = 1\nd = {\"2\": 2}\nd = [3]\nf = {\"hello\": \"world\"}\n")
    }

    test("preserveOrderPython") {
      eval(
        """std.manifestPython({
             "z": "z",
             "a": "a",
             "b": true
           })""", true) ==> ujson.Str("""{"z": "z", "a": "a", "b": True}""")

      eval(
        """std.manifestPythonVars({
             "z": "z",
             "a": "a",
             "b": true
           })""", true) ==>
        ujson.Str(
          """z = "z"
            |a = "a"
            |b = True
            |""".stripMargin)
    }

    test("preserveOrderJsonEx") {
      eval(
        """std.manifestJsonEx({
             "z": "z",
             "a": "a",
             "b": true
           }, "  ")""", true) ==>
        ujson.Str(
          """{
            |  "z": "z",
            |  "a": "a",
            |  "b": true
            |}""".stripMargin)
    }

    test("preserveOrderYaml") {
      eval(
        """std.manifestYamlDoc({
             "z": "z",
             "a": [1, 2],
             "b": {
               "s": "s",
               "c": "c"
             }
           })""", true) ==>
        ujson.Str(
          """"z": "z"
            |"a":
            |- 1
            |- 2
            |"b":
            |  "s": "s"
            |  "c": "c"""".stripMargin)

      eval(
        """std.manifestYamlStream([5, {
             "z": "z",
             "a": [1, 2],
             "b": {
               "s": "s",
               "c": "c"
             }
           }])""", true) ==>
        ujson.Str(
          """---
            |5
            |---
            |"z": "z"
            |"a":
            |- 1
            |- 2
            |"b":
            |  "s": "s"
            |  "c": "c"
            |...
            |""".stripMargin)
    }

    test("preserveOrderXml") {
      eval(
        """std.manifestXmlJsonml([
            'a', { c: 'c', b: 'b' }
          ])""", true) ==>
        ujson.Str("""<a c="c" b="b"></a>""")
    }

    test("preserveOrderToString") {
      eval(
        """std.toString({
             "z": "1",
             "a": "2",
             "b": "3"
           })""", true) ==>
        ujson.Str("""{"z": "1", "a": "2", "b": "3"}""")
    }
  }
}