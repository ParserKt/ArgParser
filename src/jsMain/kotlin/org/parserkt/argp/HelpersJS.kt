package org.parserkt.argp

private external val process: dynamic
private external val Buffer: dynamic

class NodeEnv: Env {
  private val fs = nodeRequire("fs")
  override fun getEnv(name: String): String? = process.env[name] as? String
  override fun promptForLine(prompt: String) = buildString {
    println(prompt)
    var oneChar: String
    val buf = Buffer.alloc(1)
    do {
      fs.readSync(fd = FD_STDIN, bufer = buf, offset = 0, len = 1, position = null)
      oneChar = buf.toString()
      append(oneChar)
    } while (oneChar != "\n")
  }

  override fun write(text: String) { process.stdout.write(text) }
  override fun writeErr(text: String) { process.stderr.write(text) }

  override val lineSeparator: String get() = if (isOsWindows) "\r\n" else "\n"
}

class BrowserEnv: Env {
  override fun getEnv(name: String): String? = null
  override fun promptForLine(prompt: String): String? = null
  override fun write(text: String) { println(text) }
  override fun writeErr(text: String) { write(text) }
  override val lineSeparator: String get() = "\n"
}

actual fun getSysEnv(): Env = try { NodeEnv() } catch (_: Exception) { BrowserEnv() }
actual val isOsWindows = (process.platform == "win32")

private val isNode = js("Object.prototype.toString.call(typeof process !== 'undefined' ? process : 0) === '[object process]'") as Boolean
/** Code snippet from Clikt, see [this](https://github.com/ajalt/clikt/blob/3f0e46d70251803edf1bcc03efa16b3af6383fe5/clikt/src/jsMain/kotlin/com/github/ajalt/clikt/mpp/JsCompat.kt#L10) */
internal fun nodeRequire(mod: String): dynamic {
  require(isNode) {"Not NodeJS env"}
  val imported = try {
    js("module['' + 'require']")(mod)
  } catch (e: dynamic) {
    throw IllegalArgumentException("Module not available: $mod", e as? Throwable)
  }
  require(
    imported != null && js("typeof imported !== 'undefined'").unsafeCast<Boolean>()
  ) { "Module not available: $mod" }
  return imported
}

actual fun mpReadArgInt(text: String, radix: Int): Int = text.toIntOrNull(radix) ?: throw NumberFormatException("For input string: \"$text\"")
