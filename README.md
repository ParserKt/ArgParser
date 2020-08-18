# ArgParser

<h1 align="center">
  <img alt="banner anim" src="https://parserkt.github.io/resources/ArgParser_anim.gif" />
</h1>

ArgParser is a simplified argument parser / multiplatform CLI for Kotlin.

With only about 500 LoC implementation, ArgParser has:

- Parsing for params, flags, items(positional args) and [subcommands](src/commonMain/kotlin/org/parserkt/argp/ParserTricks.kt)
- Type safe and convince data model (`Tuple4`/`OneOrMore` and `NamedMap`, you can ignore them and just use `ArgParserBy`)
- No pre-defined command syntax restriction, only prefixed(param, flag) and unprefixed(item) args are distinguished
- No self-made concept, let users define `-v`, `-h` (`ParseStop`) and `--` (`PrefixesStop`) behavior themselves 

```kotlin
object ReadMap: ArgParser3<String, Pair<String, String>, String>(
  arg("name", "store mapping to a name", param = "map_name", default_value = "anonymous") { require(it.isNotBlank()) ; it },
  arg("map m", "maps a key to its value", "k v", repeatable = true, convert = multiParam { it[0] to it[1] }),
  arg("mode", "mode of this mapping", ""/*default to it's name, "mode"*/, "none").checkOptions("none", "mutable", "linked"),
  arg("v", "print version") { println("mapper v1.0") ; throw SwitchParser.ParseStop }
)
```

And the one using `ArgParserBy` wrapper:

```kotlin
class ReadMapUsingBy(ap: ArgParserBy) {
  val name by ap+arg("name", "...", "map_name", "anonymous") { require(it.isNotBlank()) ; it }
  val map by (ap+arg("map m", "...", "k v", convert = multiParam { it[0] to it[1] })).multiply { it.toMap() }
  val mode by ap+arg("mode", "...", "", "none").checkOptions("none", "mutable", "linked")
  val showVer by ap+arg("v", "print version") { println("v1.0") ; SwitchParser.stop() }//should not be accessed
  val isShowVer/*run later?*/ by ap.flag("v", "print version", "vh") // when 'h' in res.flags, item count/ordering checking are suppressed.
}
```

Subcommands:

```kotlin
class Pip(ap: ArgParserBy) {
  val show = ArgParserBy("show").apply {
    flag("files f", "Show the full list of installed files for each package.")
    // Use general super class to add general(non subcommand specific) options.
  }
  val install = ArgParserBy("install").apply {
    this+arg("index-url i", "Base URL of Python Package Index", "url")
    (this+arg("requirement r", "Install from the given requirements file.", "file")).multiply()
    item(arg("target", "target specifier, maybe requirements file / VCS / local / PyPI", "archive url/path"))
  }
  init {
    ap+helpArg ; ap.asParser().addHelpSubCommand("pip")
    ap.flag("verbose v", "Give more output. Option is additive, and can be used up to 3 times.") // then check "vvv" in flags, or count { it == "v" }.
    ap.addSub("Show information about one or more installed packages.", show)
    ap.addSub("Install packages from PyPI/VCS/Local or remote path&archives", install)
    ap.addSub("Compute hashes of package archives.", ArgParserBy("hash").apply { items(arg("...", "")) })
  }
}
```

And the one using the old way:

```kotlin
object PipOld: ArgParser1<Unit>(noArg, helpArg) {
  init { addSub("show", "...", Show) ; addHelpSubCommand("pip") }
  object Show: ArgParser1<Unit>(noArg, arg("files f", "..."))
}
```

## Features 🎁

