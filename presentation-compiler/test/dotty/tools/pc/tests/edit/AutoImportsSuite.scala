package dotty.tools.pc.tests.edit

import dotty.tools.pc.base.BaseAutoImportsSuite

import org.junit.Test

class AutoImportsSuite extends BaseAutoImportsSuite:

  @Test def `basic` =
    check(
      """|object A {
         |  <<Future>>.successful(2)
         |}
         |""".stripMargin,
      """|scala.concurrent
         |java.util.concurrent
         |""".stripMargin
    )

  @Test def `basic-edit` =
    checkEdit(
      """|package a
         |
         |object A {
         |  <<Future>>.successful(2)
         |}
         |""".stripMargin,
      """|package a
         |
         |import scala.concurrent.Future
         |
         |object A {
         |  Future.successful(2)
         |}
         |""".stripMargin
    )

  @Test def `basic-edit-comment` =
    checkEdit(
      """|/**
         | * @param code
         | * @return
         |*/
         |object A {
         |  <<Future>>.successful(2)
         |}
         |""".stripMargin,
      """|import scala.concurrent.Future
         |/**
         | * @param code
         | * @return
         |*/
         |object A {
         |  Future.successful(2)
         |}
         |""".stripMargin
    )

  @Test def `basic-edit-directive` =
    checkEdit(
      """|// using scala 35
         |// using something else
         |
         |object A {
         |  <<Future>>.successful(2)
         |}
         |""".stripMargin,
      """|// using scala 35
         |// using something else
         |import scala.concurrent.Future
         |
         |object A {
         |  Future.successful(2)
         |}
         |""".stripMargin
    )

  @Test def `scala-cli-sc-using-directives` =
    checkEdit(
      """|object main {
         |/*<script>*///> using scala "3.1.3"
         |
         |object x {
         |  <<Try>>("1".toString)
         |}
         |}
         |
         |""".stripMargin,
      """|object main {
         |/*<script>*///> using scala "3.1.3"
         |import scala.util.Try
         |
         |object x {
         |  Try("1".toString)
         |}
         |}
         |""".stripMargin,
      filename = "A.sc.scala"
    )

  @Test def `symbol-no-prefix` =
    checkEdit(
      """|package a
         |
         |object A {
         |  val uuid = <<UUID>>.randomUUID()
         |}
         |""".stripMargin,
      """|package a
         |
         |import java.util.UUID
         |
         |object A {
         |  val uuid = UUID.randomUUID()
         |}
         |""".stripMargin
    )

  @Test def `symbol-prefix-existing` =
    checkEdit(
      """|package a
         |
         |object A {
         |  val uuid = <<UUID>>.randomUUID()
         |}
         |""".stripMargin,
      """|package a
         |
         |import java.util.UUID
         |
         |object A {
         |  val uuid = UUID.randomUUID()
         |}
         |""".stripMargin
    )

  @Test def `symbol-prefix` =
    checkEdit(
      """|package a
         |
         |object A {
         |  val l : <<Map>>[String, Int] = ???
         |}
         |""".stripMargin,
      """|package a
         |
         |import java.{util => ju}
         |
         |object A {
         |  val l : ju.Map[String, Int] = ???
         |}
         |""".stripMargin
    )

  @Test def `interpolator-edit` =
    checkEdit(
      """|package a
         |
         |object A {
         |  val l = s"${<<Seq>>(2)}"
         |}
         |""".stripMargin,
      """|package a
         |
         |import scala.collection.mutable
         |
         |object A {
         |  val l = s"${mutable.Seq(2)}"
         |}
         |""".stripMargin
    )

  @Test def `package-object` =
    checkEdit(
      """|
         |package object metals{
         |  object ABC
         |}
         |object Main{
         | val obj = <<ABC>>
         |}
         |""".stripMargin,
      """|import metals.ABC
         |
         |package object metals{
         |  object ABC
         |}
         |object Main{
         | val obj = ABC
         |}
         |""".stripMargin
    )

  @Test def `import-inside-package-object` =
    checkEdit(
      """|package a
         |
         |package object b {
         |  val l = s"${<<ListBuffer>>(2)}"
         |}
         |""".stripMargin,
      """|package a
         |
         |import scala.collection.mutable.ListBuffer
         |
         |package object b {
         |  val l = s"${ListBuffer(2)}"
         |}
         |""".stripMargin
    )

  @Test def `multiple-packages` =
    checkEdit(
      """|package a
         |package b
         |package c
         |
         |object A {
         |  val l = s"${<<ListBuffer>>(2)}"
         |}
         |""".stripMargin,
      """|package a
         |package b
         |package c
         |
         |import scala.collection.mutable.ListBuffer
         |
         |object A {
         |  val l = s"${ListBuffer(2)}"
         |}
         |""".stripMargin
    )

  @Test def `multiple-packages-existing-imports` =
    checkEdit(
      """|package a
         |package b
         |package c
         |
         |import scala.concurrent.Future
         |
         |object A {
         |  val l = s"${<<ListBuffer>>(2)}"
         |}
         |""".stripMargin,
      """|package a
         |package b
         |package c
         |
         |import scala.concurrent.Future
         |import scala.collection.mutable.ListBuffer
         |
         |object A {
         |  val l = s"${ListBuffer(2)}"
         |}
         |""".stripMargin
    )

  @Test def `import-in-import` =
    checkEdit(
      """|package inimport
         |
         |object A {
         |  import <<ExecutionContext>>.global
         |}
         |""".stripMargin,
      """|package inimport
         |
         |object A {
         |  import scala.concurrent.ExecutionContext.global
         |}
         |""".stripMargin
    )

  @Test def `first-auto-import-amm-script` =
    checkAmmoniteEdit(
      ammoniteWrapper(
        """|
           |val p: <<Path>> = ???
           |""".stripMargin
      ),
      ammoniteWrapper(
        """|import java.nio.file.Path
           |
           |val p: Path = ???
           |""".stripMargin
      )
    )

  @Test def `second-auto-import-amm-script` =
    checkAmmoniteEdit(
      ammoniteWrapper(
        """import java.nio.file.Files
          |val p: <<Path>> = ???
          |""".stripMargin
      ),
      ammoniteWrapper(
        """import java.nio.file.Files
          |import java.nio.file.Path
          |val p: Path = ???
          |""".stripMargin
      )
    )

  @Test def `amm-objects` =
    checkAmmoniteEdit(
      ammoniteWrapper(
        """|
           |object a {
           |  object b {
           |    val p: <<Path>> = ???
           |  }
           |}
           |""".stripMargin
      ),
      ammoniteWrapper(
        """|import java.nio.file.Path
           |
           |object a {
           |  object b {
           |    val p: Path = ???
           |  }
           |}
           |""".stripMargin
      )
    )

  @Test def `first-auto-import-amm-script-with-header` =
    checkAmmoniteEdit(
      ammoniteWrapper(
        """|// scala 2.13.1
           |
           |val p: <<Path>> = ???
           |""".stripMargin
      ),
      ammoniteWrapper(
        """|// scala 2.13.1
           |import java.nio.file.Path
           |
           |val p: Path = ???
           |""".stripMargin
      )
    )

  private def ammoniteWrapper(code: String): String =
    // Vaguely looks like a scala file that Ammonite generates
    // from a sc file.
    // Just not referencing any Ammonite class, that we don't pull
    // in the tests here.
    s"""|package ammonite
        |package $$file.`auto-import`
        |import _root_.scala.collection.mutable.{
        |  HashMap => MutableHashMap
        |}
        |
        |object test{
        |/*<start>*/
        |$code
        |}
        |""".stripMargin

  // https://dotty.epfl.ch/docs/internals/syntax.html#soft-keywords
  @Test
  def `soft-keyword-check-test` =
    List(
      "infix",
      "inline",
      "opaque",
      "open",
      "transparent",
      "as",
      "derives",
      "end",
      "extension",
      "throws",
      "using"
    ).foreach(softKeywordCheck)

  private def softKeywordCheck(keyword: String) =
    checkEdit(
      s"""|
          |object $keyword{ object ABC }
          |object Main{ val obj = <<ABC>> }
          |""".stripMargin,
      s"""|import $keyword.ABC
          |
          |object $keyword{ object ABC }
          |object Main{ val obj = ABC }
          |""".stripMargin
    )
