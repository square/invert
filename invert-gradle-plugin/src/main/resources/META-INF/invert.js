window.invert_report = window.invert_report || {};

window.keysForObject = function (obj) {
    let newKeys = []
    let keys = obj.keys()
    let nextKey = keys.next();
    while (!nextKey.done) {
        newKeys.push(nextKey.value)
        nextKey = keys.next();
    }
    return newKeys
}

window.loadJsFileAsync = function (url, notifyCompleteCallback) {
    let s = document.createElement('script');
    s.setAttribute('src', url);
    s.onload = function () {
        notifyCompleteCallback();
    };
    document.body.appendChild(s);
}

window.invertKeyToUrl = function (key) {
    return "js/" + key + ".js";
}

window.externalLoadJavaScriptFile = function (key, callback) {
    let src = invertKeyToUrl(key)
    loadJsFileAsync(src, function () {
        let json = JSON.stringify(window.invert_report[key])
        // null out global variable since it has been serialized to json
        window.invert_report[key] = null;
        callback(json);
    })
}

// https://github.com/vasturiano/force-graph
// https://unpkg.com/force-graph
window.render3dGraph = function (domElementId, graphDataJson, width, height) {
    if (window.ForceGraph == undefined) {
        loadJsFileAsync("https://unpkg.com/force-graph", function (obj) {
            const graphData = JSON.parse(graphDataJson)
            const Graph = ForceGraph()
            (document.getElementById(domElementId))
                .graphData(graphData)
                .nodeId('id')
                .nodeAutoColorBy('group')
                .dagMode('rl')
                .width(width)
                .height(height)
                .nodeCanvasObject((node, ctx, globalScale) => {
                    const label = node.id;
                    const fontSize = 12 / globalScale;
                    ctx.font = `${fontSize}px Sans-Serif`;
                    const textWidth = ctx.measureText(label).width;
                    const bckgDimensions = [textWidth, fontSize].map(n => n + fontSize * 0.2);

                    ctx.fillStyle = 'rgba(255, 255, 255, 0.8)';
                    ctx.fillRect(node.x - bckgDimensions[0] / 2, node.y - bckgDimensions[1] / 2, ...bckgDimensions);

                    ctx.textAlign = 'center';
                    ctx.textBaseline = 'middle';
                    ctx.fillStyle = node.color;
                    ctx.fillText(label, node.x, node.y);

                    node.__bckgDimensions = bckgDimensions;
                })
                .onNodeClick(node => {
                    Graph.centerAt(node.x, node.y, 1000);
                    Graph.zoom(8, 2000);
                })
                .nodePointerAreaPaint((node, color, ctx) => {
                    ctx.fillStyle = color;
                    const bckgDimensions = node.__bckgDimensions;
                    bckgDimensions && ctx.fillRect(node.x - bckgDimensions[0] / 2, node.y - bckgDimensions[1] / 2, ...bckgDimensions);
                });
        });
    }
}

// https://www.chartjs.org/docs/latest/getting-started/
window.renderChartJs = function (domElementId, graphDataJson, onClick) {
    if (window.ForceGraph == undefined) {
        loadJsFileAsync("https://cdn.jsdelivr.net/npm/chart.js", function (obj) {
            const chartJsData = JSON.parse(graphDataJson)
            const ctx = document.getElementById(domElementId);
            chartJsData['options']['onClick'] = function (event, elements) {
                if (elements.length > 0) {
                    var elementIndex = elements[0].index;
                    var label = this.data.labels[elementIndex];
                    var value = this.data.datasets[0].data[elementIndex];
                    onClick(label, value)
                }
            }
            // chartJsData['type']='bar'
            chartJsData['options']['indexAxis'] = 'y'
            new Chart(ctx, chartJsData);
        });
    }
}

window.markdownToHtml = function (markdownStr) {
    return marked.parse(markdownStr);
}

window.callDecodeURIComponent = function (str) {
    return decodeURIComponent(escape(str));
}

window.highlightJsHighlightAll = function () {
    hljs.highlightAll();
}