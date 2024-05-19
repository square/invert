package ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import callDecodeURIComponent
import highlightJsHighlightAll
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import loadJsFileAsync
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

fun loadRemoteContentFromGitHubUrl(url: String, markdownCallback: (String) -> Unit) {
    val callbackName = "callback_${urlToIdString(url)}"
    println("Requesting $url")
    val lambda: (Any) -> Unit = {
        val json = JSON.stringify(it)
        val result = json1.decodeFromString<GitHubRepositoryContentsResult>(json)
        val resultContentEncoded = result.data.content
        val fromBinary = window.atob(resultContentEncoded)
        val decodedString = callDecodeURIComponent(fromBinary)
        markdownCallback(decodedString)
        window.asDynamic()[callbackName] = null
    }

    window.asDynamic()[callbackName] = lambda
    println("Callback added $window.get(callbackName)")
    loadJsFileAsync("$url?callback=$callbackName") {
    }
}

suspend fun loadRemoteContentFromGitHubUrl(url: String): String {
    val completableDeferred = CompletableDeferred<String>()
    loadRemoteContentFromGitHubUrl(url) {
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

//@Composable
//fun RenderMarkdown(markdown: String) {
//    val isMarkedLibLoaded by MarkedLoaded.isLoaded.collectAsState()
//
//    if (!isMarkedLibLoaded) {
//        MarkedLoaded.load()
//        BootstrapLoadingSpinner()
//        return
//    }
//
//    val html = markdownToHtml(markdown)
//    RawHtmlComposable(html)
//}

fun urlToIdString(url: String): String {
    val rgx = Regex("[^a-zA-Z0-9 -]")
    return rgx.replace(url.substringAfter("//"), "_")
}


// State to hold the fetched HTML

sealed interface RemoteGitHubContentLoadingState {
    object Loading : RemoteGitHubContentLoadingState
    data class Loaded(val content: String) : RemoteGitHubContentLoadingState
}

object MarkdownRepo {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val urlToMarkdownFlow = MutableStateFlow<Map<String, RemoteGitHubContentLoadingState>>(mapOf())

    fun update(key: String, value: RemoteGitHubContentLoadingState) {
        urlToMarkdownFlow.value = urlToMarkdownFlow.value.toMutableMap().apply {
            this[key] = value
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun load(url: String): Flow<RemoteGitHubContentLoadingState> {
        coroutineScope.launch {
            if (urlToMarkdownFlow.value[url] == null) {
                update(url, RemoteGitHubContentLoadingState.Loading)
                val content = loadRemoteContentFromGitHubUrl(url)
                update(url, RemoteGitHubContentLoadingState.Loaded(content))
            }
        }
        return urlToMarkdownFlow.mapLatest { it[url] ?: RemoteGitHubContentLoadingState.Loading }
    }
}

@Composable
fun RemoteGitHubContent(url: String, transform: @Composable (String) -> Unit) {
    val isMarkedLibLoaded by MarkedLoaded.isLoaded.collectAsState()

    if (!isMarkedLibLoaded) {
        MarkedLoaded.load()
        BootstrapLoadingSpinner()
        return
    }

    // State to hold the fetched HTML
    val htmlContentForCurrentUrl: RemoteGitHubContentLoadingState by MarkdownRepo.load(url)
        .collectAsState(RemoteGitHubContentLoadingState.Loading)

    val local = htmlContentForCurrentUrl
    when (local) {
        is RemoteGitHubContentLoadingState.Loaded -> {
            transform(local.content)
            CoroutineScope(Dispatchers.Main).launch {
                delay(100)
                highlightJsHighlightAll()
            }
        }
        else -> {
            BootstrapLoadingSpinner()
        }
    }
}


//<pre><code class="language-html">...</code></pre>