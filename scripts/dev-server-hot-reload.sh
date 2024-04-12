#!/usr/bin/env bash

echo "** Running the invert-report module in continuous mode"
./gradlew :invert-report:jsBrowserDevelopmentWebpack --continuous
