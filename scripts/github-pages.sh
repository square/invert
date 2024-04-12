#!/usr/bin/env bash

./scripts/publish.sh
./gradlew --init-script invert.gradle :invert
