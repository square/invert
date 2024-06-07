package com.github.pgreze.kowners

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull

class CodeOwnershipTest {

    @Test
    fun `Codeowners comment`() {
        assertNull("# A comment".parseCodeOwnersLine())
    }

    @Test
    fun `Empty Codeowners line`() {
        assertNull("".parseCodeOwnersLine())
    }

    @Test
    fun `Simple Codeowners line - returns correct value for 1 owner`() {
        val expected = CodeOwnership(
            pattern = Pattern("docs/"),
            owners = listOf("@me")
        )
        assertEquals(expected, "docs/ @me".parseCodeOwnersLine())
    }

    @Test
    fun `Simple Codeowners line - returns correct value for multiple owners`() {
        val expected = CodeOwnership(
            pattern = Pattern("docs/"),
            owners = listOf("@me", "@org/team-name")
        )
        assertEquals(expected, "docs/ @me @org/team-name".parseCodeOwnersLine())
    }

    @Test
    fun `Codeowners line with escaped space - returns correct value`() {
        val expected = CodeOwnership(
            pattern = Pattern("docs/\\ hello"),
            owners = listOf("@me")
        )
        assertEquals(expected, "docs/\\ hello @me".parseCodeOwnersLine())
    }

    @Test
    fun `Codeowners line with invalid owners returns correct message for no owner`() {
        assertFails("No owner in line: docs/") { "docs/".parseCodeOwnersLine() }
    }

}
