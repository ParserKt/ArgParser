import org.junit.Test
import org.parserkt.argp.*
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

fun argFileD(name: String, help: String, param: String? = "path", default_value: File? = noFile, repeatable: Boolean = false)
  = argFile(name, help, param, default_value, repeatable, "")
internal fun argFileDI(name: String, help: String) = argFileD(name, help, param = null, default_value = null)

val myP = object: ArgParser4<String,Int,Unit,Unit>(
  arg("name", "name of the user", "", "Alice"),
  arg<Int>("count C", "number of the widgets", "", 1) { it.toInt() },
  noArg, noArg,
  moreArgs = listOf(
    arg("xxx", "added for rescue", "", ""),
    arg("map", "build map", "k v", repeatable = true, convert = multiParam { it[0] to it[1] })),
  itemArgs=listOf(arg("ah", "ah ha"), argFile("em", "emm", null, null, flags="")), itemMode=PositionalMode.MustBefore,
  autoSplit=listOf("name", "count"),
  flags=*arrayOf(arg("v", "enable verbose mode"))
) {
  override val prefixes: List<String> = listOf("--", "/", "-")

  override fun checkAutoSplitForName(name: String, param: String) {
    if (name == "name" && param == "Jerky") throw SwitchParser.ParseError("$param doesn't like been broken into parts")
  }
  override fun checkPrefixForName(name: String, prefix: String) {
    super.checkPrefixForName(name, prefix)
    if (name.startsWith("C") && prefix == "/") throw SwitchParser.ParseError("/C is good emotion, not parameter")
  }
  override fun rescuePrefix(name: String): Arg<*> = when {
    name == "add" -> arg("add", "that list", "value", repeatable = true)
    name.endsWith("=") -> {
      val dest = name.substring(0, name.length-1)
      arg(dest, "woc", "value")
    }
    '=' in name -> arg(name.substringBefore('='), "emm", default_value = name.substringAfter('='))
    else -> super.rescuePrefix(name)
  }
}

class ExtendArgParserTest: BaseArgParserTest<String, Int, Unit,Unit>(myP) {
  @Test fun readsPositional() {
    backP("hello emf --nameMike -v -count233").run {
      val (e1, e2) = tup
      assertEquals("hello", items[0])
      assertEquals("emf", named!!.getAs<File>("em").name)
      assertEquals("Mike", e1.get())
      assertEquals(233, e2.get())
      assertTrue('v' in flags)
    }
    backP("emm a -C80").run { assertEquals(80, tup.e2.get()) }
    assertFailMessage("too less heading items (all [ah, em]) (#1, arg 1 in -xx)", "-xx")
    assertFailMessage("too many items (all [ah, em]) (#3, arg 3 in x)", "a s x -nameJack")
    assertFailMessage("reading [hel, loo]: should all-before options (#5, arg 5 in loo)", "hel xx -count666 -v loo")
    assertFailMessage("reading [exx, x]: should all-before options (#4, arg 4 in x)", "exx xx -C61 x --name wtf y")
  }
  @Test fun fourCasesForMustBefore() {
    assertFailMessage("Jerky doesn't like been broken into parts (#3, arg 3 in --nameJerky)", "ya ka --nameJerky")
    assertFailMessage("/C is good emotion, not parameter (#8, arg 6 in /C19)", "eh ss -name Black -v /xxx a /C19")
    backP("wtf") // item only
    assertFailMessage("too less heading items (all [ah, em]) (#1, arg 1 in -name)", "-name Jessie emm")
    assertFailMessage("too less heading items (all [ah, em]) (#1, arg 1 in -name)", "-name Jake ehh -v")
  }
  @Test fun dynamicInterpret() {
    p("as aa --add 1 -add 2 -add 3").run { assertEquals("1 2 3".split(), named!!.getAsList("add")) }
    p("ds aa -ax= 232 -bx=333").run { val m=named!! ; assertEquals("232", m["ax"]) ; assertEquals("333", m["bx"]) }
    backP("mapping xs -map apple 苹果 -C80 -map pear 梨子 -map a b").apply {
      assertEquals(mapOf("apple" to "苹果", "pear" to "梨子", "a" to "b"), named!!.getAsList<Pair<String,String>>("map").toMap())
      assertEquals(80, tup.e2.get())
      assertEquals("mapping", items[0])
    }
  }
  @Test fun format() {
    assertEquals("""
      Usage: <ah> <em> (-name name) (-count -C count) [-v] (-xxx xxx) {-map k, v}
        -name: name of the user (default Alice)
        -count -C: number of the widgets (default 1)
        -v: enable verbose mode
        -xxx: added for rescue (default none)
        -map: build map
        ah: ah ha
        em: emm

    """.trimIndent(), myP.toString())
  }
}

