// Use external functions to call JS functions defined in global scope

external fun externalLoadJavaScriptFile(key: String, callback: (json: String) -> Unit)

external fun loadJsFileAsync(url: String, callback: () -> Unit)

external fun markdownToHtml(markdown: String): String

external fun keysForObject(any: Any): Array<String>

external fun callDecodeURIComponent(str: String): String

external fun highlightJsHighlightAll()

/**
 * Render Directed Graph with force-graph library.
 *
 * @param domElementId Dom Element id to render the graph in
 * @param graphDataJson Graph Data serialized to JSON
 */
external fun render3dGraph(domElementId: String, graphDataJson: String, width: Int, height: Int)

/** Render Pie and Bar Charts with Chart.js */
external fun renderChartJs(domElementId: String, graphDataJson: String, onClick: (label: String, value: Int) -> Unit)

/** Render Line Charts with Chart.js */
external fun renderLineChartJs(domElementId: String, graphDataJson: String, onClick: (label: String, value: Int) -> Unit)

/** Render Tree Map with Plotly */
external fun renderPlotlyTreeMap(domElementId: String, graphDataJson: String, onClick: (label: String, value: Int) -> Unit)
