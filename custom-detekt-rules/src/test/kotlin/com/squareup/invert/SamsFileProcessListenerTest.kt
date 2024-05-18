package com.squareup.invert

import io.github.detekt.test.utils.compileContentForTest
import org.jetbrains.kotlin.resolve.BindingContext
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class SamsFileProcessListenerTest {

    @Ignore
    @Test
    fun `should expect two loops`() {
        val code = """
            fun main() {
                for (i in 0..10) {
                      while (i < 5) {
                        println(i)
                      }
                }
            }
        """

        val ktFile = compileContentForTest(code)
        SamsFileProcessListener().onProcess(ktFile, BindingContext.EMPTY)

        val actual = ktFile.getUserData(SamsFileProcessListener.numberOfLoopsKey)
        assertEquals(
            expected = 2,
            actual = actual,
        )
    }
}