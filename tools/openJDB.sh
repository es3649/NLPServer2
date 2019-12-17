#!/bin/bash

echo "Looking for .jar files..."

LIBS="$(find -name "*.jar")"

echo "Found the following libs:"
for jar in $LIBS; do
    echo " -| $jar"
done
echo

LIBS=".:./bin/:$(echo $LIBS | sed "s@ @:@g"):"

echo "Starting jdb ..."
jdb -classpath "$LIBS" -sourcepath = ".:.src/"