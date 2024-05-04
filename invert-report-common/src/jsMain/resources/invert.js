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
        notifyCompleteCallback()
    };
    document.body.appendChild(s);
}

window.invertKeyToUrl = function (key) {
    return "js/" + key + ".js"
}

window.externalLoadJavaScriptFile = function (key, callback) {
    let src = invertKeyToUrl(key)
    loadJsFileAsync(src, function () {
        let json = JSON.stringify(window.invert_report[key])
        callback(json)
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
window.markdownToHtml = function (markdownStr) {
    return marked.parse(markdownStr)
}

//function githubMarkdownCallback(response) {
//    let decodedContent = atob(response.data.content);
//    let div = document.createElement("div");
//    div.innerHTML = marked.parse(decodedContent);
//    document.getElementsByTagName('body')[0].append(div);
//}

//setTimeout(function () {
//
//    let script = document.createElement('script');
//    script.src = 'https://api.github.com/repos/square/anvil/contents/README.md?callback=githubMarkdownCallback';
//    document.getElementsByTagName('head')[0].appendChild(script);
//}, 1000)

