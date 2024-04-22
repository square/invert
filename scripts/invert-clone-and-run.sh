#!/usr/bin/env bash

REPO_URL=$1

# String after last '/'
REPO_NAME=`echo "$REPO_URL" | awk -F'/' '{print $NF}'`

CLONE_FOLDER="build/clones/$REPO_NAME"

echo "git clone --depth=1 $REPO_URL $CLONE_FOLDER"
git clone --depth=1 $REPO_URL $CLONE_FOLDER

echo "cd $CLONE_FOLDER"
cd $CLONE_FOLDER

echo "pwd"
pwd

echo "./gradlew --init-script ../../../invert.gradle :invert"
# -Dorg.gradle.ignoreBuildJavaVersionCheck=true is needed for https://github.com/gradle/gradle for the Java 11/Java 17 mix
./gradlew --init-script ../../../invert.gradle :invert -Dorg.gradle.ignoreBuildJavaVersionCheck=true

cd ../../..
