#!/bin/sh
# Requires that you have already run mvn package
java -Xmx1g -jar target/cats-segmenter.jar "$@"
