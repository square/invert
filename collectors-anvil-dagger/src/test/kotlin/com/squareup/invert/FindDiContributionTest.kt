@file:OptIn(InternalStatikApi::class)

package com.squareup.invert


import com.rickbusarow.statik.InternalStatikApi
import com.rickbusarow.statik.element.kotlin.psi.utils.traversal.PsiTreePrinter.Companion.printEverything
import com.squareup.invert.models.Stat.DiProvidesAndInjectsStat.DiContribution
import com.squareup.invert.InvertTestUtils.assertEqualsAndPrintDiff
import com.squareup.psi.classesAndInnerClasses
import com.squareup.psi.toKtFile
import kotlinx.serialization.builtins.ListSerializer
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import java.io.File
import kotlin.test.Test

class FindDiContributionTest {

    @Test
    fun `boundType specified and binding found`() {
        val file = File.createTempFile("kotlin", ".kt").apply {
            writeText(
                """
        $PACKAGE_AND_IMPORTS
        interface RootAuthenticator1 {}
        interface RootAuthenticator2 {}
        
        @ContributesBinding(AppScope::class, boundType = RootAuthenticator1::class)
        class AccountStatusRootAuthenticator : RootAuthenticator2, RootAuthenticator1 {}
        """.trimIndent()
            )
        }
        val findAnvil = FindAnvilContributesBinding().apply {
            handleKotlinFile(file, file.parentFile.absolutePath)
        }

        assertEqualsAndPrintDiff(
            expected = listOf(
                DiContribution(
                    annotation = "com.squareup.anvil.annotations.ContributesBinding",
                    scope = "com.squareup.dagger.AppScope",
                    boundImplementation = "com.squareup.test.AccountStatusRootAuthenticator",
                    boundType = "com.squareup.test.RootAuthenticator1",
                    replaces = listOf(),
                )
            ),
            actual = findAnvil.getCollectedContributesBindings(),
            serializer = ListSerializer(DiContribution.serializer())
        )
    }

    @Test
    fun `boundType not specified and binding found`() {
        val ktFile = File.createTempFile("kotlin", ".kt").apply {
            writeText(
                """
        $PACKAGE_AND_IMPORTS
        interface RootAuthenticator {}
        
        @ContributesBinding(AppScope::class)
        class AccountStatusRootAuthenticator : RootAuthenticator {}
        """.trimIndent()
            )
        }
        val findAnvil = FindAnvilContributesBinding().apply {
            handleKotlinFile(ktFile, ktFile.parentFile.absolutePath)
        }

        assertEqualsAndPrintDiff(
            expected = listOf(
                DiContribution(
                    annotation = "com.squareup.anvil.annotations.ContributesBinding",
                    scope = "com.squareup.dagger.AppScope",
                    boundImplementation = "com.squareup.test.AccountStatusRootAuthenticator",
                    boundType = "com.squareup.test.RootAuthenticator",
                    replaces = listOf(),
                )
            ),
            actual = findAnvil.getCollectedContributesBindings(),
            serializer = ListSerializer(DiContribution.serializer())
        )
    }

    @Test
    fun `multiple bindings found`() {
        val ktFile = File.createTempFile("kotlin", ".kt").apply {
            writeText(
                """
        $PACKAGE_AND_IMPORTS
        interface RootAuthenticator1 {}
        interface RootAuthenticator2 {}
        
        interface OtherScope {}
        
        @ContributesBinding(AppScope::class)
        class AccountStatusRootAuthenticator1 : RootAuthenticator1 {}
        
        @ContributesBinding(OtherScope::class)
        class AccountStatusRootAuthenticator2 : RootAuthenticator2 {}
        """.trimIndent()
            )
        }
        val findAnvil = FindAnvilContributesBinding().apply {
            handleKotlinFile(ktFile, ktFile.parentFile.absolutePath)
        }

        assertEqualsAndPrintDiff(
            expected = listOf(
                DiContribution(
                    annotation = "com.squareup.anvil.annotations.ContributesBinding",
                    scope = "com.squareup.dagger.AppScope",
                    boundImplementation = "com.squareup.test.AccountStatusRootAuthenticator1",
                    boundType = "com.squareup.test.RootAuthenticator1",
                    replaces = listOf(),
                ),
                DiContribution(
                    annotation = "com.squareup.anvil.annotations.ContributesBinding",
                    scope = "com.squareup.test.OtherScope",
                    boundImplementation = "com.squareup.test.AccountStatusRootAuthenticator2",
                    boundType = "com.squareup.test.RootAuthenticator2",
                    replaces = listOf(),
                )
            ),
            actual = findAnvil.getCollectedContributesBindings(),
            serializer = ListSerializer(DiContribution.serializer())
        )
    }

