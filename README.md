# ArgParser

<h1 align="center">
  <img alt="banner anim" src="https://parserkt.github.io/resources/ArgParser_anim.gif" />
</h1>

Simplified argument parser / multiplatform CLI for Kotlin ~500 LoC

## Features

+ KISS(keep it simple, stupid) principle, modular programming, "Simplicity is the ultimate sophistication".
+ Supports positional args(called items) and named args `--a=b`(using `autoSplit=listOf("a=")`) with configurable prefix list
+ Extension funs for idiom: `checkOptions(*strings)`, `options("a" to 1)`, `options(SomeEnum)`, `getEnv()`, `multiParam {list->}`, `defineFlags("enable-oo" to "O")`
+ Supports __repeatable args, multiply params, each can with a converter__
+ __Sub-commands__ and command alias (like `pip install`, `git commit -m` -> `git cm`)
+ Extensible using `override`, features like mutually exclusive args, @-file expansion, did-you-mean suggestion can be implmented easily
+ Generates well formatted line-wrapped help message (can be grouped), only with Kotlin's named arguments in `toString`(opened for override)
+ __Supports `backRun` unparsing__, so it's also fascinating to build a CLI program binding with this lib
+ Supports 3 different programming paradigm: procedural `SwitchParser`, OOP&FP `ArgParser`, reflect `ArgParserBy`
+ Readable code with reading order guide, <^v>comments and a little line breaks, ~400LoC ;)

## Not Features

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
