#!/bin/bash

if [[ "$(ls)" != "" ]]; then
    rm -r docs/*
fi

# fins libraries
LIBS="$(find -name "*.jar")"

echo "Found the following libs:"
for jar in $LIBS; do
    echo " -| $jar"
done
echo

LIBS=".:$(echo $LIBS | sed "s@ @:@g; s@\./@../@g"):"

pushd src/
find -name "*.java" | xargs javadoc -d ../docs -classpath "$LIBS"
popd