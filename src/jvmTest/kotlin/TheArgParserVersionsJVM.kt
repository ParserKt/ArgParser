import org.junit.Test
import org.parserkt.argp.*
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

// https://github.com/DanielScholzde/KArgParser#kargparser
class KArgParserExample1: ArgParser2<Int, String>(
  arg<Int>("foo", "Description for foo", "n") { it.toInt() },
  arg("bar", "Description for bar", "str")
) {
  @Test
  fun main() {
    run("--bar Penny --foo 42".splitArgv()).run {
      assertEquals(42, tup.e1.get())
      assertEquals("Penny", tup.e2.get())
    }
    assertFails { run("--foo 42".splitArgv()) }
  }
}

object KArgParserExample2 {
  object Parser: ArgParser1<Unit>(
    noArg, arg("ignoreCase", "Ignore case when comparing file contents") {"C"},
    items = listOf(arg("sourceFile", ""), arg("targetFile", ""))
  ) {
  }
  fun compareFiles(file1: File, file2: File, ignoreCase: Boolean) { println("diff $file1 $file2 ${"-i".showIf(ignoreCase)}") }
  fun findDuplicates(directories: List<File>, ignoreCase: Boolean) { println("find_dup $directories $ignoreCase") }
}

// https://github.com/Kotlin/kotlinx-cli#example
object KotlinXCliExample: ArgParser2<KotlinXCliExample.Format, Double>(
  arg("format f", "Format for output file", "fmt_output").options(Format.CSV),
  arg("eps", "Observational error", "sec", 0.01) { it.toDouble() },
  arg("debug d", "Turn on debug mode"), helpArg,
  items = listOf(
    argFileDI("input", "Input file"),
    argFileDI("output", "Output file name")),
  more = listOf(
    arg("sf", "Format as string for output file", "fmt_output", "csv", repeatable = true).checkOptions("html", "csv", "pdf")
  )
) {
  enum class Format { HTML, CSV, PDF }
  override fun toString() = toString(prog = "example", colon = " -> ")
}

class TheArgParserVersionsJVM {
  @Test fun kotlinXCli() {
    assertEquals("""
      Usage: example (-format -f fmt_output) (-eps sec) [-debug -d] [-h -help] {-sf fmt_output} <input> <output>
        -format -f -> Format for output file in html, csv, pdf (default CSV)
        -eps -> Observational error (default 0.01)
        -debug -d -> Turn on debug mode
        -h -help -> print this help
        -sf -> Format as string for output file in html, csv, pdf (default csv)
        input -> Input file
        output -> Output file name
      options can be mixed with items.

    """.trimIndent(), KotlinXCliExample.toString())
    val res = KotlinXCliExample.run("a.csv b.pdf -d --eps 0.1")
    res.run { val named = named!!
      assertEquals("a.csv", named.getAs<File>("input").name)
      assertEquals("b.pdf", named.getAs<File>("output").name)
      assertEquals("d", flags)
      assertEquals(0.1, tup.e2.get())
    }
  }
  @Suppress("unused_value") @Test fun kotlinXCliByParse() {
    val ap = ArgParserBy("cli")
    var format by ap+arg("format f", "Format for output file", "fmt_output").options(KotlinXCliExample.Format.CSV)
    var sf by (ap+arg("sf", "Format as string for output file", "fmt_output", "csv").checkOptions("csv", "html", "pdf")).multiply()
    var epSec by ap+arg("eps", "Observational error", "sec", 0.01) { it.toDouble() }
    var debug by ap.flag("debug d", "Turn on debug mode")
    var input by ap.item(argFileDI("input", "Input file"))
    var output by ap.item(argFileDI("output", "output file"))
    assertEquals(emptyList(), ap.run("-format csv -sf pdf -sf html -d a.csv b.pdf".splitArgv()))
    assertEquals(KotlinXCliExample.Format.CSV, format)
    assertEquals(listOf("pdf", "html"), sf)
    assertEquals("a.csv", input.name)
    assertEquals("b.pdf", output.name)
    assertEquals(0.01, epSec)
    assertTrue(debug)
    format = KotlinXCliExample.Format.PDF
    sf = sf + listOf("csv")
    epSec = 1.0
    debug = false
    input = File("new.csv") ; output = File("new.pdf")
    assertEquals("-format pdf -sf pdf -sf html -sf csv -eps 1.0 new.csv new.pdf".split(), ap.backRun().toList())
  }
  // https://github.com/xenomachina/kotlin-argparser-example/blob/master/src/main/kotlin/com/xenomachina/argparser/example/Main.kt
  @Test fun kotlinArgParserExample() {
    val ap = ArgParserBy("example")
    val verbose by ap.flag("verbose v", "enable verbose mode")
    val name by ap+arg("name N", "name of the widget", "","John Doe")
    val size by ap+argInt("size s", "size of the plumbus")
    val includeDirs by (ap+argFileD("I", "directory to search for header files")).multiply()
    val optimizeFor by ap+arg("optimize-for", "what to optimize for", "").options(OptimizationMode.FAST,
      "good" to OptimizationMode.GOOD,
      "fast" to OptimizationMode.FAST,
      "cheap" to OptimizationMode.CHEAP)
    val sources by ap.items(argFileDI("...", "source filenames"))
    val dest by ap.item(argFileDI("DEST", "destination filename"))
    ap.autoSplit("I")
    ap.asParser().run("i1 i2 o -size 0")
    assertEquals(emptyList(), ap.run("a b c d -v -Ix -Iy -size 23 -optimize-for cheap".splitArgv()))
    assertEquals(listOf("a","b","c"), sources.map { it.name })
    assertEquals("d", dest.name)
    assertEquals(listOf(true, "John Doe", 23, listOf("x", "y"), OptimizationMode.CHEAP), listOf(verbose, name, size, includeDirs.map { it.name }, optimizeFor))
  }
  enum class OptimizationMode { GOOD, FAST, CHEAP }
}
