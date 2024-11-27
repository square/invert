/**
 * Implementation from https://github.com/pgreze/kowners
 */
package com.github.pgreze.kowners

data class CodeOwnership(
    /**
     * File pattern following [gitignore format](https://git-scm.com/docs/gitignore#_pattern_format).
     */
    val pattern: Pattern,
    /**
     * Non empty list of owners following
     * [CODEOWNERS syntax](https://help.github.com/en/articles/about-code-owners#codeowners-syntax).
     *
     * Can be a @username, @org/team-name or an email address registered for a user.
     */
    val owners: List<String>
)

fun List<CharSequence>.parseCodeOwners(): List<CodeOwnership> =
    mapNotNull(CharSequence::parseCodeOwnersLine)

fun CharSequence.parseCodeOwnersLine(): CodeOwnership? {
    val line = trim { it.isWhitespace() || it.isEndOfLine() }

    if (line.isEmpty() || line.startsWith("#")) return null

    val tokens = line.tokenize().filter(String::isNotEmpty)
    require(tokens.size > 1) { "No owner in line: ${line.trim(Char::isEndOfLine)}" }

    return CodeOwnership(
        pattern = tokens.first().let(::Pattern),
        owners = tokens.subList(1, tokens.size)
    )
}

private fun CharSequence.tokenize(delimiter: String = " "): List<String> =
    split(delimiter).fold(listOf()) { acc, s ->
        if (acc.isNotEmpty() && acc.last().endsWith("\\")) {
            acc.subList(0, acc.size - 1) + (acc.last() + delimiter + s)
        } else {
            acc + s
        }
    }

private fun Char.isEndOfLine(): Boolean =
    this == '\r' || this == '\n'
