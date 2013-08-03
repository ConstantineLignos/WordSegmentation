#!/bin/bash -e
DATA=../data
# Concatenate them all together
cat $DATA/{adam,sarah,eve}_syll.txt > $DATA/all_syll.txt
# Split into training and testing
head -n 60000 $DATA/all_syll.txt > $DATA/all_syll_train.txt
tail -n +60001 $DATA/all_syll.txt > $DATA/all_syll_test.txt
