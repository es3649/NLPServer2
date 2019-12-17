#!/bin/bash

echo "Looking for .jar files..."

LIBS="$(find -name "*.jar")"

# echo "Found the following libs:"
# for jar in $LIBS; do
#     echo " -| $jar"
# done
echo

LIBS=".:$(echo $LIBS | sed "s@ @:@g"):"

# echo "$LIBS"

find -name "*.java" | xargs javac -g -d out/ -classpath "$LIBS"