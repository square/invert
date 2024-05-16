package com.squareup.invert

import com.squareup.invert.testutils.InvertTestUtils.assertEqualsAndPrintDiff
import kotlinx.serialization.builtins.ListSerializer


import java.io.File
import kotlin.test.Test

class FindDiContributionTest {

    @Test
    fun `boundType specified and binding found`() {
        val ktFile = File.createTempFile("kotlin", ".kt").apply {
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
            handleKotlinFile(ktFile)
        }

        assertEqualsAndPrintDiff(
            expected = listOf(
                AnvilContributesBinding(
                    annotation = "com.squareup.anvil.annotations.ContributesBinding",
                    scope = "com.squareup.dagger.AppScope",
                    boundImplementation = "com.squareup.test.AccountStatusRootAuthenticator",
                    boundType = "com.squareup.test.RootAuthenticator1",
                    replaces = listOf(),
                    fileName = ktFile.name
                )
            ),
            actual = findAnvil.getCollectedContributesBindings(),
            serializer = ListSerializer(AnvilContributesBinding.serializer())
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
            handleKotlinFile(ktFile)
        }

        assertEqualsAndPrintDiff(
            expected = listOf(
                AnvilContributesBinding(
                    annotation = "com.squareup.anvil.annotations.ContributesBinding",
                    scope = "com.squareup.dagger.AppScope",
                    boundImplementation = "com.squareup.test.AccountStatusRootAuthenticator",
                    boundType = "com.squareup.test.RootAuthenticator",
                    replaces = listOf(),
                    fileName = ktFile.name
                )
            ),
            actual = findAnvil.getCollectedContributesBindings(),
            serializer = ListSerializer(AnvilContributesBinding.serializer())
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
            handleKotlinFile(ktFile)
        }

        assertEqualsAndPrintDiff(
            expected = listOf(
                AnvilContributesBinding(
                    annotation = "com.squareup.anvil.annotations.ContributesBinding",
                    scope = "com.squareup.dagger.AppScope",
                    boundImplementation = "com.squareup.test.AccountStatusRootAuthenticator1",
                    boundType = "com.squareup.test.RootAuthenticator1",
                    replaces = listOf(),
                    fileName = ktFile.name
                ),
                AnvilContributesBinding(
                    annotation = "com.squareup.anvil.annotations.ContributesBinding",
                    scope = "com.squareup.test.OtherScope",
                    boundImplementation = "com.squareup.test.AccountStatusRootAuthenticator2",
                    boundType = "com.squareup.test.RootAuthenticator2",
                    replaces = listOf(),
                    fileName = ktFile.name
                )
            ),
            actual = findAnvil.getCollectedContributesBindings(),
            serializer = ListSerializer(AnvilContributesBinding.serializer())
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
            handleKotlinFile(ktFile)
        }

        assertEqualsAndPrintDiff(
            expected = listOf(
                AnvilContributesBinding(
                    annotation = "com.squareup.anvil.annotations.ContributesBinding",
                    scope = "com.squareup.dagger.AppScope",
                    boundImplementation = "com.squareup.test.AccountStatusRootAuthenticator",
                    boundType = "com.squareup.test.RootAuthenticator",
                    replaces = listOf(
                        "com.squareup.test.ToBeReplaced"
                    ),
                    fileName = ktFile.name
                )
            ),
            actual = findAnvil.getCollectedContributesBindings(),
            serializer = ListSerializer(AnvilContributesBinding.serializer())
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
            handleKotlinFile(ktFile)
        }

        assertEqualsAndPrintDiff(
            expected = listOf(
                AnvilContributesBinding(
                    annotation = "com.squareup.anvil.annotations.ContributesBinding",
                    scope = "com.squareup.dagger.AppScope",
                    boundImplementation = "com.squareup.test.AccountStatusRootAuthenticator",
                    boundType = "com.squareup.test.RootAuthenticator",
                    replaces = listOf(
                        "com.squareup.test.ToBeReplaced1",
                        "com.squareup.test.ToBeReplaced2"
                    ),
                    fileName = ktFile.name
                )
            ),
            actual = findAnvil.getCollectedContributesBindings(),
            serializer = ListSerializer(AnvilContributesBinding.serializer())
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
            handleKotlinFile(ktFile)
        }

        assertEqualsAndPrintDiff(
            expected = listOf(),
            actual = findAnvil.getCollectedContributesBindings(),
            serializer = ListSerializer(AnvilContributesBinding.serializer())
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