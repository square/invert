package com.github.pgreze.kowners

// Based on a JS implementation: https://github.com/codemix/gitignore-parser/blob/master/lib/index.js
// https://github.com/JetBrains/idea-gitignore was too complex to be reused
data class Pattern(
    @Suppress("CanBeParameter")
    val pattern: String
) {
    private val regex: Regex = pattern
        .trimEnd('/') // TODO: filter file/directory
        .let {
            // Match only root folder for a path containing a / (except end of string)
            when {
                it.startsWith('/') -> "^${it.substring(1)}"
                it.contains('/') -> "^$it"
                else -> it
            }
        } // Use .+ for ** and later on replace with .*
        .replace("**/", ".+/?") // Cover /**/ and **/
        .replace("**", ".+")
        .replace("*", "[^\\\\/]*")
        .replace(".+", ".*") // Convert back ** pattern
        .toRegex()

    fun matches(relativePath: String): Boolean =
        relativePath
            .trim('/')
            .let {
                when {
                    pattern.contains("**") ->
                        regex.find(it).isMatchingInputStart(it)
                    else ->
                        regex.containsMatchIn(it)
                }
            }
}

private fun MatchResult?.isMatchingInputStart(input: String): Boolean =
    this?.value?.let { input.startsWith(it) } ?: false
