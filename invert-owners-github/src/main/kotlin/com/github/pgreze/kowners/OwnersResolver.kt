/**
 * Implementation from https://github.com/pgreze/kowners
 */
package com.github.pgreze.kowners

class OwnersResolver(
  val ownerships: List<CodeOwnership>
) {
  fun resolveOwnership(path: String): List<String>? =
    ownerships.lastOrNull { it.pattern.matches(path) }?.owners
}
