#!/bin/bash -e
DATA=../data
for TYPE in "" _unreal
do
    # Concatenate them all together
    cat $DATA/{adam,sarah,eve}_syll${TYPE}.txt > $DATA/all_syll${TYPE}.txt
    # Split into training and testing
    head -n 60000 $DATA/all_syll.txt > $DATA/all_syll${TYPE}_train.txt
    tail -n +60001 $DATA/all_syll.txt > $DATA/all_syll${TYPE}_test.txt
done