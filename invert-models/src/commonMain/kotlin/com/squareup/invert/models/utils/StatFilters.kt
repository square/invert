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
    when (statMetadata.dataType) {
      StatDataType.CODE_REFERENCES -> {
        if (filter.details != null) {
          errors.add("'details' cannot filter '$statKey', a CODE_REFERENCES stat. Use 'extras'.")
        }
        if (filter.extras.isEmpty()) {
          errors.add("The filter for '$statKey' has no 'extras' conditions.")
        }
        val declaredExtraKeys = statMetadata.extras.map { it.key }.toSet()
        filter.extras.keys.filterNot { it in declaredExtraKeys }.forEach { extraKey ->
          errors.add(
            "Extra '$extraKey' is not declared by '$statKey'. " +
              "Declared extras: ${declaredExtraKeys.sorted().ifEmpty { listOf("(none)") }.joinToString()}"
          )
        }
      }

      StatDataType.NUMERIC,
      StatDataType.BOOLEAN -> {
        if (filter.extras.isNotEmpty()) {
          errors.add(
            "'extras' cannot filter '$statKey', a ${statMetadata.dataType} stat. Use 'details'."
          )
        }
        if (filter.details == null) {
          errors.add("The filter for '$statKey' has no 'details' condition.")
        }
      }

      StatDataType.STRING -> {
        errors.add("'$statKey' is a STRING stat, which has no totals and cannot be filtered.")
      }
    }

    val matchers = filter.extras.map { (key, matcher) -> "extras[$key]" to matcher } +
      listOfNotNull(filter.details?.let { "details" to it })
    matchers.forEach { (name, matcher) ->
      errors.addAll(validate(matcher).map { "$name of the filter for '$statKey': $it" })
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
   * Builds a function that applies [filter] to one module's [Stat]:
   *
   * - [Stat.CodeReferencesStat]: keeps only the code references whose extras match.
   * - [Stat.NumericStat] / [Stat.BooleanStat]: keeps the stat if its details match, otherwise
   *   replaces its value with `0` / `false`, so the module still reports the stat.
   *
   * Call [validate] first. A [Stat.StringStat] is returned unchanged.
   */
  fun transform(filter: StatFilter): (Stat) -> Stat {
    val extrasPredicates = filter.extras.map { (key, matcher) -> key to predicate(matcher) }
    val detailsPredicate = filter.details?.let { predicate(it) } ?: { true }
    return { stat ->
      when (stat) {
        is Stat.CodeReferencesStat -> Stat.CodeReferencesStat(
          stat.value.filter { codeReference ->
            extrasPredicates.all { (key, matches) -> matches(codeReference.extras[key]) }
          }
        )

        is Stat.NumericStat -> if (detailsPredicate(stat.details)) stat else stat.copy(value = 0)
        is Stat.BooleanStat -> if (detailsPredicate(stat.details)) stat else stat.copy(value = false)
        is Stat.StringStat -> stat
      }
    }
  }
}