+ Supports named args `--a=b`(using `autoSplit=listOf("a=")`) with configurable prefix list
+ Supports variable number of items and named destruct (both `[src...] [dst]` and `[dst] [src...]` are supported)
+ Extension funs for idiom: `checkOptions(*strings)`, `options("a" to 1)`, `options(SomeEnum)`, `getEnv()`, `multiParam {list->}`, `defineFlags("enable-oo" to "O")`
+ Supports __repeatable args, multiply params, each can with a converter__
+ __Sub-commands and command alias__ (like `pip install`, `git commit -m` -> `git cm`)
+ Extensible using `override`, features like prompt when missing, mutually exclusive args, sequential prefixes(`ffmpeg`), @-file expansion, did-you-mean suggestion can be implmented easily
+ Generates well formatted line-wrapped help message (can be grouped), only with Kotlin's named arguments in `toString`(opened for override)
+ __Supports `backRun` unparsing__, so it's also fascinating to build a CLI program binding with this lib
+ Supports 3 different programming paradigm: procedural `SwitchParser`, OOP&FP `ArgParser`, reflect `ArgParserBy`
+ KISS(keep it simple, stupid) principle, modular programming, "Simplicity is the ultimate sophistication".
+ Readable code with reading order guide, <^v>comments and a little line breaks ;)
+ TBD: add shell completion gen(not compile time, req. Arg objects) in a separate module

## Not Features 😟

