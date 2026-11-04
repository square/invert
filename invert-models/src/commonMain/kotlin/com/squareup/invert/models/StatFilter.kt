package com.squareup.invert.models

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * Selects part of a [Stat], so that a [com.squareup.invert.models.js.TechDebtInitiative] can track
 * a subset of an existing stat.
 *
 * Every condition that is set must match.
 *
 * These classes use only concrete types and always encode their non-null defaults, so that
 * consumers which read `tdi_config.json` with Gson (which cannot create interfaces and ignores
 * Kotlin default values) can parse them.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class StatFilter(
  /**
   * [StatDataType.CODE_REFERENCES] stats only. Each entry must match the code reference's
   * [Stat.CodeReferencesStat.CodeReference.extras] value for that key. Every key must be declared
   * in the stat's [StatMetadata.extras].
   */
  @EncodeDefault
  val extras: Map<ExtraKey, ValueMatcher> = emptyMap(),
  /**
   * [StatDataType.NUMERIC] and [StatDataType.BOOLEAN] stats only. Matches the stat's `details`.
   *
   * These stats hold one value per module, so a module whose `details` do not match contributes
   * `0` (or `false`) instead of its value.
   */
  val details: ValueMatcher? = null,
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
