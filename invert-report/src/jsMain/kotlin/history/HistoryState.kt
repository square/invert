package history

import kotlinx.serialization.Serializable

/**
 * A Simple wrapper around a Map<String,String>
 */
@Serializable
data class HistoryState(val params: Map<String, String>)
