package ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import loadJsFileAsync
import markdownToHtml
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLElement
import kotlin.collections.set

@Serializable
data class GitHubRepositoryContentsResult(
    val data: GitHubRepositoryContentsResultData
)

@Serializable
data class GitHubRepositoryContentsResultData(
    val content: String
)

private val json1 = Json {
    ignoreUnknownKeys = true
}

private object MarkedLoaded {
    val isLoaded = MutableStateFlow(false)

    private var isLoading = false

    fun load() {
        if (!isLoaded.value && !isLoading) {
            isLoading = true
            loadJsFileAsync("https://cdn.jsdelivr.net/npm/marked/marked.min.js") {
                isLoaded.value = true
                isLoading = false
            }
        }
    }
}

fun loadRemoteMarkdownFromGitHubUrl(url: String, markdownCallback: (String) -> Unit) {
    val callbackName = "callback_${urlToIdString(url)}"
    println("Requesting $url")
    val lambda: (Any) -> Unit = {
        val json = JSON.stringify(it)
        val result = json1.decodeFromString<GitHubRepositoryContentsResult>(json)
        val resultContentEncoded = result.data.content
        val decodedString = window.atob(resultContentEncoded)
        markdownCallback(decodedString)
        println("Nulling out $callbackName")
        window.asDynamic()[callbackName] = null
    }

    window.asDynamic()[callbackName] = lambda
    println("Callback added $window.get(callbackName)")
    loadJsFileAsync("$url?callback=$callbackName") {
    }
}

suspend fun loadRemoteMarkdownFromGitHubUrl(url: String): String {
    val completableDeferred = CompletableDeferred<String>()
    loadRemoteMarkdownFromGitHubUrl(url) {
        completableDeferred.complete(it)
    }
    return completableDeferred.await()
}

@Composable
fun RawHtmlComposable(htmlString: String) {
    Div(
        attrs = {
            id("html-container")
            ref {
                it as HTMLElement
                it.innerHTML = htmlString
                onDispose {
                    it.innerHTML = ""  // Clean up when the composable is removed
                }
            }
        }
    )
}

@Composable
fun RenderMarkdown(markdown: String) {
    val isMarkedLibLoaded by MarkedLoaded.isLoaded.collectAsState()

    if (!isMarkedLibLoaded) {
        MarkedLoaded.load()
        BootstrapLoadingSpinner()
        return
    }

    val html = markdownToHtml(markdown)
    RawHtmlComposable(html)
}

fun urlToIdString(url: String): String {
    val rgx = Regex("[^a-zA-Z0-9 -]")
    return rgx.replace(url.substringAfter("//"), "_")
}


// State to hold the fetched HTML

sealed interface MarkdownLoadingState {
    object Loading : MarkdownLoadingState
    data class Loaded(val markdown: String) : MarkdownLoadingState
}

object MarkdownRepo {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val urlToMarkdownFlow = MutableStateFlow<Map<String, MarkdownLoadingState>>(mapOf())

    fun update(key: String, value: MarkdownLoadingState) {
        urlToMarkdownFlow.value = urlToMarkdownFlow.value.toMutableMap().apply {
            this[key] = value
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun load(url: String): Flow<MarkdownLoadingState> {
        coroutineScope.launch {
            if (urlToMarkdownFlow.value[url] == null) {
                update(url, MarkdownLoadingState.Loading)
                val markdown = loadRemoteMarkdownFromGitHubUrl(url)
                val markdownHtml = markdownToHtml(markdown)
                update(url, MarkdownLoadingState.Loaded(markdownHtml))
            }
        }
        return urlToMarkdownFlow.mapLatest { it[url] ?: MarkdownLoadingState.Loading }
    }
}


@Composable
fun RemoteGitHubMarkdown(url: String) {
    val isMarkedLibLoaded by MarkedLoaded.isLoaded.collectAsState()

    if (!isMarkedLibLoaded) {
        MarkedLoaded.load()
        BootstrapLoadingSpinner()
        return
    }

    // State to hold the fetched HTML
    val htmlContentForCurrentUrl: MarkdownLoadingState by MarkdownRepo.load(url)
        .collectAsState(MarkdownLoadingState.Loading)

    MarkdownRepo.load(url)
    val local = htmlContentForCurrentUrl
    when (local) {
        is MarkdownLoadingState.Loaded -> {
            RawHtmlComposable(local.markdown)
        }

        else -> {
            BootstrapLoadingSpinner()
        }
    }
}
