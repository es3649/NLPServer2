#!/bin/bash

if [[ $# -lt 1 ]]; then
    echo "usage: ./run.sh CLASSNAME [args]"
    exit 0
fi

echo "Looking for .jar files..."

LIBS="$(find -name "*.jar")"

echo "Found the following libs:"
for jar in $LIBS; do
    echo " -| $jar"
done
echo "Also using default classpath: ./bin"
echo

LIBS=".:./out/:$(echo $LIBS | sed "s@ @:@g"):"

# echo "$LIBS"

java -mx7g -cp "$LIBS" -ea $@
