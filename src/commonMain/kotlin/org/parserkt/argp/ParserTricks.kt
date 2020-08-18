package org.parserkt.argp

import org.parserkt.argp.Env.Constants.sys

private class HelpSubCommand(private val parent: DynArgParser, private val prog: String): DynArgParserUnit(noArg, noArg, noArg, noArg,
  itemArgs = listOf(arg(SPREAD, ""))) {
  override fun preCheckResult(result: ParseResult<Unit, Unit, Unit, Unit>) {
    result.flags += "h" //< make parent-parser return early
    if (result.items.isEmpty()) { println(parent.toString()) ; return }
    val helpContainer = parent.getSub(result.items.run { subList(0, size-1) })
    val name = result.items.last()
    //^ subcmd parent/name got
    val subcmd = try { helpContainer.getSub(listOf(name))
    } catch (_: NoSuchElementException) { println("unknown sub-command $name in ${helpContainer.toString(head="cmd: ")}") ; SwitchParser.stop() }
    val help = helpContainer.getSubHelp(name)
    println("$prog${result.items.joinToString(" ")}: $help")
    println(subcmd.toString()) //^[val#1] try get&print or err&stop
  }

  // returns empty.
  override fun toString(caps: Pair<TextCaps, TextCaps>, row_max: Int, head: String, prog: String, prologue: String, epilogue: String, indent: String, space: String, colon: String, comma: String, newline: String, groups: Map<String, String>?, transform_summary: ((String) -> String)?, recursion: Int): String = ""
}

fun DynArgParser.addHelpSubCommand(prog: String = "") {
  addSub("help", "show help for a sub-command", HelpSubCommand(this, prog))
}

/** Try loads env with key `SOME_NAME` from [Env.sys] first. see [getEnv] */
fun <T> Arg<T>.env(): Arg<T> = wrapHelpAndDefault { "$help, env ${firstName.envKey}" to (sys.getEnv(firstName)?.let { convert?.invoke(it) } ?: defaultValue) }
fun Arg<String>.getEnv() = wrapHelpAndDefault { "$help, env ${firstName.envKey}" to (sys.getEnv(firstName) ?: defaultValue) }
private val String.envKey get() = replace('-', '_').toUpperCase()

fun <T> Arg<T>.wrapHelpAndDefault(op: Arg<T>.() -> Pair<String, T?>) = op(this).run { Arg(name, first, param, second, repeatable, convert) }
fun <T, R> Arg<T>.wrapConvertAndDefault(default_value: R?, convert: Convert<R>): Arg<R> = Arg(name, help, param, default_value, repeatable, convert)
/** Makes item arg result writes to [ParseResult.named], otherwise [Arg.convert] must not be null */
fun <T> Arg<T>.addNopConvert() = wrapConvertAndDefault(defaultValue) { convert?.invoke(it) ?: @Suppress("unchecked_cast") (it as T) }

/** Support reversed-order item destruct `a b c [...]` to `[...] c b a` */
fun <A,B,C,D> ArgParser4<A,B,C,D>.reversed() = run {
  ArgParser4(p1, p2, p3, p4, flags=*flags, itemArgs=itemArgs.reversed(), moreArgs=moreArgs, autoSplit=autoSplit, itemMode=itemMode.reversed())
}
fun PositionalMode.reversed() = when (this) {
  PositionalMode.MustBefore -> PositionalMode.MustAfter
  PositionalMode.MustAfter -> PositionalMode.MustBefore
  else -> this
}
fun DynArgParser.reversedArgArray(args: ArgArray) = run {
  val ap = ArgParser4(p1, p2, p3, p4, flags=*flags, itemArgs=listOf(arg(SPREAD, "items")), moreArgs=moreArgs, autoSplit=autoSplit, itemMode=itemMode)
  return@run ap.run(args).let { res -> ap.backRun(res) { it.reversed() } }
}
