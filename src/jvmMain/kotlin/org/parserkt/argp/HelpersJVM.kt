package org.parserkt.argp

import java.io.File
import java.io.IOException

val noFile = File("")
/** Argument ensures a file, value [noFile] is provided by default. [flags]: mode "rw"/"d" (dir), create "+" */
fun argFile(name: String, help: String, param: String? = "path", default_value: File? = noFile, repeatable: Boolean = false, flags: String = "r")
  = arg(name, help, param, default_value, repeatable) {
  val file = File(it)
  fun require(mode: String, predicate: (File) -> Boolean) = require(predicate(file)) {"file \"$file\" cannot be opened $mode"}
  fun requireDir(p: Boolean) = require(p) {"dir $file create fail"}
  val isDir = ('d' in flags)
  if ('+' in flags && !file.exists()) {
    if (isDir) { requireDir(file.mkdirs()) }
    else { requireDir(File(file.parent).mkdirs()) ; file.createNewFile() }
  }
  if (isDir) require("as dir", File::isDirectory)
  else {
    if ('r' in flags) require("read", File::canRead)
    if ('w' in flags) require("write", File::canWrite)
  }
  file
}

object EnvJvm: Env {
  override fun getEnv(name: String): String? = System.getenv(name)
  private val reader = System.`in`.bufferedReader()
  override fun promptForLine(prompt: String): String? {
    print(prompt)
    return try { reader.readLine() } catch(_: IOException) {null}
  }
  override fun write(text: String) = print(text)
  override fun writeErr(text: String) = System.err.print(text)
  override val lineSeparator = System.lineSeparator()!!
} //^ multi-platform later!

actual fun getSysEnv(): Env = EnvJvm
actual val isOsWindows = System.getProperty("os.name").toLowerCase().contains("windows")

actual fun mpReadArgInt(text: String, radix: Int) = text.toInt(radix)
