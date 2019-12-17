#!/bin/bash

## look for libs
echo "Looking for .jar files..."

LIBS="$(find -name "*.jar")"

echo "Found the following libs:"
for jar in $LIBS; do
    echo " -| $jar"
done

LIBS=".:bin/:$(echo $LIBS | sed "s@ @:@g"):"

echo $LIBS