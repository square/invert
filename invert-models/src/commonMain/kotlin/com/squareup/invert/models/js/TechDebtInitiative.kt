package com.squareup.invert.models.js

import com.squareup.invert.models.StatFilter
import com.squareup.invert.models.StatKey
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

/**
 * A special class of stats that represent tracking of Tech Debt in a repository, facilitating
 * both snapshot and (when recorded) over time tracking.
 *
 * These can either be:
 *
 *  1. Pure Burndown: in which case only [remainingStatKey] is specified.
 *  2. % Completion: in which case a [completedStatKey] and [remainingStatKey] are specified.
 *
 * Either stat, when it is a code references stat, can be narrowed with a [StatFilter]
 * ([remainingFilter], [completedFilter]). The filtered result is published at build time as a
 * derived stat, under [effectiveRemainingStatKey] and [effectiveCompletedStatKey], so that it gets
 * totals, history and exports like any other stat. Read the effective keys to show or export an
 * initiative's numbers.
 *
 * A derived stat's history is kept by its key: [StatFilter.key], or [derivedStatKey] of [id] when
 * it is not set. If you change what a filter selects, also change that key, so the old and new
 * definitions do not share one history.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class TechDebtInitiative @JvmOverloads constructor(
  /** Unique identifier for this initiative, should remain constant for deep-linking **/
  val id: String,
  /** Display title **/
  val title: String,
  /** Markdown Format full description of this Tech Debt Initiative. **/
  val descriptionMarkdown: String,
  /** Eg. Files, Tests, Methods, etc. **/
  val unit: String,
  /** (Optional) If there is a 'completed' stat associated with this TDI that identifies
   * the code references where it has already been addressed. **/
  val completedStatKey: StatKey?,
  /** The remaining code references of this TDI that need to be addressed. **/
  val remainingStatKey: StatKey,
  /** (Optional) Only count the part of [remainingStatKey] that matches this filter. **/
  val remainingFilter: StatFilter? = null,
  /** (Optional) Only count the part of [completedStatKey] that matches this filter. **/
  val completedFilter: StatFilter? = null,
) {
  /**
   * The stat to read for this initiative's remaining work: [remainingStatKey], or the derived
   * stat when [remainingFilter] is set ([StatFilter.key], or [derivedStatKey] by default).
   *
   * Computed from the other properties. It is written to `tdi_config.json` for consumers that
   * cannot compute it, such as shell scripts.
   */
  @EncodeDefault
  val effectiveRemainingStatKey: StatKey =
    remainingFilter?.let { it.key ?: derivedStatKey(id, StatRole.REMAINING) } ?: remainingStatKey

  /**
   * The stat to read for this initiative's completed work: [completedStatKey], or the derived
   * stat when [completedFilter] is set. `null` when there is no [completedStatKey].
   *
   * Computed from the other properties, and written to `tdi_config.json` like
   * [effectiveRemainingStatKey].
   */
  @EncodeDefault
  val effectiveCompletedStatKey: StatKey? = completedStatKey?.let { completedStatKey ->
    completedFilter?.let { it.key ?: derivedStatKey(id, StatRole.COMPLETED) } ?: completedStatKey
  }

  /** Which of an initiative's two stats a key refers to. */
  enum class StatRole(val keySuffix: String, val displayName: String) {
    REMAINING("remaining", "Remaining"),
    COMPLETED("completed", "Completed"),
  }

  companion object {
    private const val DERIVED_STAT_KEY_PREFIX = "tdi__"

    /** The default key of the derived stat for an initiative's filtered [role] stat. */
    fun derivedStatKey(id: String, role: StatRole): StatKey =
      "$DERIVED_STAT_KEY_PREFIX${id}__${role.keySuffix}"
  }
}

@Serializable
data class TechDebtInitiativeConfig(
  val tdis: List<TechDebtInitiative>,
)
