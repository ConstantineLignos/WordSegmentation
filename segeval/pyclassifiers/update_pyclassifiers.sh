#!/bin/sh
# This is a hackish script to avoid adding a submdodule. It automatically
# updates the classifiers used.
git clone https://github.com/ConstantineLignos/pyclassifiers.git
cp pyclassifiers/binaryclassifier.py .
cp pyclassifiers/stump.py .
rm -rf pyclassifiers
