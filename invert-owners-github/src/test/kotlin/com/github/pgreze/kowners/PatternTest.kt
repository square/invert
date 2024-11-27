package com.github.pgreze.kowners

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PatternTest {

    @Test
    fun `single patterns - single pattern`() {
        // If a separator is at the end, it can only match a folder
        val pattern = Pattern("docs/")

        // match a single directory
        assertTrue(pattern.matches("docs"))
        assertTrue(pattern.matches("docs/"))

        // match a sub-file
        assertTrue(pattern.matches("docs/item"))

    }

    @Test
    fun `single patterns - root based pattern`() {
        val pattern = Pattern("/docs")

        // match a single file
        assertTrue(pattern.matches("docs"))
        assertTrue(pattern.matches("docs/"))


        // not match a sub-file
        assertFalse(pattern.matches("/item/docs/"))

    }

    @Test
    fun `single patterns - sub-tree pattern`() {
        val pattern = Pattern("hello/world")

        // match only root folder
        assertTrue(pattern.matches("hello/world"))
        assertTrue(pattern.matches("/hello/world"))

        // not match as a sub-folder
        assertFalse(pattern.matches("/root/hello/world"))
    }


    @Test
    fun `wildcard patterns - extension based`() {
        val pattern = Pattern("*.txt")

        // match a single file
        assertTrue(pattern.matches("file.txt"))
        // match a sub-file
        assertTrue(pattern.matches("docs/file.txt"))
        // match a deep hierarchy
        assertTrue(pattern.matches("docs/file.txt/rtfm"))
    }


    @Test
    fun `wildcard patterns - any name`() {
        val pattern = Pattern("docs/*")

        // not match a non folder
        assertFalse(pattern.matches("docs"))
        // match a sub-file
        assertTrue(pattern.matches("docs/file"))
        // not match a deep hierarchy
        assertTrue(pattern.matches("docs/sub/file.txt"))
    }


    @Test
    fun `wildcard patterns - partial name`() {
        val pattern = Pattern("/core_*")

        // match a single file
        assertTrue(pattern.matches("core_"))
        assertTrue(pattern.matches("core_ui/"))
        assertTrue(pattern.matches("core_ui.txt"))
        // match a sub-file
        assertTrue(pattern.matches("core_ui/file.txt"))
        // not match a deep hierarchy
        assertFalse(pattern.matches("/tmp/core_ui/file.txt"))
    }

    @Test
    fun `wildcard patterns - double wildcard patterns`() {

        // Equals to the same pattern without **/
        // prefixed pattern
        run {
            val pattern = Pattern("**/docs")

            // match root folder
            assertTrue(pattern.matches("docs"))
            // match a deep hierarchy
            assertTrue(pattern.matches("my/docs/"))
            assertTrue(pattern.matches("my/docs/file.txt"))
        }

        run {
            // describe("suffixed pattern
            val pattern = Pattern("docs/**")

            // not match a non folder
            assertFalse(pattern.matches("docs"))
            // match a sub-file
            assertTrue(pattern.matches("docs/file"))
            assertTrue(pattern.matches("docs/file.txt"))
            // match a deep hierarchy
            assertTrue(pattern.matches("docs/sub/file.txt"))
        }

        run {
            // describe("inside a pattern
            val pattern = Pattern("a/**/b")

            // match 0 depth hierarchy
            assertTrue(pattern.matches("a/b"))
            // match n depth hierarchy
            assertTrue(pattern.matches("a/x/b"))
            assertTrue(pattern.matches("a/x/y/b"))
            // match sub-tree
            assertTrue(pattern.matches("a/x/b/y"))
            assertTrue(pattern.matches("a/x/y/b/z/x"))
            // not match hierarchy inside other folders
            assertFalse(pattern.matches("x/a/b/y"))
            assertFalse(pattern.matches("X/a/y/b/Z"))
        }

        run {

            // partial name
            val pattern = Pattern("/core_[a-zA-Z]")
            // match a single file
            assertTrue(pattern.matches("core_a"))
            assertTrue(pattern.matches("core_b/"))
            assertTrue(pattern.matches("core_Z"))
            // match a sub-file
            assertTrue(pattern.matches("core_a/file.txt"))
        }
    }
}
