package com.squareup.invert.models.utils

import com.squareup.invert.models.MatchOp
import com.squareup.invert.models.Stat
import com.squareup.invert.models.StatDataType
import com.squareup.invert.models.StatFilter
import com.squareup.invert.models.StatMetadata
import com.squareup.invert.models.ValueMatcher

/**
 * Validation and evaluation of [StatFilter]s, shared by the build and the web report so that
 * both apply the same rules.
 */
object StatFilters {

  /**
   * Returns a description of every problem with applying [filter] to the stat described by
   * [statMetadata], or an empty list if there are none.
   */
  fun validate(filter: StatFilter, statMetadata: StatMetadata): List<String> {
    val errors = mutableListOf<String>()
    val statKey = statMetadata.key
    if (statMetadata.dataType != StatDataType.CODE_REFERENCES) {
      errors.add(
        "'$statKey' is a ${statMetadata.dataType} stat. Only CODE_REFERENCES stats can be filtered."
      )
    }
    if (filter.key?.isBlank() == true) {
      errors.add("The filter for '$statKey' has a blank 'key'. Remove it to use the default key.")
    }
    if (filter.extras.isEmpty()) {
      errors.add("The filter for '$statKey' has no 'extras' conditions.")
    }
    if (statMetadata.dataType == StatDataType.CODE_REFERENCES) {
      val declaredExtraKeys = statMetadata.extras.map { it.key }.toSet()
      filter.extras.keys.filterNot { it in declaredExtraKeys }.forEach { extraKey ->
        errors.add(
          "Extra '$extraKey' is not declared by '$statKey'. " +
            "Declared extras: ${declaredExtraKeys.sorted().ifEmpty { listOf("(none)") }.joinToString()}"
        )
      }
    }
    filter.extras.forEach { (extraKey, matcher) ->
      errors.addAll(validate(matcher).map { "extras[$extraKey] of the filter for '$statKey': $it" })
    }
    return errors
  }

  private fun validate(matcher: ValueMatcher): List<String> = buildList {
    when (matcher.op) {
      MatchOp.ONE_OF -> if (matcher.values.isEmpty()) add("ONE_OF needs at least one value.")
      MatchOp.REGEX -> {
        if (matcher.values.isEmpty()) add("REGEX needs at least one pattern.")
        matcher.values.forEach { pattern ->
          try {
            Regex(pattern)
          } catch (e: Exception) {
            add("Invalid regular expression '$pattern': ${e.message}")
          }
        }
      }

      MatchOp.PRESENT -> Unit
    }
  }

  /** Builds a predicate for [matcher]. Regular expressions are compiled once, here. */
  fun predicate(matcher: ValueMatcher): (String?) -> Boolean {
    val matches: (String) -> Boolean = when (matcher.op) {
      MatchOp.ONE_OF -> matcher.values.toSet()::contains
      MatchOp.REGEX -> {
        val regexes = matcher.values.map(::Regex)
        fun(value: String): Boolean = regexes.any { it.containsMatchIn(value) }
      }

      MatchOp.PRESENT -> fun(_: String): Boolean = true
    }
    val negate = matcher.negate
    return fun(value: String?): Boolean = (value != null && matches(value)) != negate
  }

  /**
   * Builds a function that applies [filter] to one module's [Stat.CodeReferencesStat], keeping
   * only the code references whose extras all match. A module with no matches keeps an empty
   * stat, so a finished initiative reads `0`.
   *
   * Call [validate] first. Other stat types are returned unchanged.
   */
  fun transform(filter: StatFilter): (Stat) -> Stat {
    val extrasPredicates = filter.extras.map { (key, matcher) -> key to predicate(matcher) }
    return { stat ->
      when (stat) {
        is Stat.CodeReferencesStat -> Stat.CodeReferencesStat(
          stat.value.filter { codeReference ->
            extrasPredicates.all { (key, matches) -> matches(codeReference.extras[key]) }
          }
        )

        else -> stat
      }
    }
  }
}
