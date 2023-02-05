package dotty.tools.dotc.coverage

import org.junit.Test
import org.junit.AfterClass
import org.junit.Assert.*
import org.junit.experimental.categories.Category
import dotty.{BootstrappedOnlyTests, Properties}
import dotty.tools.vulpix.*
import dotty.tools.vulpix.TestConfiguration.*
import dotty.tools.dotc.Main
import dotty.tools.dotc.reporting.TestReporter

import java.nio.file.{FileSystems, Files, Path, Paths, StandardCopyOption}
import scala.jdk.CollectionConverters.*
import scala.util.Properties.userDir
import scala.language.unsafeNulls
import scala.collection.mutable.Buffer
import dotty.tools.dotc.util.DiffUtil

@Category(Array(classOf[BootstrappedOnlyTests]))
class CoverageTests:
  import CoverageTests.{*, given}

  private val scalaFile = FileSystems.getDefault.getPathMatcher("glob:**.scala")
  private val rootSrc = Paths.get(userDir, "tests", "coverage")

  @Test
  def checkCoverageStatements(): Unit =
    checkCoverageIn(rootSrc.resolve("pos"), false)

  @Test
  def checkInstrumentedRuns(): Unit =
    checkCoverageIn(rootSrc.resolve("run"), true)

  def checkCoverageIn(dir: Path, run: Boolean)(using TestGroup): Unit =
    /** Converts \\ (escaped \) to / on windows, to make the tests pass without changing the serialization. */
    def fixWindowsPaths(lines: Buffer[String]): Buffer[String] =
      val separator = java.io.File.separatorChar
      if separator == '\\' then
        val escapedSep = "\\\\"
        lines.map(_.replace(escapedSep, "/"))
      else
        lines
    end fixWindowsPaths

    def runOnFile(p: Path): Boolean =
      scalaFile.matches(p) &&
      (Properties.testsFilter.isEmpty || Properties.testsFilter.exists(p.toString.contains))

    Files.walk(dir).filter(runOnFile).forEach(path => {
      val fileName = path.getFileName.toString.stripSuffix(".scala")
      val targetDir = computeCoverageInTmp(path, dir, run)
      val targetFile = targetDir.resolve(s"scoverage.coverage")
      val expectFile = path.resolveSibling(s"$fileName.scoverage.check")
      if updateCheckFiles then
        Files.copy(targetFile, expectFile, StandardCopyOption.REPLACE_EXISTING)
      else
        val expected = fixWindowsPaths(Files.readAllLines(expectFile).asScala)
        val obtained = fixWindowsPaths(Files.readAllLines(targetFile).asScala)
        if expected != obtained then
          val instructions = FileDiff.diffMessage(expectFile.toString, targetFile.toString)
          fail(s"Coverage report differs from expected data.\n$instructions")

    })

  /** Generates the coverage report for the given input file, in a temporary directory. */
  def computeCoverageInTmp(inputFile: Path, sourceRoot: Path, run: Boolean)(using TestGroup): Path =
    val target = Files.createTempDirectory("coverage")
    val options = defaultOptions.and("-Ycheck:instrumentCoverage", "-coverage-out", target.toString, "-sourceroot", sourceRoot.toString)
    if run then
      val test = compileDir(inputFile.getParent.toString, options)
      test.checkRuns()
    else
      val test = compileFile(inputFile.toString, options)
      test.checkCompile()
    target

object CoverageTests extends ParallelTesting:
  import scala.concurrent.duration.*

  def maxDuration = 30.seconds
  def numberOfSlaves = 1

  def safeMode = Properties.testsSafeMode
  def testFilter = Properties.testsFilter
  def isInteractive = SummaryReport.isInteractive
  def updateCheckFiles = Properties.testsUpdateCheckfile
  def failedTests = TestReporter.lastRunFailedTests

  given summaryReport: SummaryReporting = SummaryReport()
  @AfterClass def tearDown(): Unit =
    super.cleanup()
    summaryReport.echoSummary()

  given TestGroup = TestGroup("instrumentCoverage")
