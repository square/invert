package com.squareup.invert

import com.squareup.invert.models.Stat.DiProvidesAndInjectsStat.DiContribution
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class LiveNetworkGraphPsiTest {

    @Test
    fun test() {
        val code = """
        @ContributesBinding(AppScope::class)
        class LiveNetworkGraph @Inject constructor(
            override val userRepo: UserRepo,
            override val categoryRepo: CategoryRepo,
            val networkConfig: NetworkConfig,
            val httpClient: HttpClient,
        ) : NetworkGraph {
        """.trimIndent()
        val allCollected = extractDiContributions(code)
        assertEquals(
            listOf(DiContribution(
                annotation = "com.squareup.anvil.annotations.ContributesBinding",
                scope = "com.squareup.invert.examples.scopes.AppScope",
                boundImplementation = "com.squareup.invert.examples.LiveNetworkGraph",
                boundType = "com.squareup.invert.examples.di.NetworkGraph",
                replaces = listOf(),
            )), allCollected
        )
    }

    private fun extractDiContributions(code: String): List<DiContribution> {
        val file = File.createTempFile("kotlin", ".kt").apply {
            writeText(packageAndImports + "\n" + code)
        }
        val findAnvil = FindAnvilContributesBinding().apply {
            handleKotlinFile(file, file.parentFile.absolutePath)
        }
        return findAnvil.getCollectedContributesBindings()
    }

    val packageAndImports = """
        package com.squareup.invert.examples
        
        import com.squareup.anvil.annotations.ContributesBinding
        import com.squareup.invert.examples.di.NetworkGraph
        import com.squareup.invert.examples.models.NetworkConfig
        import com.squareup.invert.examples.repository.CategoryRepo
        import com.squareup.invert.examples.repository.UserRepo
        import com.squareup.invert.examples.scopes.AppScope
        import io.ktor.client.HttpClient
        import javax.inject.Inject
    """.trimIndent()
}