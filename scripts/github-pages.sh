#!/usr/bin/env bash

REPOS=(
"https://github.com/square/anvil"
"https://github.com/chrisbanes/tivi"
"https://github.com/android/nowinandroid"
#"https://github.com/Jetbrains/kotlin"
)

./scripts/publish.sh
./gradlew --init-script invert.gradle :invert

STATIC_SITE_FOLDER="build/static"
rm -Rf $STATIC_SITE_FOLDER
mkdir -p $STATIC_SITE_FOLDER
HTML_FILE="$STATIC_SITE_FOLDER/index.html"

mkdir -p "$STATIC_SITE_FOLDER/invert/invert"
cp -p -r "build/reports/invert" "$STATIC_SITE_FOLDER/invert/invert"


echo "<html><head><link rel='stylesheet' href='https://cdn.jsdelivr.net/npm/bootstrap@3.4.1/dist/css/bootstrap.min.css' crossorigin='anonymous'></head>" >> $HTML_FILE
echo "<body><h1>Invert Reports</h1><br/><ul>" >> $HTML_FILE
echo "<li><a href='invert/invert/index.html'>invert</a></li>" >> $HTML_FILE

for REPO_URL in ${REPOS[@]}; do
    echo "./scripts/invert-clone-and-run.sh $REPO_URL"
    ./scripts/invert-clone-and-run.sh $REPO_URL

    # String after last '/'
    REPO_NAME=`echo "$REPO_URL" | awk -F'/' '{print $NF}'`
    echo "Moving Report"
    mkdir -p "$STATIC_SITE_FOLDER/$REPO_NAME"
    if [ $REPO_NAME == "anvil" ]
    then
        # Special root build dir in Anvil Repo
        BUILD_FOLDER="build/clones/$REPO_NAME/build/root-build"
    else
        BUILD_FOLDER="build/clones/$REPO_NAME/build"
    fi
    cp -r "$BUILD_FOLDER/reports/invert" "$STATIC_SITE_FOLDER/$REPO_NAME"
    echo "<li><a href='$REPO_NAME/invert/index.html'>$REPO_NAME</a> ($REPO_URL)</li>" >> $HTML_FILE
done

echo "</ul></body></html>" >> $HTML_FILE