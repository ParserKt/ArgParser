import org.parserkt.argp.SwitchParser
import org.parserkt.argp.mpReadArgInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SwitchParserTest {
  fun itWorks(vararg args: String) {
    val r = LuaSwitchParser(args).run()
    assertEquals("main()", r.exec)
    assertEquals(listOf("objects", "json"), r.libs)
    assertEquals("E", r.flags)
    assertEquals("my.lua", r.script)
  }
  @Test fun itWorks() = itWorks("-e", "main()", "-l", "objects", "-l", "json", "-E", "my.lua", "--", "-")
  @Test fun itWorks1() {
    assertMessageEquals("expecting lib_name for -l (#3, arg 2 in -l)", "-e", "print(1)", "-l")
    assertMessageEquals("x unknown (#1, arg 1 in -x)", "-x")
    assertMessageEquals("bad argument 1, -hex's n: For input string: \"af_0\"", "-hex", "af_0")
  }
  private fun assertMessageEquals(expected: String, vararg args: String) {
    val ex = assertFailsWith<Throwable> { LuaSwitchParser(args).run() }
    assertEquals(expected, ex.message)
  }
}

data class LuaCommand(var exec: String?, val libs: MutableList<String>, var flags: String, var script: String?, val args: MutableList<String>)

class LuaSwitchParser(args: Array<out String>): SwitchParser<LuaCommand>(args, "-") {
  override val res = LuaCommand(null, mutableListOf(), "", null, mutableListOf())
  override fun onPrefix(name: String): Unit = when (name) {
    "e" -> res.exec = arg("code")
    "l" -> res.libs.plusAssign(arg("lib_name"))
    "i", "E" -> res.flags += name
    "v", "-", "" -> {
      when (name) {
        "v" -> println("Lua 5.3.5")
        "" -> res.flags += 'x'
      }
      throw ParseStop
    }
    "hex" -> res.args.plusAssign(mpReadArgInt(arg("n"), 10).toString(16))
    else -> throw ParseError("$name unknown")
  } // require script=null?

  override fun onItem(text: String) {
    if (res.script == null) res.script = text
    else res.args.add(text)
  }
}
val Lua_HELP = """
lua [args] [[script] options]
options:
  -e stat  execute string 'stat'
  -i       enter interactive mode after executing 'script'
  -l name  require library 'name' into global 'name'
  -v       show version information
  -E       ignore environment variables
  --       stop handling options
  -        stop handling options and execute stdin
""".trimIndent()