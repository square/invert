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

/** Render a Plotly treemap chart from a list of file paths */
window.renderPlotlyTreeMap = function (domElementId, filePaths, onClick) {
    loadJsFileAsync("https://cdn.plot.ly/plotly-2.35.2.min.js", function (obj) {
        function generateCsvFromPaths(input) {
            const paths = input.split('\n').filter(Boolean); // Split by newlines and filter out empty lines
            const ids = new Set();
            const rows = [];

            paths.forEach(path => {
                const parts = path.split('/');
                for (let i = 0; i < parts.length; i++) {
                    const currentPath = parts.slice(0, i + 1).join('/');
                    const parentPath = i > 0 ? parts.slice(0, i).join('/') : '';
                    if (!ids.has(currentPath)) {
                        ids.add(currentPath);
                        rows.push({
                            ids: currentPath,
                            labels: parts[i],
                            parents: parentPath
                        });
                    }
                }
            });

            // Convert rows to CSV
            const csv = ['ids,labels,parents'];
            rows.forEach(row => {
                csv.push(`${row.ids},${row.labels},${row.parents}`);
            });

            return csv.join('\n');
        }

        const csvContent = generateCsvFromPaths(filePaths);


        // Parse the CSV content
        function parseCSV(content) {
            const rows = content.trim().split("\n").map(row => row.split(","));
            const keys = rows[0]; // Extract headers
            return rows.slice(1).map(row => {
                const obj = {};
                row.forEach((value, index) => {
                    obj[keys[index]] = value;
                });
                return obj;
            });
        }

        // Unpack function to extract data for a specific key
        function unpack(rows, key) {
            return rows.map(row => row[key]);
        }

        // Parse the CSV and prepare the data
        const rows = parseCSV(csvContent);

        const plotlyData = [{
            type: "treemap",
            ids: unpack(rows, "ids"),
            labels: unpack(rows, "labels"),
            parents: unpack(rows, "parents"),
        }];

        // Layout configuration to remove margins
        const layout = {
            margin: {t: 0, l: 0, r: 0, b: 0}, // Removes top, left, right, and bottom margins
            // title: {
            //     text: 'Hide the Modebar'
            // },
            showlegend: true
        };

        // Configuration to remove Plotly logo and download button
        const config = {
            displayModeBar: false, // Removes the mode bar (including download button)
        };

        // Render the Plotly chart
        Plotly.newPlot(domElementId, plotlyData, layout, config);
    });
}

/**
 * Allows someone to define a "customizeInvert" function to be called before it is loaded.
 */
if (typeof window.configureInvert === 'function') {
    window.configureInvert();
}
