window.invert_report = window.invert_report || {};

// Bootstrap 5.3 data-bs-theme only supports "light" and "dark".
// "auto" is not natively handled, so we detect system preference and set it.
(function() {
    var html = document.documentElement;
    if (html.getAttribute('data-bs-theme') === 'auto') {
        var prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
        html.setAttribute('data-bs-theme', prefersDark ? 'dark' : 'light');
        // Update if system preference changes mid-session
        if (window.matchMedia) {
            window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', function(e) {
                if (!html.dataset.userOverride) {
                    html.setAttribute('data-bs-theme', e.matches ? 'dark' : 'light');
                }
            });
        }
    }
})();

window.invertIsDarkMode = function() {
    var theme = document.documentElement.getAttribute('data-bs-theme');
    if (theme === 'dark') return true;
    if (theme === 'light') return false;
    return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
};

window.applyChartJsTheme = function() {
    if (typeof Chart === 'undefined') return;
    var rootStyles = getComputedStyle(document.documentElement);
    var textColor = rootStyles.getPropertyValue('--bs-body-color').trim();
    var borderColor = rootStyles.getPropertyValue('--bs-border-color').trim();
    if (textColor) Chart.defaults.color = textColor;
    if (borderColor) Chart.defaults.borderColor = borderColor;
};

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

/**
 * Load a stat key via chunked JSON transport when a manifest is available.
 * Falls back to legacy externalLoadJavaScriptFile only when no manifest exists.
 *
 * Flow: fetch manifest → fetch all chunks in parallel → merge statsByModule → callback(json)
 */
window.externalLoadChunkedJsonFile = function (key, callback, errorCallback) {
    function chunkTransportUnavailable(message) {
        var err = new Error(message);
        err.noChunkManifest = true;
        return err;
    }

    function fetchWithTimeout(url, timeoutMs, responseHandler) {
        var controller = typeof AbortController === "function" ? new AbortController() : null;
        var timeoutId;
        var timeout = new Promise(function (_, reject) {
            timeoutId = setTimeout(function () {
                if (controller) {
                    controller.abort();
                }
                reject(new Error("Timed out fetching " + url));
            }, timeoutMs);
        });
        var request = Promise.resolve().then(function () {
            return controller ? fetch(url, { signal: controller.signal }) : fetch(url);
        }).then(function (response) {
            return responseHandler ? responseHandler(response) : response;
        });
        return Promise.race([request, timeout]).then(
            function (response) {
                clearTimeout(timeoutId);
                return response;
            },
            function (err) {
                clearTimeout(timeoutId);
                throw err;
            }
        );
    }

    function fetchJson(url, attemptsRemaining) {
        return fetchWithTimeout(
            url,
            30000,
            function (response) {
                if (!response.ok) throw new Error("Failed to fetch " + url + " (" + response.status + ")");
                return response.json();
            }
        )
            .catch(function (err) {
                if (attemptsRemaining > 1) {
                    return fetchJson(url, attemptsRemaining - 1);
                }
                throw err;
            });
    }

    var manifestUrl = "js/" + key + ".manifest.json";
    fetchWithTimeout(
        manifestUrl,
        30000,
        function (response) {
            if (!response.ok) {
                throw chunkTransportUnavailable("Manifest unavailable for " + key + " (" + response.status + ")");
            }
            return response.json().catch(function (err) {
                err.invalidChunkManifest = true;
                throw err;
            });
        }
    )
        .catch(function (err) {
            if (err.noChunkManifest || err.invalidChunkManifest) {
                throw err;
            }
            throw chunkTransportUnavailable("Unable to fetch manifest for " + key + ": " + err.message);
        })
        .then(function (manifest) {
            if (!manifest.chunkFiles || manifest.chunkFiles.length === 0) {
                throw new Error("Chunk manifest for " + key + " does not list any chunk files");
            }
            return Promise.all(
                manifest.chunkFiles.map(function (chunkFile) {
                    return fetchJson("js/" + chunkFile, 3);
                })
            );
        })
        .then(function (chunks) {
            var merged = { statInfo: chunks[0].statInfo, statsByModule: {} };
            chunks.forEach(function (chunk) {
                var modules = chunk.statsByModule;
                var keys = Object.keys(modules);
                for (var i = 0; i < keys.length; i++) {
                    merged.statsByModule[keys[i]] = modules[keys[i]];
                }
            });
            callback(JSON.stringify(merged));
        })
        .catch(function (err) {
            if (err.noChunkManifest) {
                console.log("Chunked load unavailable for " + key + ", falling back to legacy: " + err.message);
                externalLoadJavaScriptFile(key, callback);
                return;
            }
            var errorMessage = "Chunked load failed for " + key + ": " + err.message;
            console.error(errorMessage);
            if (typeof errorCallback === "function") {
                errorCallback(errorMessage);
            }
        });
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
                .backgroundColor(window.invertIsDarkMode() ? '#212529' : '#ffffff')
                .nodeCanvasObject((node, ctx, globalScale) => {
                    const label = node.id;
                    const fontSize = 12 / globalScale;
                    ctx.font = `${fontSize}px Sans-Serif`;
                    const textWidth = ctx.measureText(label).width;
                    const bckgDimensions = [textWidth, fontSize].map(n => n + fontSize * 0.2);

                    ctx.fillStyle = window.invertIsDarkMode() ? 'rgba(33, 37, 41, 0.8)' : 'rgba(255, 255, 255, 0.8)';
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
            window.applyChartJsTheme();
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

/**
 * Render a Plotly treemap chart from a list of file paths
 *
 * https://plotly.com/javascript/treemaps/
 */
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
        var rootStyles = getComputedStyle(document.documentElement);
        var bgColor = rootStyles.getPropertyValue('--bs-body-bg').trim() || '#ffffff';
        var textColor = rootStyles.getPropertyValue('--bs-body-color').trim() || '#212529';
        const layout = {
            margin: {t: 0, l: 0, r: 0, b: 0}, // Removes top, left, right, and bottom margins
            showlegend: true,
            paper_bgcolor: bgColor,
            font: { color: textColor }
        };

        // Configuration to remove Plotly logo and download button
        const config = {
            displayModeBar: false, // Removes the mode bar (including download button)
        };

        // Render the Plotly chart
        Plotly.newPlot(domElementId, plotlyData, layout, config);
    });
}

/** Render Line Charts with Chart.js */
window.renderLineChartJs = function (domElementId, graphDataJson, onClick) {
    if (window.Chart === undefined) {
        loadJsFileAsync("https://cdn.jsdelivr.net/npm/chart.js", function (obj) {
            renderLineChartJs(domElementId, graphDataJson, onClick)
        });
    } else {
        const chartJsData = JSON.parse(graphDataJson)
        console.log(chartJsData)
        const ctx = document.getElementById(domElementId).getContext('2d');
        window.applyChartJsTheme();
        new Chart(ctx, chartJsData);
    }
}

/**
 * Allows someone to define a "customizeInvert" function to be called before it is loaded.
 */
if (typeof window.configureInvert === 'function') {
    window.configureInvert();
}