val yourP = ArgParser4(
  arg("name N", "name of the user", "", "Duckling"),
  arg<Int>("count c", "number of widgets", "") { it.toInt() },
  arg<File>("I", "directory to search for header files", "file", repeatable = true) { File(it) },
  arg("mode", "mode of operation", "", "small").checkOptions("fast", "small", "quite"),
  itemArgs = listOf(arg("source",""), arg("dest","")), itemMode = PositionalMode.MustAfter,
  flags = *arrayOf(helpArg)
)

class ExtendArgParserTest1: BaseArgParserTest<String, Int, File, String>(yourP) {
  @Test fun readsPositional() {
    backP("-N mike --count 233 -I ArgParserTest.kt -I ParserTest.kt -mode fast fg hs").run {
      assertEquals(2, tup.e3.size)
      assertEquals("fast", tup.e4.get())
    }
    backP("-count 233 -mode quite sd bd").run {
      assertEquals("Duckling", tup.e1.get())
      assertEquals(listOf("sd", "bd"), items)
    }
    assertFailMessage("reading [a]: should all-after options (#2, arg 2 in -N)", "a -N make")
    assertFailMessage("reading [k]: should all-after options (#4, arg 3 in -c)", "-N make k -c 88 c d")
    assertFailMessage("too less items (all [source, dest])", "-c 233 sf")
    assertFailMessage("too many items (all [source, dest]) (#5, arg 4 in 996)", "--name doge 233 666 996")
    p("-h --help")
  }
  @Test fun format() {
    assertEquals("""
      Usage: (-name -N name) [-count -c count] {-I file} (-mode mode) [-h -help] <source> <dest>
        -name -N: name of the user (default Duckling)
        -count -c: number of widgets
        -I: directory to search for header files
        -mode: mode of operation in fast, small, quite (default small)
        -h -help: print this help

    """.trimIndent(), yourP.toString())
  }
}



// https://github.com/xenomachina/kotlin-argparser/issues/8
object AWKArgParser: ArgParser4<File, String, String, String>(
  argFile("file= f exec= E", "execute script file", "path", flags = ""),
  arg("field-separator= F", "set field separator", "fs", " \t"),
  arg("assign= v", "assign variable", "var=val", repeatable = true),
  arg("load= l", "load library", "lib", repeatable = true),
  flags = *defineFlags(
    "characters-as-bytes" to "b",
    "traditional" to "c",
    "copyright" to "C",
    "gen-pot" to "g",
    "bignum" to "M",
    "use-lc-numeric" to "N",
    "non-decimal-data" to "n",
    "optimize" to "O",
    "posix" to "P",
    "re-interval" to "r",
    "no-optimize" to "s",
    "sandbox" to "S",
    "lint-old" to "t"
  ) +arrayOf(helpArg, arg("version V", "print version") { println("GNU Awk 5.0.1, API: 3.0"); SwitchParser.stop() }),
  autoSplit = "F E v d D L l o p".split(),
  itemArgs = listOf(arg("0", "ss") {it}, arg("...","")), itemMode = PositionalMode.MustAfter,
  moreArgs = listOf(
    argFileD("dump-variables= d", "dump vars to file", "file"),
    argFileD("debug= D", "debug", "file"),
    arg("source= e", "execute source", "code", "emm"),
    argFileD("include= i", "include file", "file"),
    arg("lint= L", "lint level", "", "none").checkOptions("fatal", "invalid", "no-ext"),
    argFileD("pretty-print= o", "pretty print to", "file"),
    argFileD("profile= p", "use profile", "file"),
    arg("color", "colorize the output", "path", ""), //<v not good approach, try use dynamic arg rescuePrefix()
    *listOf("always", "auto", "never").map { arg("color=$it", "$it colorize the output", "path", "") }.toTypedArray()
  )
) {

  override fun checkAutoSplitForName(name: String, param: String)
    = if (name.length == 1 && name[0] !in "vfdDlLop") throw SwitchParser.ParseError("auto-split $param used in shorthands") else Unit

  override fun checkResult(result: ParseResult<File, String, String, String>) {
    if (result.tup.e1.get() == noFile && result.items.isEmpty()) throw SwitchParser.ParseError("no executable provided")
  }
}

