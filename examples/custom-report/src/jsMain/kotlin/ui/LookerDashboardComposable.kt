package ui

import androidx.compose.runtime.Composable
import com.squareup.invert.models.OwnerName
import kotlinx.browser.window
import org.jetbrains.compose.web.dom.Iframe

@Composable
fun AndroidModuleMetricsDashboard() {
  FullScreenIframe("https://github.com/squareup/invert")
}

@Composable
fun FullScreenIframe(src: String) {
  Iframe({
    attr("src", src)
    attr("width", "100%")
    attr("height", window.innerHeight.toString() + "px")
  })
}