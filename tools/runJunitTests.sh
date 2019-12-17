#!/bin/bash

## look for libs
echo "Looking for .jar files..."

LIBS="$(find -name "*.jar")"

# echo "Found the following libs:"
# for jar in $LIBS; do
#     echo " -| $jar"
# done

LIBS=".:out/:$(echo $LIBS | sed "s@ @:@g"):"

## look for test files
echo
echo "Looking for test files..."

TESTS="$(find ./test/ -name "*Test.java")"

echo "Found the following tests:"
for test in $TESTS; do
    echo " -| $test"
done
echo

ARGS=""
for test in $TESTS; do
    # grep out the package names
    PKG=$(grep -E "package .*;" $test | perl -pe "s@package ([^;]+);@\1@g")
    # get the last part of the file path, but not the .java part
    FILE=$(echo $test | perl -pe "s@([^/]*/)*([^/]+)\.java\b@\2@g")
    ARGS="$ARGS $PKG.$FILE"

done
# TESTS=$(echo " $TESTS" | sed "s@\.java\b@@g; s@/@.@g; s@\s\.*@@g")
echo $ARGS

# do the tests
java -mx7g -cp "$LIBS" -ea org.junit.runner.JUnitCore $ARGS