+ Doesn't provide basic APIs for building CLIs on Kotlin/Multiplatform
+ Use __*too much puzzling configuration objects*__ that hiding the essence from users (e.g. "token transformer" instead of Kotlin's native approach `ap.run(map {}.toTypedArray())`)
+ Define __*too complicated data/mental model*__ for command line args
+ Unchangable&invisible __*pre-defined syntax for args*__ (e.g. GNU/POSIX)
+ Boring API that have __*too much coding conventions(e.g. named parameter)*__ or boilerplate codes
+ Doesn't support argument types other than `String`, or expose unchecked types
+ Treating flags as "boolean params", instead of storing them together
+ Made it too complicated to set help-printing format
+ Call `exitProcess` instead of throwing `ParseError` to let users influence error behavior(e.g. to print usage)
+ Unnecessary reflection / lazy calculation(e.g. runParse)
+ Unnecessary design patterns(e.g. validator)/ Exception type definitions
+ Too long implementation code that scares newcomers from reading :fearful:

## FAQ

- _Why not add shorthands for types like `Double`, `kotlin.Pair` and `Triple`?_

I think using a general `convert` parameter for `arg` is sufficient, and those "special case" ain't special enough.

(In fact I checked `--help` option in ruby, python, lua, rustc, php, squeak, java, javac, kotlinc; no one have multi-param or float num options... even gcc, clang doesn't have much)

So what's the reason for adding them in `org.parserkt.argp`, instead of letting users to define them explicitly in their projects...

- _Why not enforce GNU(`-o`,`--name`,`--name=val`)/POSIX(`--`) syntax for cmdline_

This repository is licensed under MIT, neither GNU nor UNIX :)

...and it's boring to separate "long option" and "short option"(they are both "prefixed" option), if you wants to build a [Minimist Parser](https://github.com/substack/minimist)
that must distinguish between them, override `checkPrefixForName` and save `var currentPrefix`
 (a partially implemented one can be found [here](src/commonTest/kotlin/TheArgParserVersions.kt#L64)).

- _The animated banner is low-quality/have slow frame rate_

This one is the best I've got, it tooks me at least 5-hrs making that...

- _How's the performance of this lib comparing to others?_

I don't care, but `SwitchParser` will only traverse `args` once every time you `run`,
 and declarative code is _always no faster than_ imperative one (a lot of `Map<String,Arg<*>>` is allocated in `ArgParser4`)

- _`ArgParserBy` for unusual/subcommand arg definitions requires dirty code_

I'v tried my best... forgive me

- _Your English grammar is poor_

...Sorry for that, and please open an issue if you found my grammar mistakes.

## Extension APIs

[ArgParserHandlers](src/commonMain/kotlin/org/parserkt/argp/ArgParser.kt#L46)

```kotlin
//package org.parserkt.argp

const val SPREAD = "..." //< spread itemArg name

val noArg: Arg<Unit> = arg<Unit>("\u0000\u0000", "") { error("noArg parsed") }
val helpArg = arg("h help", "print this help") { SwitchParser.stop() }

abstract class ArgParserHandlers {
  protected open fun printHelp() = println(toString())
  protected open fun prefixMessageCaps() = TextCaps.nonePair
  protected open fun rescuePrefix(name: String): Arg<*> { throw SwitchParser.ParseError("$name unknown") }
  protected open fun rescueMissing(p: Arg<*>): Any? = null
  protected open fun checkAutoSplitForName(name: String, param: String) {}
  protected open fun checkPrefixForName(name: String, prefix: String) {
    if (prefix == "--" && name.length == 1) throw SwitchParser.ParseError("single-char shorthand should like: -$name")
  }
  /** Un-parse method used in ArgParser4.backRun */
  protected open fun showParam(param: Any): String? = param?.toString() //<^ real implementation omitted
}
```

- rewrite `prefixMessageCaps` if you want to change letter case in message prefix / body
- rewrite `rescuePrefix` (yield an `Arg<*>` with param/defaultValue) if you want to support dynamic `-k=v` or `-n5`
- rewrite `rescueMissing` if you want to prompt for arg value when missing

## Source Reading

Ordering & focal point:

- [SwitchParser](src/commonMain/kotlin/org/parserkt/argp/SwitchParser.kt) a basic `Array<String>` iterator state machine with `onItem` and `onPrefix` and pretty error messages. __focus__: `run`, `arg`, `ParseError`/`ParseLoc`, `Companion.extractArg`
- [ArgParser](src/commonMain/kotlin/org/parserkt/argp/ArgParser.kt) `ArgParser4`~`ArgParsrer1`(subclass) functional wrapper to `SwitchParser` using `Map`s to make DSL-style available. 
    __focus__: `ParseResult` (tup,named,flags,items), `ArgParser4.<init>`(checks), `commandAliases`,
    `dynamicNameMap`/`flagMap`(non-typed "prefix name" map), `inner class Driver: SwitchParser`, `argToOMs`+`nameMap`,
    `getOrPutOM`/`addResult`(dynamic storage), `itemArgZ`(an iter); `onItem`, `onPrefix`, `read`, `dynamicInterpret`; `onArg` (alias&subcmd), `run`, `assignDefaultOrFail`; and `backRun`/`toString`/`ArgParserAsSubDelegate`  
- [Helpers](src/commonMain/kotlin/org/parserkt/argp/Helpers.kt) Utils & multiplatform CLI and extension for `Arg<String>`/`Arg<T>` building. __focus__: `Env` a multiplatform(MP) CLI API set(envvar/prompt/write), `arg`/`argInt`, (JVM)`argFile`, 
    `OneOrMore`+`NamedMap`, `TextCaps`, `associateByAll`(used in arg alias `Arg.names`), `joinToBreakLines` (format --help summary line)
- [ArgParserBy](src/commonMain/kotlin/org/parserkt/argp/ArgParserBy.kt) By delegates and `backRun`/`addSub` wrapper to dynamic typed `ArgParser`. __focus__: `reversed` (for `[...] a b` desctruct), `By`(used as namespace) `.res`, 
    `By.Flag`/`By.ParsedArg<T,R>`(`.wrap`), `By.Param.multiply`, `ArgParser4.reversed`/`Arg<Str>.addNopConvert`(in ParserTricks) 
- [ParserTricks](src/commonMain/kotlin/org/parserkt/argp/ParserTricks.kt) Tricks using this framework (contributions are welcome). __focus__: `addHelpSubcommand`, `Arg<T>.env`+`Arg<Str>.getEnv` (!overload for JVM), `Arg<T>.wrapXXX`

## Thanks

Thanks to the creators of Python's [argparse](https://docs.python.org/3/library/argparse.html) module, which provided the initial inspiration for this library.

Thanks also to the language design team behind Kotlin (not [kotlinx-cli](https://github.com/Kotlin/kotlinx-cli), which disappointed me!!).

Thanks to [xenomachina's argparser](https://github.com/xenomachina/kotlin-argparser), where I copied this section from :P

And to [AJ Alt's CliKT](https://github.com/ajalt/clikt), which inspired me to make the banner animation
