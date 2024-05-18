package com.squareup.invert.common.charts

import androidx.compose.runtime.Composable
import com.squareup.invert.models.InvertSerialization
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Canvas
import renderChartJs

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
        val data: List<Int>,
        val borderWidth: Int = 1,
    )

    @Serializable
    data class ChartJsData(
        val labels: List<String>,
        val datasets: List<ChartJsDataset>,
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
            maxHeight(300.px)
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


@Composable
fun ChartJsChartComposable(
    domId: String,
    type: String,
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
