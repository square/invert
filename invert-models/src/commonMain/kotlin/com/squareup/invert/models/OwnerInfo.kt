package com.squareup.invert.models

import kotlinx.serialization.Serializable

/**
 * Shared model holding Ownership information.  In the future, this may hold more data
 * which is why it is a class and not just a primitive [String]
 */
@Serializable
data class OwnerInfo(
  val name: OwnerName = UNOWNED,
) {
  companion object {
    const val UNOWNED: OwnerName = "Unowned"
  }
}
