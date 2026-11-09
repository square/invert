package com.squareup.invert.models

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * Selects part of a [StatDataType.CODE_REFERENCES] stat by its code references' extras, so that a
 * [com.squareup.invert.models.js.TechDebtInitiative] can track a subset of an existing stat.
 *
 * These classes always encode their non-null defaults, so that consumers of `tdi_config.json`
 * that are not written in Kotlin see every field.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class StatFilter(
  /**
   * Conditions on a code reference's [Stat.CodeReferencesStat.CodeReference.extras], by extra key.
   * A code reference is kept when all of them match. Every key must be declared in the stat's
   * [StatMetadata.extras].
   */
  @EncodeDefault
  val extras: Map<ExtraKey, ValueMatcher> = emptyMap(),
  /**
   * (Optional) The key of the derived stat that holds the filtered result. Defaults to
   * [com.squareup.invert.models.js.TechDebtInitiative.derivedStatKey].
   *
   * History is kept by this key. Set it to version the definition independently of the
   * initiative's id, and change it when you change what the filter selects, so that the old and
   * new definitions do not share one history.
   */
  val key: StatKey? = null,
)

/**
 * How a [ValueMatcher] compares a value.
 */
enum class MatchOp {
  /** Matches when the value equals any of [ValueMatcher.values]. */
  ONE_OF,

  /**
   * Matches when any of [ValueMatcher.values], read as a regular expression, is found anywhere in
   * the value. Anchor the pattern with `^` and `$` to match the whole value.
   */
  REGEX,

  /** Matches when the value is present, whatever it is. [ValueMatcher.values] is ignored. */
  PRESENT,
}

/**
 * Matches a single, possibly missing, [String] value.
 *
 * A missing value never matches [MatchOp.ONE_OF] or [MatchOp.REGEX]. With [negate], a missing
 * value therefore matches: "not one of X" includes items that have no value at all.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ValueMatcher(
  val op: MatchOp,
  @EncodeDefault
  val values: List<String> = emptyList(),
  /** Inverts the result. */
  @EncodeDefault
  val negate: Boolean = false,
) {

  /** A copy of this matcher with the result inverted. */
  fun negated(): ValueMatcher = copy(negate = !negate)

  companion object {
    fun equalTo(value: String): ValueMatcher = ValueMatcher(MatchOp.ONE_OF, listOf(value))

    fun oneOf(vararg values: String): ValueMatcher = ValueMatcher(MatchOp.ONE_OF, values.toList())

    fun regex(vararg patterns: String): ValueMatcher = ValueMatcher(MatchOp.REGEX, patterns.toList())

    fun present(): ValueMatcher = ValueMatcher(MatchOp.PRESENT)
  }
}
