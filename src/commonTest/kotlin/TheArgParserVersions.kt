import org.parserkt.argp.*
import kotlin.jvm.JvmStatic
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

// https://github.com/jshmrsn/karg#example-usage
/**
 * +Features: types other than [String]; `--help` override; custom terminating: [SwitchParser.stop]
 */
object KArgExample: ArgParser2<String, String>(
  arg("text-to-print text t", "Print this text."),
  arg("text-to-print-after", "If provided, print this text after the primary text."),
  arg("shout", "Print in all uppercase.") {"s"},
  items = listOf(arg("...", "rest to print"))
) {
  @JvmStatic
  fun main(vararg args: String) {
    val res = run(args)
    var output = res.tup.e1.get()
    res.items.forEach { output += " $it" }
    res.tup.e2.forEach { output += "\n$it" }
    println(output.let { if ('s' in res.flags) it.toUpperCase() else it })
  }
}



// https://github.com/st235/ArgsParser#usage-example
object ArgsParser: ArgParser3<Int, String, Int>(
  argInt("id", "ID"),
  arg("name", "Name"),
  argInt("age", "Age")
) {
  @JvmStatic fun main(vararg args: String) {
    val tup = run(args).tup
    println("""
      id: ${tup.e1}
      name: ${tup.e2}
      age: ${tup.e3}
    """.trimIndent())
  }
}

// https://github.com/dustinliu/argparse4k#usage
object ArgParse4KExamples: ArgParser1<String>(
  arg("foo", "fo fo fo"), // meta-var assignment is not supported for typed args
  arg("detached d", "fd sf"),
  arg("v", "help version") { SwitchParser.stop() },
  items = listOf(arg("container", "container name")) // TODO subParser ccc -v
) {
  override fun toString() = "testprog " + super.toString()
}

// https://github.com/tarantelklient/kotlin-argparser
class KotlinArgParserExamples: ArgParser2<String, String>(
  arg("arg1", "simple test argument", ""),
  arg("arg2", "test argument with default", "text", "hello world"),
  arg("flag", "flag argument")
)

// https://github.com/substack/minimist#example
object MiniMistExample: ArgParser4<Unit, Unit, Unit, Unit>(noArg, noArg, noArg, noArg, itemArgs = listOf(arg("...", "rest items")), moreArgs = emptyList()) {
  override fun rescuePrefix(name: String): Arg<*> {
    for (re in autoParam)
      re.find(name)?.let { val (k, v) = it.destructured ; return arg(k, "", null, v) }
    return if (name.matches(nameArg)) arg(name, name, "")
    else arg("flags", "", null, name, repeatable = true)
  }
  override fun checkPrefixForName(name: String, prefix: String) {
    super.checkPrefixForName(name, prefix)
    //if (prefix.length == 1 && name.length != 1 && !autoParam[1].matches(name) && '=' !in name) throw SwitchParser.ParseError("assign names like --name=value")
  }
  private val nameArg = Regex("""\w""")
  private val autoParam = listOf(Regex("""(\w+)=(\S+)"""), Regex("""(\w+)(\d+)"""))
}

class TheArgParserVersions {
  @Test fun miniMist() {
    val res = MiniMistExample.run("-a beep -b bop").named!!
    assertEquals("beep", res["a"])
    assertEquals("bop", res["b"])
    val res1 = MiniMistExample.run("-x 3 -y 4 -n5 -abc -def --beep=bop foo bar baz")
    assertEquals(mapOf("x" to "3", "y" to "4", "n" to "5", "beep" to "bop"), res1.named!!.map.filterValues { it.value != null }.mapValues { it.value.get() })
    assertEquals(listOf("abc", "def"), res1.named!!.getAsList("flags"))
    assertEquals("foo bar baz".split(" "), res1.items)
  }
  @Test fun kotlinXCliSubcmd() {
    val p = KotlinXCliSubcmdExample
    assertEquals("""
      Usage: [-output -o path]
      Subcommands: 
        help: show help for a sub-command
        summary: Calculate summary
        {-addendums n} [-invert -i]
        Options: 
          -addendums: Addendums
          -invert -i: Invert results
        mul: Multiply
        {-addendums n}
        Subcommands: 
          justice: Just an ice
          [-n n]
          Options: 
            -n: number
        Options: 
          -addendums: Addendums
      Options: 
        -output -o: Output file

    """.trimIndent(), p.toString(groups = mapOf("*" to "Options", "(subcmd)" to "Subcommands"), indent = " ", recursion = 2))
    p.run("help")
    val res = p.run("-o hello summary -i -addendums 12 -addendums 3")
    assertEquals("hello", res.tup.e1.get())
    assertEquals(-15, res.named!!.getAs("addendums"))
    assertFails { p.run("justice") }
    assertFails { p.run("mul") }
    val res1 = p.run("-o outs mul -addendums 25 -addendums 4")
    assertEquals(100, res1.named!!.getAs("addendums"))
  }

