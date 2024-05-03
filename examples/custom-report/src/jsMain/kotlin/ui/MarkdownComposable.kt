package ui

import androidx.compose.runtime.*
import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import loadJsFileAsync
import markdownToHtml
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLElement

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
    val lambda: (Any) -> Unit = {
        val json = JSON.stringify(it)
        val result = json1.decodeFromString<GitHubRepositoryContentsResult>(json)
        val resultContentEncoded = result.data.content
        val decodedString = window.atob(resultContentEncoded)
        markdownCallback(decodedString)
    }

    val callbackName = "callback_${urlToIdString(url)}"
    window.asDynamic()[callbackName] = lambda
    loadJsFileAsync("$url?callback=$callbackName") {
        window.asDynamic()[callbackName] = null
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


@Composable
fun RemoteGitHubMarkdown(url: String) {
    val isMarkedLibLoaded by MarkedLoaded.isLoaded.collectAsState()

    if (!isMarkedLibLoaded) {
        MarkedLoaded.load()
        BootstrapLoadingSpinner()
        return
    }

    // State to hold the fetched HTML
    var htmlContent by remember { mutableStateOf<String?>(null) }

    // Fetch the HTML content asynchronously
    LaunchedEffect(Unit) {
        val markdown = loadRemoteMarkdownFromGitHubUrl(url)
        htmlContent = markdownToHtml(markdown)
    }

    if (htmlContent == null) {
        BootstrapLoadingSpinner()
        return
    } else {
        RawHtmlComposable(htmlContent!!)
    }
}
