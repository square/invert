package ui

import androidx.compose.runtime.Composable
import kotlinx.browser.window
import org.jetbrains.compose.web.dom.Iframe

@Composable
fun FullScreenIframe(src: String) {
  Iframe({
    attr("src", src)
    attr("width", "100%")
    attr("height", window.innerHeight.toString() + "px")
  })
}
