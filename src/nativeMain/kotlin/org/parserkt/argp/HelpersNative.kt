package org.parserkt.argp

import kotlinx.cinterop.toKString
import platform.posix.*

object NativeEnv: Env {
  override fun getEnv(name: String): String? = getenv(name)?.toKString()
  override fun promptForLine(prompt: String): String? {
    if (isatty(FD_STDIN) != 0) { print(prompt) }
    return readLine()
  }
  override fun write(text: String) { print(text) }
  override fun writeErr(text: String) { fprintf(stderr, text) ; fflush(stderr) }
  override val lineSeparator: String get() = if (isOsWindows) "\r\n" else "\n"
}

actual fun getSysEnv(): Env = NativeEnv
actual val isOsWindows: Boolean = (Platform.osFamily == OsFamily.WINDOWS)

actual fun mpReadArgInt(text: String, radix: Int) = text.toIntOrNull(radix) ?: throw NumberFormatException("For input string: \"$text\"")
