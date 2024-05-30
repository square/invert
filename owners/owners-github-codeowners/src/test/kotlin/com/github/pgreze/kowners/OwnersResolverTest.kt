package com.github.pgreze.kowners

import com.google.common.truth.Truth.assertThat
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class OwnersResolverTest : Spek({

    describe("docs/sample project") {
        val ownersResolver = CODEOWNERS.split("\n")
            .filter(String::isNotEmpty)
            .parseCodeOwners()
            .let(::OwnersResolver)

        val fileToOwners = FILE_TO_OWNER.keys
            .map { it to ownersResolver.resolveOwnership(it) }
            .toMap()

        it("resolve all owners") {
            assertThat(fileToOwners).isEqualTo(FILE_TO_OWNER)
        }
    }
})

private const val CODEOWNERS = """
*.md              charlie
sf.md             boss
CODEOWNERS        boss
dir/alice/        alice
dir/bob/          bob
dir/*.md          maintainer
"""

private val FILE_TO_OWNER = mapOf(
    "dir/alice/report.txt" to listOf("alice"),
    "dir/bob/notes.md" to listOf("bob"),
    "dir/f3.txt" to null,
    "dir/docs.md" to listOf("maintainer"),
    "dir/read.md" to listOf("maintainer"),
    "charlie.md" to listOf("charlie"),
    "CODEOWNERS" to listOf("boss"),
    "f1.txt" to null,
    "f2.txt" to null,
    "sf.md" to listOf("boss")
)
