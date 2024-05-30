package com.github.pgreze.kowners

import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull

class CodeOwnershipTest : Spek({

    describe("Codeowners comment") {
        it("returns null") {
            assertNull("# A comment".parseCodeOwnersLine())
        }
    }

    describe("Empty Codeowners line") {
        it("returns null") {
            assertNull("".parseCodeOwnersLine())
        }
    }

    describe("Simple Codeowners line") {
        it("returns correct value for 1 owner") {
            val expected = CodeOwnership(
                pattern = Pattern("docs/"),
                owners = listOf("@me")
            )
            assertEquals(expected, "docs/ @me".parseCodeOwnersLine())
        }

        it("returns correct value for multiple owners") {
            val expected = CodeOwnership(
                pattern = Pattern("docs/"),
                owners = listOf("@me", "@org/team-name")
            )
            assertEquals(expected, "docs/ @me @org/team-name".parseCodeOwnersLine())
        }
    }

    describe("Codeowners line with escaped space") {
        it("returns correct value") {
            val expected = CodeOwnership(
                pattern = Pattern("docs/\\ hello"),
                owners = listOf("@me")
            )
            assertEquals(expected, "docs/\\ hello @me".parseCodeOwnersLine())
        }
    }

    describe("Codeowners line with invalid owners") {
        it("returns correct message for no owner") {
            assertFails("No owner in line: docs/") { "docs/".parseCodeOwnersLine() }
        }
    }
})
