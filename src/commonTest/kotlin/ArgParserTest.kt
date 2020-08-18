import org.parserkt.argp.*
import kotlin.test.*

private var printed = ""
val luaP = ArgParser3(
  arg("l", "require library 'name' into global 'name'", "name", repeatable = true),
  arg("e", "execute string 'stat'", "stat mode", "a" to "b", convert = multiParam { it[0] to it[1].also { m -> require(m != "fail") } }),
  arg("hex", "just an option added for test", "n", "FA") { mpReadArgInt(it, 16).toString() },
  arg("i", "enter interactive mode after executing 'script'"), //^ errors should not looks like -e's stat's mode, that's error.
  arg("v", "show version information") { printed += "Lua 5.3" ; SwitchParser.stop() },
  arg("E", "ignore environment variables"),
  arg("", "stop handling options and execute stdin") { SwitchParser.stop() },
  arg("-", "stop handling options") { SwitchParser.stop() }
)

abstract class BaseArgParserTest<A,B,C,D>(val p: ArgParser4<A,B,C,D>) {
  fun assertFailMessage(expected: String, args: String) = assertEquals(expected, assertFailsWith<Throwable> { p.run(args.splitArgv()) }.message)
  fun p(args: String) = p.run(args.splitArgv())
  fun backP(args: String) = p(args).let { p.run(p.backRun(it)) }
}

class ArgParserTest: BaseArgParserTest<String, Pair<String, String>, String,Unit>(luaP) {
  @Test fun itWorks() {
    assertEquals(listOf("a", "b"), p("-l a -l b").tup.e1.toList())
    p("-e hello stmt -l a -i -E --").run {
      val (e1, e2, e3, _) = tup
      assertEquals("hello" to "stmt", e2.get())
      assertEquals("a", e1[0])
      assertEquals(listOf("a"), e1.toList())
      assertEquals("FA", e3.get())
      assertEquals("iE", flags)
    }
    p("-hex ff -v - -v").run {
      assertEquals("255", tup.e3.get())
      assertEquals("Lua 5.3", printed)
    }
    backP("-l a -e fault expr -l b -i -E -v").run {
      assertEquals("iE", flags)
      assertEquals(listOf("a", "b"), tup.e1.toList())
      assertEquals("fault" to "expr", tup.e2.get())
    }
    backP("-- a").run { assertEquals(emptyList<String>(), items) }
  }
  @Test fun itFails() {
    assertFailMessage("single-char shorthand should like: -E (#3, arg 2 in --E)", "--hex af --E")
    assertFailMessage("bad argument 1, --hex's n: For input string: \".23\"", "--hex .23")
    assertFailMessage("argument e repeated (#6, arg 2 in in -e)", "-e wtf mode -e twice x item")
    assertEquals("flag wtf w/o param should be putted in ArgParser(flags = ...)",
      assertFailsWith<IllegalStateException> { ArgParser1(arg("wtf", "e mmm", param = null)).run("".splitArgv()) }.message)
    assertFailMessage("expecting stat for -e (#1, arg 1 in -e)", "-e")
    assertFailMessage("expecting mode for -e (#4, arg 2 in -e)", "-hex 23 -e wtf")
    assertFailMessage("bad argument 1, in -e: Failed requirement.", "-e code fail")
  }
  @Test fun itFormats() {
    assertEquals("""
      Usage: {-l name} (-e stat, mode) (-hex n) [-i] [-v] [-E] [-] [--]
        -l: require library 'name' into global 'name'
        -e: execute string 'stat' (default (a, b))
        -hex: just an option added for test (default FA)
        -i: enter interactive mode after executing 'script'
        -v: show version information
        -E: ignore environment variables
        -: stop handling options and execute stdin
        --: stop handling options

    """.trimIndent(), luaP.toString())
    assertEquals("""
      用法： {-l NAME} (-e STAT, MODE) (-hex N) [-i] [-v] [-E] [-] [--]哈。
      | 参数-l呢，是Require library 'name' into global 'name'哈。
      | 参数-e呢，是Execute string 'stat' (default (a, b))哈。
      | 参数-hex呢，是Just an option added for test (default FA)哈。
      | 参数-i呢，是Enter interactive mode after executing 'script'哈。
      | 参数-v呢，是Show version information哈。
      | 参数-E呢，是Ignore environment variables哈。
      | 参数-呢，是Stop handling options and execute stdin哈。
      | 参数--呢，是Stop handling options哈。
      就是这样，喵。
    """.trimIndent(), luaP.toString(TextCaps.AllUpper to TextCaps.Capitalized, head="用法： ", epilogue="就是这样，喵。", indent="| 参数", colon="呢，是", newline="哈。\n"))
    assertEquals("""
      Usage: {-l name} (-e stat, mode) (-hex n) [-i] [-v] [-E] [-] [--]
      Options: 
          -l: require library 'name' into global 'name'
          -e: execute string 'stat' (default (a, b))
          -hex: just an option added for test (default FA)
      Flags: 
          -i: enter interactive mode after executing 'script'
          -E: ignore environment variables
      Help: 
          -v: show version information
          -: stop handling options and execute stdin
          --: stop handling options

    """.trimIndent(), luaP.toString(groups = mapOf("l e hex" to "Options", "i E" to "Flags", "*" to "Help")))
  }
  @Test fun unorderedFormats() {
    val pas = ArgParser4(arg("donkey", "donkey you rides", "name"), noArg, noArg, noArg, itemArgs = listOf(arg("papa",""), arg("mama","")))
    assertEquals("""
      Usage: [-donkey name] <papa> <mama>
        -donkey: donkey you rides
      options can be mixed with items.

    """.trimIndent(), pas.toString())
    assertEquals(listOf("A", "B"), pas.run(arrayOf("A", "-donkey", "ED2K", "B")).items)
  }
}
