package com.squareup.invert.models.js

import com.squareup.invert.models.StatKey
import kotlinx.serialization.Serializable

/**
 * A special class of stats that represent tracking of Tech Debt in a repository, facilitating
 * both snapshot and (when recorded) over time tracking.
 *
 * These can either be:
 *
 *  1. Pure Burndown: in which case only [remainingStatKey] is specified.
 *  2. % Completion: in which case a [completedStatKey] and [remainingStatKey] are specified.
 */
@Serializable
data class TechDebtInitiative(
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
)

@Serializable
data class TechDebtInitiativeConfig(
  val tdis: List<TechDebtInitiative>,
)