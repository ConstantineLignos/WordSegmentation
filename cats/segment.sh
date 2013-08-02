#!/bin/sh
# Requires that you have already run the following line:
#mvn package appassembler:assemble
sh target/appassembler/bin/cats "$@"
