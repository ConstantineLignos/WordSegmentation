#!/bin/bash

# This script depends on having a number of other scripts that are not here,
# all of which can be found at https://github.com/ConstantineLignos/LingTools
# 1. syllabify.py (originally from https://github.com/kylebgorman/syllabify)
# 2. clean_childes.py 
# 3. The lexinfo package
# Also, you need the following data:
# 1. CHILDES data in CHA text (not XML) format
# 2. A copy of CMUDict, preferably modified to remove stress from functional 
#    elements
# 

# In one transcript, Sarah is SAR instead of CHI
FILTER="^CHI,SAR"
ROOT=../data
for NAME in adam eve sarah
do
    echo "Processing ${NAME}..."
    CAT_CHA=$ROOT/${NAME}.cha
    CAT_CLEAN=$ROOT/${NAME}.txt
    SYLL=$ROOT/${NAME}_syll.txt
    echo "Concatenating to ${CAT_CHA}"
    cat childes_data/Brown/"${NAME^}"/${NAME}*.cha > $CAT_CHA
    ./clean_childes.py $CAT_CHA $CAT_CLEAN $FILTER clean
    echo "Converting to phonemic form"
    ./convert_corpus_phonemic.py $ROOT/cmudict.0.7a_ext_reduced eng < $CAT_CLEAN > $SYLL 2> /dev/null
    echo "Number of lines:"
    wc -l < $SYLL
    echo "Unique words:"
    grep -o -E '\S+' $SYLL | sort -u -f | wc -l
    echo
done
