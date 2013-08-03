#!/bin/sh
# Requires that you have already run mvn package
java -Xmx2g -cp target/cats-segmenter.jar edu.upenn.ircs.lignos.cats.SegExperiment "$@"
