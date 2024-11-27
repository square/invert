package com.squareup.invert.common.charts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.squareup.invert.models.InvertSerialization
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.web.css.CSSSizeValue
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.border
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.maxHeight
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.style
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Canvas
import org.jetbrains.compose.web.dom.Div
import renderChartJs
import renderLineChartJs
import renderPlotlyTreeMap
import kotlin.math.absoluteValue
import kotlin.random.Random

object ChartsJs {

  @Serializable
  data class ScaleInfo(
    val beginAtZero: Boolean = true,
  )

  @Serializable
  data class ChartJsScales(
    val y: ScaleInfo = ScaleInfo(),
  )

  @Serializable
  data class ChartJsOptions(
    val scales: ChartJsScales,
  )

  @Serializable
  data class ChartJsDataset(
    val label: String,
    val data: Collection<Int>,
    val borderWidth: Int = 1,
  )

  @Serializable
  data class ChartJsData(
    val labels: Collection<String>,
    val datasets: Collection<ChartJsDataset>,
  )

  @Serializable
  data class ChartJsParam(
    val type: String,
    val data: ChartJsData,
    val options: ChartJsOptions,
  )
}

@Composable
private fun LineChartJsComposable(
  graphDomId: String,
  chartData: ChartsJs.ChartJsParam,
  heightCssValue: CSSSizeValue<*>,
  onClick: (label: String, value: Int) -> Unit = { _, _ -> }
) {
  Canvas({
    id(graphDomId)
    style {
      border {
        width(1.px)
        style(LineStyle.Solid)
        color = Color.gray
      }
      height(heightCssValue)
      maxWidth(100.percent)
    }
  })
  CoroutineScope(Dispatchers.Main).launch {
    renderLineChartJs(
      graphDomId,
      InvertSerialization.InvertJson.encodeToString(
        ChartsJs.ChartJsParam.serializer(), chartData
      ),
      onClick = onClick
    )
  }
}

@Composable
private fun ChartJsComposable(
  graphDomId: String,
  chartData: ChartsJs.ChartJsParam,
  heightCssValue: CSSSizeValue<*>,
  onClick: (label: String, value: Int) -> Unit = { _, _ -> }
) {
  Canvas({
    id(graphDomId)
    style {
      border {
        width(2.px)
        style(LineStyle.Solid)
        color = Color.black
      }
      height(heightCssValue)
      maxHeight(heightCssValue)
      maxWidth(100.percent)
    }
  })
  CoroutineScope(Dispatchers.Main).launch {
    renderChartJs(
      graphDomId,
      InvertSerialization.InvertJson.encodeToString(
        ChartsJs.ChartJsParam.serializer(), chartData
      ),
      onClick = onClick
    )
  }
}


private val random = Random(0)

@Composable
fun ChartJsChartComposable(
  domId: String = "chart-${random.nextInt().absoluteValue}",
  type: String = "pie",
  height: CSSSizeValue<*> = 400.px,
  data: ChartsJs.ChartJsData,
  onClick: (String, Int) -> Unit = { _, _ -> }
) {
  val domId = remember { "plotly-${random.nextInt().absoluteValue}" } // Remember the domId
  ChartJsComposable(
    graphDomId = domId,
    heightCssValue = height,
    chartData = ChartsJs.ChartJsParam(
      type = type,
      data = data,
      options = ChartsJs.ChartJsOptions(
        scales = ChartsJs.ChartJsScales(
          y = ChartsJs.ScaleInfo(
            beginAtZero = true
          )
        )
      )
    ),
    onClick = onClick,
  )
}


@Composable
fun PlotlyTreeMapComposable(
  filePaths: List<String>,
  onClick: (String, Int) -> Unit = { _, _ -> }
) {
  val domId = remember { "plotly-${random.nextInt().absoluteValue}" } // Remember the domId

  Div({
    id(domId)
  })

  CoroutineScope(Dispatchers.Main).launch {
    renderPlotlyTreeMap(
      domId,
      filePaths.joinToString("\n"),
      onClick = onClick
    )
  }
}

@Composable
fun ChartJsLineChartComposable(
  domId: String = "chart-${random.nextInt().absoluteValue}",
  height: CSSSizeValue<*> = 260.px,
  data: ChartsJs.ChartJsData,
  onClick: (String, Int) -> Unit = { _, _ -> }
) {
  LineChartJsComposable(
    graphDomId = domId,
    heightCssValue = height,
    chartData = ChartsJs.ChartJsParam(
      type = "line",
      data = data,
      options = ChartsJs.ChartJsOptions(
        scales = ChartsJs.ChartJsScales(
          y = ChartsJs.ScaleInfo(
            beginAtZero = true
          )
        )
      )
    ),
    onClick = onClick,
  )
}
