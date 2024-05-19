// Use external functions to call JS functions defined in global scope

external fun externalLoadJavaScriptFile(key: String, callback: (json: String) -> Unit)


external fun loadJsFileAsync(url: String, callback: () -> Unit)

external fun markdownToHtml(markdown: String): String

external fun keysForObject(any: Any): Array<String>

external fun callDecodeURIComponent(str: String): String

external fun highlightJsHighlightAll()


/**
 * @param domElementId Dom Element id to render the graph in
 * @param graphDataJson Graph Data serialized to JSON
 */
external fun render3dGraph(domElementId: String, graphDataJson: String, width: Int, height: Int)

external fun renderChartJs(domElementId: String, graphDataJson: String, onClick: (label: String, value: Int) -> Unit)