class AWKArgParserTests: BaseArgParserTest<File, String, String, String>(AWKArgParser) {
  @Test fun fourCaseForMustAfter() {
    assertFailMessage("no executable provided", "-g")
    assertFailMessage("reading [a.awk]: should all-after options (#3, arg 3 in -g)", "z a.awk -g")
    assertFailMessage("reading [x.awk]: should all-after options (#3, arg 3 in -g)", "z x.awk -g a.awk")
    backP("z a.awk")
  }
  @Test fun itWorks() {
    backP("-field-separator=: print a.awk").run {
      assertEquals(":", tup.e2.get()) } // if f=noFile, items[0] = code.
    p("-p233 -lint=no-ext --pretty-print=f --posix -S a.awk hah.awk mam.awk").run {
      val named = this.named!!
      assertEquals("a.awk", named["0"])
      assertEquals(listOf("hah.awk", "mam.awk"), items)
      assertEquals("emm", named["source="])
      assertEquals("233", named.getAs<File>("profile=").name)
      assertEquals("no-ext", named.getAs<String>("lint="))
      assertEquals("PS", flags)
    }
    backP("-la -lb -va=2 -vb=2 -fa")
    assertFailMessage("auto-split : used in shorthands (#1, arg 1 in -F:)", "-F:")
  }
  @Test fun format() {
    assertEquals("""
      Usage: (-file= -f -exec= -E path) (-field-separator= -F fs) {-assign= -v var=val}
              {-load= -l lib} [-characters-as-bytes -b] [-traditional -c] [-copyright -C]
              [-gen-pot -g] [-bignum -M] [-use-lc-numeric -N] [-non-decimal-data -n]
              [-optimize -O] [-posix -P] [-re-interval -r] [-no-optimize -s]
              [-sandbox -S] [-lint-old -t] [-h -help] [-version -V] (-dump-variables= -d file)
              (-debug= -D file) (-source= -e code) (-include= -i file) (-lint= -L lint=)
              (-pretty-print= -o file) (-profile= -p file) (-color path) (-color=always path)
              (-color=auto path) (-color=never path) <0> <...>
        -file= -f -exec= -E: execute script file (default none)
        -field-separator= -F: set field separator (default  	)
        -assign= -v: assign variable
        -load= -l: load library
        -characters-as-bytes -b: characters as bytes
        -traditional -c: traditional
        -copyright -C: copyright
        -gen-pot -g: gen pot
        -bignum -M: bignum
        -use-lc-numeric -N: use lc numeric
        -non-decimal-data -n: non decimal data
        -optimize -O: optimize
        -posix -P: posix
        -re-interval -r: re interval
        -no-optimize -s: no optimize
        -sandbox -S: sandbox
        -lint-old -t: lint old
        -h -help: print this help
        -version -V: print version
        -dump-variables= -d: dump vars to file (default none)
        -debug= -D: debug (default none)
        -source= -e: execute source (default emm)
        -include= -i: include file (default none)
        -lint= -L: lint level in fatal, invalid, no-ext (default none)
        -pretty-print= -o: pretty print to (default none)
        -profile= -p: use profile (default none)
        -color: colorize the output (default none)
        -color=always: always colorize the output (default none)
        -color=auto: auto colorize the output (default none)
        -color=never: never colorize the output (default none)
        0: ss

    """.trimIndent(), p.toString())
  }
}
