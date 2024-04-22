#!/usr/bin/env bash

echo "** Building the invert-report so it can be packaged in the invert-plugin"
./gradlew :invert-report:jsBrowserProductionWebpack --no-daemon --no-configuration-cache

echo "** Copy compiled invert-report html & js into invert-plugin"
mkdir -p invert-plugin/src/main/resources/META-INF/
cp invert-report/build/kotlin-webpack/js/invert_web/invert_web.js invert-plugin/src/main/resources/META-INF/invert_web.js
cp invert-report/build/processedResources/js/main/index.html invert-plugin/src/main/resources/META-INF/index.html

echo "** Publishing to Maven Local"
./gradlew publishToMavenLocal --no-daemon --no-configuration-cache
