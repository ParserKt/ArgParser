package org.parserkt.argp

typealias ArgArray = Array<out String>
typealias Convert<R> = ((String) -> R)?

/** Platform-dependent environment polyfill for getenv/prompt/writeErr support */
interface Env {
  fun getEnv(name: String): String?
  fun promptForLine(prompt: String): String?
  fun write(text: String)
  fun writeErr(text: String)
  val lineSeparator: String
  companion object Constants {
    val sys: Env = getSysEnv()
  }
}
expect fun getSysEnv(): Env
expect val isOsWindows: Boolean

internal const val FD_STDIN = 0

/** Defines an argument, empty [param] is a shorthand to [Arg.firstName], empty [help] will not show in [ArgParser4.toString] */
inline fun <reified R> arg( //v of generic R, string and int (default provided)
  name: String, help: String, param: String? = null,
  default_value: R? = null, repeatable: Boolean = false, noinline convert: Convert<R>): Arg<R>
  = Arg(name, help, if (param == "") name.split(' ').first() else param, default_value, repeatable, convert)

fun arg(name: String, help: String, param: String? = null,
        default_value: String? = null, repeatable: Boolean = false, convert: Convert<String> = null)
  = arg<String>(name, help, param, default_value, repeatable, convert)
fun argInt(name: String, help: String, param: String? = "n",
           default_value: Int? = null, repeatable: Boolean = false, radix: Int = 10): Arg<Int>
  = arg(name, help, param, default_value, repeatable) { mpReadArgInt(it, radix) }

expect fun mpReadArgInt(text: String, radix: Int): Int

/** Mapping string param to [R], appends ..." in map.keys" to help */
fun <R> Arg<String>.options(default_value: R?, map: Map<String, R>): Arg<R> //v of Enum, (str to R), (str checked)
  = Arg(name, "$help in ${map.entries.joinToString(", ") {it.key}}", param, default_value, repeatable, map::getValue)

inline fun <reified R: Enum<R>> Arg<String>.options(default_value: R?): Arg<R> = options(default_value, enumValues<R>().associateBy { it.name.toLowerCase() })
fun <R> Arg<String>.options(default_value: R?, vararg pairs: Pair<String, R>): Arg<R> = options(default_value, mapOf(*pairs))
fun Arg<String>.checkOptions(vararg strings: String) = options(defaultValue, *strings.map { it to it }.toTypedArray())

/** Use like `arg(..., convert = multiParam {})` to add multi-param converter */
fun <R> multiParam(convert: (List<String>) -> R): Convert<R> = { it.split('\u001F').let(convert) }
/** Use like `ArgParser(flags = *defineFlags("apple" to "a", "banana" to "b"))` to add long/short flags in a quick way */
fun defineFlags(vararg pairs: Pair<String, String>): Array<Arg<*>>
  = pairs.asIterable().map { arg("${it.first} ${it.second}", it.first.split("-").joinToString(" ")) }.toTypedArray()

/** Union type for [E] and its [List], type consistence ([value] = null)? should be cared manually. */
class OneOrMore<E>(var value: E? = null): Iterable<E> {
  val list: MutableList<E> by lazy(::mutableListOf)
  val size get() = if (value != null) 1 else list.size
  fun add(item: E) { list.add(item) }
  fun get() = value ?: error("use list[0]")
  operator fun get(index: Int) = if (value != null) error("not list") else list[index]
  override fun iterator(): Iterator<E> = if (value != null) listOf(value!!).iterator() else list.iterator()
  override fun toString() = "${value ?: list}"
  companion object {
    fun <T> list(xz: Iterable<T>) = OneOrMore<T>().apply { for (x in xz) add(x) }
    fun listAny(xz: Iterable<*>) = OneOrMore<Any>().apply { for (x in xz) add(x as Any) }
  }
}

/** Dynamic typed map for [OneOrMore], use its [getAs]/[getAsList] to check&access key. */
class NamedMap(val map: Map<String, OneOrMore<Any>>) {
  inline fun <reified R> getAs(key: String) = map[key]?.get() as R
  inline fun <reified R> getAsList(key: String) = @Suppress("unchecked_cast") (map[key]?.list as List<R>)
  operator fun get(key: String) = getAs<String>(key)
}

/** Makes text case an option： being all-upper/lower cased or capitalized */
enum class TextCaps {
  None, AllUpper, AllLower, Capitalized;
  operator fun invoke(text: String) = when (this) {
    None -> text
    AllUpper -> text.toUpperCase()
    AllLower -> text.toLowerCase()
    Capitalized -> text.capitalize()
  }
  companion object {
    val nonePair = None to None
  }
}

internal fun <T> Iterable<T>.associateByAll(key_selector: (T) -> Iterable<String>): Map<String, T> {
  val map: MutableMap<String, T> = mutableMapOf()
  for (item in this) for (k in key_selector(item)) map[k] = item
  return map
}
inline fun <K, V> MutableMap<K, V>.mapKey(key: K, transform: (V) -> V) {
  this[key]?.let(transform)?.let { this[key] = it }
}
internal fun String.split() = split(' ')
fun Char.repeats(n: Int): String {
  val sb = StringBuilder()
  for (_t in 1..n) sb.append(this)
  return sb.toString()
}

/** Space+Tab+CRLF */val whiteSpaces = charArrayOf(' ', '\t', '\r', '\n')
/** Trim, and split using [whiteSpaces] */
fun String.splitArgv() = trim(*whiteSpaces).split(*whiteSpaces).toTypedArray()
fun String.takeUnlessEmpty() = takeUnless { it.isEmpty() }
fun String.takeNotEmptyOr(value: String) = takeUnlessEmpty() ?: value
fun String.showIf(p: Boolean) = if (p) this else ""
fun String?.showIfPresent(transform: (String) -> String = {it}) = this?.let(transform) ?: ""

/** Append all elements to [sb] using [separator], and when [line_limit] reaches append a [line_separator]. [sb]'s length can be mutated calling [transform]. */
inline fun <T> Iterable<T>.joinToBreakLines(sb: StringBuilder, separator: String, line_limit: Int, line_separator: String, crossinline transform: (T) -> CharSequence): StringBuilder {
  var lineSize = 0
  var lastLength = sb.length
  return joinTo(sb, separator) {
    val line = transform(it).also { line -> lineSize += line.length + (sb.length - lastLength) ; lastLength = sb.length  }
    if (lineSize < line_limit) line
    else (line_separator + line).also { lineSize = 0 }
  }.let { if (lineSize == 0 && it.isNotEmpty()) StringBuilder(it.deletedLast(line_separator.length)) else it }
}
fun StringBuilder.deletedLast(n: Int): CharSequence = subSequence(0, length - n)
//^ the old Java-only one is = remove(length - (n-1), length) ; setLength in Kotlin requires experimental, so temp. change to this
