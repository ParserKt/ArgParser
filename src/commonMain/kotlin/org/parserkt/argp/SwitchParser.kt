package org.parserkt.argp

/**
 * Simple argument parser driver, use [arg] for arguments when [onPrefix], use [extractArg] for args like `-Ts`/`-T s`;
 *  throw [ParseStop]/[PrefixesStop] when stop is requested. [prefixes] __should be__ sorted descending by [String.length]
 */
abstract class SwitchParser<R>(protected val args: ArgArray, private val prefixes: Iterable<String> = listOf("--", "-")): Iterator<String> {
  constructor(args: ArgArray, vararg prefixes: String): this(args, prefixes.sortedByDescending { it.length })
  protected var pos = 0 ; private set
  override fun hasNext() = pos < args.size //<v expose var [pos] from Iterator<T>, add description [currentArg]
  override fun next(): String = args[pos++]
  protected var currentArg = "?"

  protected abstract fun onPrefix(name: String)
  protected abstract fun onItem(text: String) //< on prefix/item, and mutable result.
  protected abstract val res: R

  protected fun <R> arg(name: String, convert: (String) -> R): R
    = if (!hasNext()) throw ParseError(prefixMessageCaps().first("expecting $name for $currentArg"))
      else next().also { currentArg += "'s $name" }.let(convert)
  protected fun arg(name: String) = arg(name) { it }

  /** Called before [onPrefix]. [ParseError] thrown can be caught in [run]. */
  protected open fun checkPrefixForName(name: String, prefix: String) {} //<v custom prefix check & message capitalize
  protected open fun prefixMessageCaps(): Pair<TextCaps, TextCaps> = TextCaps.nonePair
  data class ParseLoc(val pos: Int, val argCount: Int, val currentArg: String) {
    fun chain(other: ParseLoc) = ParseLoc(pos+other.pos, argCount+other.argCount, currentArg)
    override fun toString() = "(#$pos, arg $argCount $currentArg)"
  }
  class ParseError(message: String, cause: Throwable? = null, val loc: ParseLoc? = null): Error(message, cause) {
    override val message: String? get() = super.message?.plus(loc?.toString().showIfPresent {" $it"})
  }
  object ParseStop: Exception() //<v control exceptions
  object PrefixesStop: Exception()

  protected open fun onArg(text: String) {
    prefixes.firstOrNull { text.startsWith(it) }?.let {
      val name = text.substring(it.length)
      checkPrefixForName(name, it)
      onPrefix(name) /*or*/} ?: onItem(text)
  }
  open fun run(): R {
    val (capPre, capMsg) = prefixMessageCaps()
    val kwAt = capPre("at") ; val kwIn = capPre("in")
    fun msgFmt(ex: Throwable) = capMsg(": ${ex.message}") //< main counted read&decide loop
    var argCount = 1 ; var prefixesStop = false
    fun parseLoc() = ParseLoc(pos, argCount, "$kwAt $currentArg")
    fun rootLoc() = parseLoc().copy(currentArg = "$kwIn $currentArg")
    while (hasNext()) try {
      val text = next().also { currentArg = it }
      if (prefixesStop) onItem(text) else onArg(text)
      argCount++
    } catch (_: ParseStop) { break }
      catch (_: PrefixesStop) { prefixesStop = true }
      catch (e: IllegalArgumentException) { throw if (e.cause != null/*no append*/) e else IllegalArgumentException(capPre("bad argument $argCount, $currentArg")+msgFmt(e), e) }
      catch (e: ParseError) { throw ParseError(e.message!!.let { if (e.loc == null) capMsg(it) else it }, e.cause, e.loc?.let(parseLoc()::chain) ?: rootLoc()) }
      catch (e: Throwable) { throw ParseError(capPre("parse fail")+msgFmt(e), e, rootLoc()) }
    return res
  }
  companion object {
    fun extractArg(prefix_name: String, text: String): String?
      = if (text.startsWith(prefix_name)) text.substring(prefix_name.length) else null
    fun stop(): Nothing = throw ParseStop
  }
}
