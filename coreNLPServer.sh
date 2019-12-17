#!/bin/bash

pushd libs/jars

java -Xmx10g -cp "*" edu.stanford.nlp.pipeline.StanfordCoreNLPServer -port 9000 -timeout 15000

popd