    @Test
    fun `single replaces specified and binding found`() {
        val ktFile = File.createTempFile("kotlin", ".kt").apply {
            writeText(
                """
        $PACKAGE_AND_IMPORTS
        interface RootAuthenticator {}
        
        class ToBeReplaced : RootAuthenticator {}
        
        @ContributesBinding(AppScope::class, replaces = [ToBeReplaced::class])
        class AccountStatusRootAuthenticator : RootAuthenticator {}
        """.trimIndent()
            )
        }
        val findAnvil = FindAnvilContributesBinding().apply {
            handleKotlinFile(ktFile, ktFile.parentFile.absolutePath)
        }

        assertEqualsAndPrintDiff(
            expected = listOf(
                DiContribution(
                    annotation = "com.squareup.anvil.annotations.ContributesBinding",
                    scope = "com.squareup.dagger.AppScope",
                    boundImplementation = "com.squareup.test.AccountStatusRootAuthenticator",
                    boundType = "com.squareup.test.RootAuthenticator",
                    replaces = listOf(
                        "com.squareup.test.ToBeReplaced"
                    ),
                )
            ),
            actual = findAnvil.getCollectedContributesBindings(),
            serializer = ListSerializer(DiContribution.serializer())
        )
    }

    @Test
    fun `multiple replaces specified and binding found`() {
        val ktFile = File.createTempFile("kotlin", ".kt").apply {
            writeText(
                """
        $PACKAGE_AND_IMPORTS
        interface RootAuthenticator {}
        
        class ToBeReplaced1 : RootAuthenticator {}
        class ToBeReplaced2 : RootAuthenticator {}
        
        @ContributesBinding(AppScope::class, replaces = [ToBeReplaced1::class, ToBeReplaced2::class])
        class AccountStatusRootAuthenticator : RootAuthenticator {}
        """.trimIndent()
            )
        }
        val findAnvil = FindAnvilContributesBinding().apply {
            handleKotlinFile(ktFile, ktFile.parentFile.absolutePath)
        }

        assertEqualsAndPrintDiff(
            expected = listOf(
                DiContribution(
                    annotation = "com.squareup.anvil.annotations.ContributesBinding",
                    scope = "com.squareup.dagger.AppScope",
                    boundImplementation = "com.squareup.test.AccountStatusRootAuthenticator",
                    boundType = "com.squareup.test.RootAuthenticator",
                    replaces = listOf(
                        "com.squareup.test.ToBeReplaced1",
                        "com.squareup.test.ToBeReplaced2"
                    ),
                )
            ),
            actual = findAnvil.getCollectedContributesBindings(),
            serializer = ListSerializer(DiContribution.serializer())
        )
    }

    @Test
    fun `no bindings`() {
        val ktFile = File.createTempFile("kotlin", ".kt").apply {
            writeText(
                """
        $PACKAGE_AND_IMPORTS
        interface RootAuthenticator {}
        
        class AccountStatusRootAuthenticator : RootAuthenticator {}
        """.trimIndent()
            )
        }
        val findAnvil = FindAnvilContributesBinding().apply {
            handleKotlinFile(ktFile, ktFile.parentFile.absolutePath)
        }

        assertEqualsAndPrintDiff(
            expected = listOf(),
            actual = findAnvil.getCollectedContributesBindings(),
            serializer = ListSerializer(DiContribution.serializer())
        )
    }

    companion object {
        val PACKAGE_AND_IMPORTS =
            """
      package com.squareup.test
        
      import com.squareup.anvil.annotations.ContributesBinding
      import com.squareup.dagger.AppScope
      """.trimIndent()
    }
}