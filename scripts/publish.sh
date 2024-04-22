#!/usr/bin/env bash

echo "** Building the invert-report so it can be packaged in the invert-plugin"
./gradlew :invert-report:jsBrowserProductionWebpack

# Wait 2 seconds
sleep 2

echo "** Copy compiled invert-report html & js into invert-plugin"
mkdir -p invert-plugin/src/main/resources/META-INF/
cp invert-report/build/dist/js/invert_web/index.html invert-plugin/src/main/resources/META-INF/index.html
cp invert-report/build/dist/js/invert_web/invert_web.js invert-plugin/src/main/resources/META-INF/invert_web.js

echo "** Publishing to Maven Local"
./gradlew publishToMavenLocal --no-configuration-cache