  data class ArgParser(val args: ArgArray) {
    fun <T> parseInto(type: (ArgParserBy) -> T): T {
      val ap = ArgParserBy("")
      val res = type(ap)
      ap.run(args) ; return res
    }
  }
  class MyArgs(ap: ArgParserBy) {
    val verbose by ap.flag("v", "enable verbose mode")
    val name by ap+arg("name", "name of the user", "")
    val count by ap+argInt("count", "number of the widgets")
    val source by ap.item(arg("source", "source filename"))
    val destination by ap.item(arg("dest", "destination filename"))
    val percentageSum by (ap+argInt("add-p", "add to percentages")).multiply { require(it.sum() == 100) ; it }
    val ap1 = ArgParserBy("wtf")
    val item by ap1.item(arg("apple", ""))
    init {
      ap.itemMode(PositionalMode.Disordered)
      ap.addSub("test sub-command", ap1)
    }
  }
  @Test fun argParserSimulatedDSL() {
    ArgParser("-name Jake --count 233 a b -add-p 50 -add-p 50".splitArgv()).parseInto(::MyArgs).run {
      assertEquals(listOf("Jake", 233, "a", "b"), listOf(name, count, source, destination))
    }
  }
}

object KotlinXCliSubcmdExample: ArgParser1<String>(
  arg("output o", "Output file", "path")
) {
  override fun toString() = toString(prog = "example")
  private val addNumbers = argInt("addendums", "Addendums", "n", repeatable = true)
  object Summary: ArgParser1<Int>(
    addNumbers,
    arg("invert i", "Invert results")
  ) {
    override fun checkResult(result: ParseResult<Int, Unit, Unit, Unit>) {
      val acc = result.tup.e1
      acc.value = acc.sum().let { if ('i' in result.flags) -it else it }
    }
  }
  object Multiply: ArgParser1<Int>(addNumbers) {
    override fun checkResult(result: ParseResult<Int, Unit, Unit, Unit>) {
      val acc = result.tup.e1
      acc.value = acc.fold(1) { n, d -> n*d }
    }
    init { addSub("justice", "Just an ice", ArgParser1(argInt("n", "number"))) }
  } //v [K/Native] probably bug: InvalidMutabilityException when calling outside receiver
  init { addHelpSubCommand() ; addSub("summary", "Calculate summary", Summary) ; addSub("mul", "Multiply", Multiply) }
}

// https://github.com/aPureBase/arkenv
// https://apurebase.gitlab.io/arkenv/guides/the-basics/#profiles is not supported. impl it yourself
object ArkenvExample: ArgParser4<String, Int, Int, List<String>>(
  arg("country", "").getEnv(),
  argInt("port p this-can-be-set-via-env", "An Int with a default value and custom names", "").env(),
  argInt("null-int", "A nullable Int, which doesn't have to be declared", "n", 0).env(),
  arg<List<String>>("mapped", "Complex types can be achieved with a mapping", "names") { it.split("|") },
  arg("bool", "A bool, which will be false by default")
) // Tuple by idx delegate is not possible, since this project unlinked from funcly-ParserKt Tuple4

// https://github.com/airlift/airline
class AirLineExample {
  @Test fun main() {
    assertEquals("""
      Usage: git
      the stupid content tracker
        help: show help for a sub-command
        add: Add file contents to the index
        {-p glob} [-i]
        Options: 
            -p: Patterns of files to be added
            -i: Add modified contents interactively.
        remote: Manage set of tracked repositories
          show: Gives some information about the remote <name>
          [-n] <name>
          Options: 
              -n: Do not query remote heads
            name: Remote to show
          add: Adds a remote
          [-t] <...>
          Options: 
              -t: Track only a specific branch
            ...: Remote repository to add
    """.trimIndent()+"\n    ", Git.toString())
    Git.run("add -p file")
    Git.run("remote add origin git@github.com:airlift/airline.git")
    Git.run("-v remote show origin")
    //Git.run("-v remote show orig -n -g")
    assertEquals("""
      Usage: pint (-count -c num) [-h -help]
      network test utility
        -count -c: Send count packets (default 1)
        -h -help: print this help

    """.trimIndent(), Ping.toString())
    Ping.run("-c 5")
    Ping.run("--help")
  }
  abstract class GitCmd<A>(param: Arg<A>, vararg flags: Arg<*>, items: List<Arg<*>> = emptyList()): ArgParser1<A>(param,
    flags = *(flags.toList()+arg("v", "Verbose mode")).toTypedArray(), items = items)
  object Git: GitCmd<Unit>(noArg) {
    override fun toString() = toString(prog = "git", prologue = "the stupid content tracker\n",
      groups = mapOf("*" to "Options", "v" to "-")).replace(Regex("""\s*(\[-v])|(options can be mixed with items\.)\s*"""), "")
    init {
      addHelpSubCommand("git")
      addSub("add", "Add file contents to the index", Add)
      addSub("remote", "Manage set of tracked repositories", Remote)
    }
    object Add: GitCmd<String>(
      arg("p", "Patterns of files to be added", "glob", repeatable = true),
      arg("i", "Add modified contents interactively.")
    )
    object Remote: GitCmd<Unit>(noArg) {
      init {
        addSub("show", "Gives some information about the remote <name>", Show)
        addSub("add", "Adds a remote", Add)
      }
      object Show: GitCmd<Unit>(
        noArg, arg("n", "Do not query remote heads") { throw IllegalArgumentException("ee") },
        items = listOf(arg("name", "Remote to show"))
      )
      object Add: GitCmd<Unit>(
        noArg, arg("t", "Track only a specific branch"),
        items = listOf(arg("...", "Remote repository to add"))
      )
    }
  }
  object Ping: ArgParser1<Int>(
    argInt("count c", "Send count packets", "num", 1), helpArg
  ) {
    override fun toString() = toString(prog = "pint", prologue = "network test utility\n")
  }
}


internal fun <A,B,C,D> ArgParser4<A,B,C,D>.run(text: String) = run(text.splitArgv())
