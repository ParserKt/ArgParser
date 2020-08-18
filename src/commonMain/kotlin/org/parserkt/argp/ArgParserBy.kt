package org.parserkt.argp

import kotlin.reflect.KProperty

/** [ArgParser4] class constructed with val by delegate support, using [flag]/[plus]/[item] ordered call side-effects(not direct list) */
class ArgParserBy(private val prog: String) {
  private val flags: MutableList<Arg<*>> = mutableListOf()
  private val params: MutableList<Arg<*>> = mutableListOf() //v inner classes composed with by delegates
  private val items: MutableList<Arg<*>> = mutableListOf()
  private var autoSplit: List<String> = emptyList()
  private var itemMode: PositionalMode = PositionalMode.MustAfter

  private val reversed by lazy { items.firstOrNull()?.name == SPREAD } //< item [...] a b reversed
  private fun mayReversed(ap: DynArgParserUnit) = if (reversed) ap.reversed() else ap
  private val ap by lazy { flags.add(helpArg)
    DynArgParserUnit(noArg, noArg, noArg, noArg, *flags.toTypedArray(),
      itemArgs=items.toList(), moreArgs=this.params, autoSplit=this.autoSplit, itemMode=this.itemMode).let(::mayReversed) }
  private lateinit var res: DynParseResult //^ created later, flags/moreArgs is not decidable in <init>

  /** Exposed val-by delegate classes container */
  class By(private val self: ArgParserBy) { // symbol [M] means this "myself", [KP] reflect is never used
    private inline val res get() = self.res
    inner class Flag(p: Arg<*>) { init { self.flags.add(p) }
      private val flag = p.secondName
      operator fun getValue(thisRef: Any?, property: KProperty<*>): Boolean = flag in res.flags
      operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) { res.flags = if (value) res.flags+flag else res.flags.replace(flag, "") }
    }
    abstract inner class ParsedArg<T, R>(private val p: Arg<T>, private val isItem: Boolean = false) {
      init { (if (isItem) self.items else self.params).add(p) }
      protected var sto get() = res.named!!.map.getValue(p.firstName)
        set(v) { res.named!!.getMutable()!![p.firstName] = v }
      private fun remove() = (if (isItem) self.items else self.params).remove(p)
      fun <R> wrap(transform: (Arg<T>) -> R): R { remove() ; return transform(p) }

      operator fun getValue(thisRef: Any?, property: KProperty<*>): R = value
      operator fun setValue(thisRef: Any?, property: KProperty<*>, value: R) { this.value = value }
      abstract var value: R
    }
    open inner class SingleArg<T>(p: Arg<T>, isItem: Boolean): ParsedArg<T, T>(p, isItem) {
      override var value: T
        get() = @Suppress("unchecked_cast") (sto.get() as T)
        set(v) { sto = OneOrMore<Any>(v) }
    }
    inner class RepeatArg<T>(p: Arg<T>, isItem: Boolean): ParsedArg<T, List<T>>(p, isItem) {
      override var value: List<T>
        get() = @Suppress("unchecked_cast") (sto.toList() as List<T>)
        set(v) { sto = OneOrMore.listAny(v) }
    }

    inner class Param<T>(p: Arg<T>): SingleArg<T>(p, isItem = false) {
      init { require(!p.repeatable) {"use (ap+p).multiply() instead"} }
      fun <R> multiply(transform: (OneOrMore<T>) -> R): ArgConvert<T, R> = wrap { ArgConvert(it.toRepeatable(), transform) }
      fun multiply(): RepeatArg<T> = wrap { RepeatArg(it.toRepeatable(), isItem = false) } //<^ convert ops
      private fun <T> Arg<T>.toRepeatable() = Arg(name, help, param, null, true, convert)
    }
    inner class ArgConvert<T, R>(p: Arg<T>, private val transform: (OneOrMore<T>) -> R): ParsedArg<T, R>(p) {
      override var value: R
        get() = @Suppress("unchecked_cast") (sto as OneOrMore<T>).let(transform)
        set(_) {}
    }
  }

  fun flag(name: String, help: String, flag: String? = null) = By(this).Flag(if (flag != null) arg(name, help) { flag } else arg(name, help))
  operator fun <T> plus(p: Arg<T>) = By(this).Param(p)
  fun <T> item(p: Arg<T>) = By(this).SingleArg(p.addNopConvert(), true)
  fun <T> items(p: Arg<T>) = By(this).RepeatArg(p.addNopConvert(), true)
  fun autoSplit(rules: String = "") { autoSplit = rules.split() }
  fun itemMode(mode: PositionalMode) { itemMode = mode }

  /** Parse [args], results [ParseResult.items], only then by-delegates becomes usable. [SwitchParser.ParseError] may thrown. */
  fun run(args: ArgArray): List<String> {
    res =  ap.run(if (reversed) ap.reversedArgArray(args) else args) //v reverse result items?
    if (reversed) { res.items.reverse() ; res.named?.getMutable()?.mapKey(SPREAD) { OneOrMore.list(it.reversed()) } }
    return res.items
  }
  /** Transform last [run] result back into args. Change to use var-by delegates and modify their values, it's convenient for calling CLI programs */
  fun backRun(): ArgArray = @Suppress("unchecked_cast") ap.backRun(res as ParseResult<Unit,Unit,Unit,Unit>)
  fun asParser(): DynArgParserUnit = ap
  /** Adds sub-command [p] named [prog] with [help], see [ArgParser4.addSub] */
  fun addSub(help: String, p: ArgParserBy) { asParser().addSub(p.prog, help, p.asParser()) }
  override fun toString() = asParser().toString(prog = this.prog)
}
