package com.squareup.invert.internal

import java.io.File

/**
 * Data bundle that includes standard output, error output, and exit code of a process.
 */
data class TerminalExecRunnerResult(
  val stdOut: String,
  val stdErr: String,
  val exitCode: Int
)

/**
 * Runs a terminal command that returns [TerminalExecRunnerResult] when the process finishes.
 *
 * @param [command] shell command split into a list that includes the program and it's arguments.
 */
fun exec(command: List<String>, cwd: File? = null): TerminalExecRunnerResult {
  val process = ProcessBuilder(command)
    .apply {
      if (cwd != null) {
        directory(cwd)
      }
    }
    .start()

  val stdout = process
    .inputStream
    .bufferedReader()
    .use { it.readText() }
    .trim()

  val stderr = process
    .errorStream
    .bufferedReader()
    .use { it.readText() }
    .trim()

  val exitCode = process.waitFor()

  return TerminalExecRunnerResult(stdout, stderr, exitCode)
}

/**
 * Runs a terminal command that returns [TerminalExecRunnerResult] when the process finishes.
 *
 * Note: This method should only be used for simple commands, as the command is manually split by
 * a space character. This becomes a problem for commands with more complex arguments that include
 * spaces. We don't split spaces inside of double quotes, but more advanced scenarios might fail, so
 * use at your own risk.
 *
 * @param [command] shell command as you would enter it in the terminal.
 */
fun exec(command: String, cwd: File? = null): TerminalExecRunnerResult {
  return exec(
    // https://stackoverflow.com/a/51356605
    // This regex splits strings only outside of double quotes.
    command = command.split(" (?=(?:[^\"]*\"[^\"]*\")*[^\"]*\$)".toRegex())
      .map {
        // Strip surrounding double quotes.
        it.trim('"')
      },
    cwd = cwd,
  )
}
