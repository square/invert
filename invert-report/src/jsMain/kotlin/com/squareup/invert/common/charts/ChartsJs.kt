package com.squareup.invert.common.charts

import androidx.compose.runtime.Composable
import com.squareup.invert.models.InvertSerialization
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.border
import org.jetbrains.compose.web.css.maxHeight
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.style
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Canvas
import renderChartJs
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
private fun ChartJsComposable(
    graphDomId: String,
    chartData: ChartsJs.ChartJsParam,
    onClick: (label: String, value: Int) -> Unit = { _, _ -> }
) {
    // Good to go
    Canvas({
        id(graphDomId)
        style {
            border {
                width(2.px)
                style(LineStyle.Solid)
                color = Color.black
            }
            maxHeight(400.px)
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
    data: ChartsJs.ChartJsData,
    onClick: (String, Int) -> Unit = { _, _ -> }
) {
    ChartJsComposable(
        graphDomId = domId,
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
