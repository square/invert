package com.squareup.invert

import com.rickbusarow.statik.InternalStatikApi
import com.rickbusarow.statik.element.kotlin.psi.utils.traversal.PsiTreePrinter.Companion.printEverything
import com.squareup.psi.classesAndInnerClasses
import com.squareup.psi.toKtFile
import kotlin.test.Test

@InternalStatikApi
class StatikDemoTest {

    @Test
    fun statik() {
        val kotlinFileText =
            """
package com.squareup.invert
import com.rickbusarow.statik.InternalStatikApi
import com.rickbusarow.statik.element.kotlin.psi.utils.traversal.PsiTreePrinter.Companion.printEverything
import com.squareup.psi.classesAndInnerClasses
import com.squareup.psi.toKtFile
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import kotlin.test.Test

@InternalStatikApi
class StatikDemoTest {

    @Test
    fun statik() {
        val ktFile = toKtFile(
            content = "",
        )
        ktFile.classesAndInnerClasses().forEach {
            it.printEverything()
        }
    }
}
        """.trimIndent()

        val ktFile = toKtFile(
            content = kotlinFileText,
        )
        ktFile.classesAndInnerClasses().forEach {
            it.printEverything()
        }
    }
}