package com.github.pgreze.kowners

import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PatternTest : Spek({

    describe("single patterns") {

        context("single pattern") {
            // If a separator is at the end, it can only match a folder
            val pattern = Pattern("docs/")

            it("match a single directory") {
                assertTrue(pattern.matches("docs"))
                assertTrue(pattern.matches("docs/"))
            }

            it("match a sub-file") {
                assertTrue(pattern.matches("docs/item"))
            }
        }

        context("root based pattern") {
            val pattern = Pattern("/docs")

            it("match a single file") {
                assertTrue(pattern.matches("docs"))
                assertTrue(pattern.matches("docs/"))
            }

            it("not match a sub-file") {
                assertFalse(pattern.matches("/item/docs/"))
            }
        }

        context("sub-tree pattern") {
            val pattern = Pattern("hello/world")

            it("match only root folder") {
                assertTrue(pattern.matches("hello/world"))
                assertTrue(pattern.matches("/hello/world"))
            }

            it("not match as a sub-folder") {
                assertFalse(pattern.matches("/root/hello/world"))
            }
        }
    }

    // An asterisk "*" matches anything except a slash.
    // The character "?" matches any one character except a slash.

    describe("wildcard patterns") {

        context("extension based") {
            val pattern = Pattern("*.txt")

            it("match a single file") {
                assertTrue(pattern.matches("file.txt"))
            }
            it("match a sub-file") {
                assertTrue(pattern.matches("docs/file.txt"))
            }
            it("match a deep hierarchy") {
                assertTrue(pattern.matches("docs/file.txt/rtfm"))
            }
        }

        context("any name") {
            val pattern = Pattern("docs/*")

            it("not match a non folder") {
                assertFalse(pattern.matches("docs"))
            }
            it("match a sub-file") {
                assertTrue(pattern.matches("docs/file"))
            }
            it("not match a deep hierarchy") {
                assertTrue(pattern.matches("docs/sub/file.txt"))
            }
        }

        context("partial name") {
            val pattern = Pattern("/core_*")

            it("match a single file") {
                assertTrue(pattern.matches("core_"))
                assertTrue(pattern.matches("core_ui/"))
                assertTrue(pattern.matches("core_ui.txt"))
            }
            it("match a sub-file") {
                assertTrue(pattern.matches("core_ui/file.txt"))
            }
            it("not match a deep hierarchy") {
                assertFalse(pattern.matches("/tmp/core_ui/file.txt"))
            }
        }
    }

    describe("double wildcard patterns") {

        // Equals to the same pattern without **/
        describe("prefixed pattern") {
            val pattern = Pattern("**/docs")

            it("match root folder") {
                assertTrue(pattern.matches("docs"))
            }
            it("match a deep hierarchy") {
                assertTrue(pattern.matches("my/docs/"))
                assertTrue(pattern.matches("my/docs/file.txt"))
            }
        }

        describe("suffixed pattern") {
            val pattern = Pattern("docs/**")

            it("not match a non folder") {
                assertFalse(pattern.matches("docs"))
            }
            it("match a sub-file") {
                assertTrue(pattern.matches("docs/file"))
                assertTrue(pattern.matches("docs/file.txt"))
            }
            it("match a deep hierarchy") {
                assertTrue(pattern.matches("docs/sub/file.txt"))
            }
        }

        describe("inside a pattern") {
            val pattern = Pattern("a/**/b")

            it("match 0 depth hierarchy") {
                assertTrue(pattern.matches("a/b"))
            }
            it("match n depth hierarchy") {
                assertTrue(pattern.matches("a/x/b"))
                assertTrue(pattern.matches("a/x/y/b"))
            }
            it("match sub-tree") {
                assertTrue(pattern.matches("a/x/b/y"))
                assertTrue(pattern.matches("a/x/y/b/z/x"))
            }
            it("not match hierarchy inside other folders") {
                assertFalse(pattern.matches("x/a/b/y"))
                assertFalse(pattern.matches("X/a/y/b/Z"))
            }
        }
    }

    describe("range patterns") {

        context("partial name") {
            val pattern = Pattern("/core_[a-zA-Z]")

            it("match a single file") {
                assertTrue(pattern.matches("core_a"))
                assertTrue(pattern.matches("core_b/"))
                assertTrue(pattern.matches("core_Z"))
            }
            it("match a sub-file") {
                assertTrue(pattern.matches("core_a/file.txt"))
            }
        }
    }
})
