package ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.models.FileKey
import com.squareup.invert.models.js.JsReportFileKey
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H4
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text


@Composable
fun NavBarComposable(loadingProgressFlow: Flow<List<FileKey>>) {
  val outstandingCalls by loadingProgressFlow.collectAsState(listOf())
  if (outstandingCalls.isNotEmpty()) {
    H4 {
      Span({ classes("pe-4") }) {
        Text(
          "Loading... ${
            outstandingCalls.map { fileKey ->
              JsReportFileKey.entries.firstOrNull { it.key == fileKey }?.description ?: fileKey
            }
          }"
        )
      }
      Div({
        classes("spinner-border text-light".split(" "))
        attr("role", "status")
      }) {
        Span({
          classes("visually-hidden")
        }) {
          Text("Loading...")
        }
      }
    }
  }
}