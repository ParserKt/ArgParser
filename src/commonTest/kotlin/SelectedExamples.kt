import org.parserkt.argp.*

object ReadMap: ArgParser3<String, Pair<String, String>, String>(
  arg("name", "store mapping to a name", "map_name", default_value = "anonymous") { require(it.isNotBlank()) ; it },
  arg("map m", "maps a key to its value", "k v", repeatable = true, convert = multiParam { it[0] to it[1] }),
  arg("mode", "mode of this mapping", ""/*default to it's name, "mode"*/, "none").checkOptions("none", "mutable", "linked"),
  arg("v", "print version") { println("mapper v1.0") ; SwitchParser.stop() }
)

class ReadMapUsingBy(ap: ArgParserBy) {
  val name by ap+arg("name", "...", "map_name", "anonymous") { require(it.isNotBlank()) ; it }
  val map by (ap+arg("map p", "...", "k v", convert = multiParam { it[0] to it[1] })).multiply { it.toMap() }
  val mode by ap+arg("mode", "...", "", "none").checkOptions("none", "mutable", "linked")
  val showVer by ap+arg("v", "print version") { println("v1.0") ; SwitchParser.stop() }//should not be accessed
  val isShowVer/*run later?*/ by ap.flag("v", "print version", "vh") // when 'h' in res.flags, item count checking are suppressed.
}

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

object PipOld: ArgParser1<Unit>(noArg, helpArg) {
  init { addSub("show", "...", Show) ; addHelpSubCommand("pip") }
  object Show: ArgParser1<Unit>(noArg, arg("files f", "..."))
}

object KotlincJVM: ArgParser4<List<String>, String, String, String>(
  arg<List<String>>("classpath cp", "Paths where to find user class files", "path") { it.split(':') },
  arg("d", "Destination for generated class files", "directory|jar", "."),
  arg("jvm-target", "Target version of the generated JVM bytecode (1.6 or 1.8)", "version", "1.6"),
  arg("module-name", "Name of the generated .kotlin_module file", "name"),
  arg("include-runtime i", "Include Kotlin runtime in to resulting .jar"),
  arg("java-parameters p", "Generate metadata for Java 1.8 reflection on method parameters"),
  *listOf("jdk j" to "Java runtime", "reflect r" to "kotlin-reflect.jar", "stdlib l" to "kotlin-stdlib.jar or kotlin-reflect.jar").map {
    arg("no-${it.first}", "Don't include ${it.second} into classpath")
  }.toTypedArray(),
  arg("script", "Evaluate the script file"),
  arg("Werror", "Report an error if there are any warnings"),
  arg("X", "Print a synopsis of advanced options"),
  helpArg,
  arg("nowarn", "Generate no warnings"),
  arg("verbose", "Enable verbose logging output"),
  arg("v", "Display compiler version") { println("Kotlinc Fake") ; SwitchParser.stop() },
  moreArgs = listOf(
    arg("java-home", "Path to JDK home directory to include into classpath, if differs from default JAVA_HOME", "path").getEnv(),
    arg("script-templates", "Script definition template classes", "fully qualified class name[,]", emptyList()) { it.split(',') },
    arg("api-version", "Allow to use declarations only from the specified version of bundled libraries", "version"),
    arg("kotlin-home", "Path to Kotlin compiler home directory, used for runtime libraries discovery"),
    arg("language-version", "Provide source compatibility with specified language version", "version"),
    arg("P", "Pass an option to a plugin", "plugin:<pluginId>:<optionName>=<value>", repeatable = true)
  )
)

/** A arithmetic calculator: `calc 1 2 3 -+ 10 -* 2` = 22 44 66 */
object Arithmetic: ArgParser4<Int, Int, Int, Int>(
  argInt(". add +", "Plus a number", "n"/*default in argInt*/, repeatable = true),
  argInt(". sub -", "Subtract a number", "", repeatable = true), // semantic for -- is not forcibly pre-defined!
  argInt(". mul *", "Repeat results n times", "", repeatable = true),
  argInt(". div /", "Divides results", "", repeatable = true),
  helpArg, //< here comes flags (args w/o param)
  arg("neg n", "Negate result"),
  arg("--", "stop handling prefixes") { throw SwitchParser.PrefixesStop }, // now "---" means "stop of options(prefixed args)"
  itemArgs = listOf(argInt("...", "operand numbers")),
  moreArgs = listOf(arg("add-postfix ap", "add postfix for result", param = "prefix", default_value = ""))
) {
  override fun checkPrefixForName(name: String, prefix: String) {
    super.checkPrefixForName(name, prefix)
    if (name == ".") throw SwitchParser.ParseError("that's a placeholder, not arg")
  }
}
/*discontinued -- add opId for operands is too complex
class ArithmeticTest: BaseArgParserTest<Int,Int,Int,Int>(Arithmetic) {
  fun doArithmetic(result: ParseResult<Int, Int, Int, Int>): String {
    result.named!!.getAsList<Int>("...").forEach {
      var accumulator = it
      result.named.getAsList<Int>()
    }
  }
}*/
