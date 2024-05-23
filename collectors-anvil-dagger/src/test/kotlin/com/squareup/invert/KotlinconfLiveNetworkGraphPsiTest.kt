package com.squareup.invert

import com.rickbusarow.statik.InternalStatikApi
import com.rickbusarow.statik.element.kotlin.psi.utils.traversal.PsiTreePrinter.Companion.printEverything
import com.squareup.invert.models.Stat.DiProvidesAndInjectsStat.DiContribution
import com.squareup.psi.classesAndInnerClasses
import com.squareup.psi.toKtFile
import org.jetbrains.kotlin.psi.KtFile
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

@InternalStatikApi
class KotlinconfLiveNetworkGraphPsiTest {

    companion object {
        private val LIVE_NETWORK_GRAPH_CODE = """
        package com.squareup.invert.examples
        
        import com.squareup.anvil.annotations.ContributesBinding
        import com.squareup.invert.examples.di.NetworkGraph
        import com.squareup.invert.examples.models.NetworkConfig
        import com.squareup.invert.examples.repository.CategoryRepo
        import com.squareup.invert.examples.repository.UserRepo
        import com.squareup.invert.examples.scopes.AppScope
        import io.ktor.client.HttpClient
        import javax.inject.Inject
        
        @ContributesBinding(AppScope::class)
        class LiveNetworkGraph @Inject constructor(
            override val userRepo: UserRepo,
            override val categoryRepo: CategoryRepo,
            val networkConfig: NetworkConfig,
            val httpClient: HttpClient,
        ) : NetworkGraph {
        """.trimIndent()
    }


    @Test
    fun printPsiTreeStatik() {
        val ktFile: KtFile = toKtFile(LIVE_NETWORK_GRAPH_CODE)
        ktFile.importDirectives.forEach { println(it)}
    }

    private fun extractDiContributions(code: String): List<DiContribution> {
        val file = File.createTempFile("kotlin", ".kt").apply {
            writeText(code)
        }
        val findAnvil = FindAnvilContributesBinding().apply {
            handleKotlinFile(file, file.parentFile.absolutePath)
        }
        return findAnvil.getCollectedContributesBindings()
    }


    @Test
    fun test() {
        val allCollected = extractDiContributions(LIVE_NETWORK_GRAPH_CODE)
        assertEquals(
            listOf(
                DiContribution(
                    annotation = "com.squareup.anvil.annotations.ContributesBinding",
                    scope = "com.squareup.invert.examples.scopes.AppScope",
                    boundImplementation = "com.squareup.invert.examples.LiveNetworkGraph",
                    boundType = "com.squareup.invert.examples.di.NetworkGraph",
                    replaces = listOf(),
                )
            ), allCollected
        )
    }

}