#!/bin/bash -e

# This script depends on having a number of other scripts that are not here,
# all of which can be found at https://github.com/ConstantineLignos/LingTools
# I just symlink these into this directory.
# 1. syllabify.py (originally from https://github.com/kylebgorman/syllabify)
# 2. clean_childes.py 
# 3. The lexinfo package
# Also, you need the following data:
# 1. A copy of CMUDict, preferably modified to remove stress from functional 
#    elements
# This will automatically download the Brown data from CHILDES.

DATA=../data

# Download and unzip the Brown data if needed
if [ ! -d $DATA/Brown ]; then
    wget http://childes.psy.cmu.edu/data/Eng-NA-MOR/Brown.zip -O $DATA/Brown.zip
    unzip -o $DATA/Brown.zip -d $DATA
fi

# In one transcript, Sarah is SAR instead of CHI
FILTER="^CHI,SAR"
for NAME in adam eve sarah
do
    echo "Processing ${NAME}..."
    # Because not everyone has bash 4 where ${NAME^} would work, and folks
    # on Windows are more likely to have Python than sed/awk on their path,
    # I use the oversized Python hammer to capitalize a string.
    UPPER_NAME=`echo $NAME | python -c "print raw_input().capitalize()"`
    CAT_CHA=$DATA/${NAME}.cha
    CAT_CLEAN=$DATA/${NAME}.txt
    SYLL=$DATA/${NAME}_syll.txt
    UNREAL_SYLL=$DATA/${NAME}_syll_unreal.txt
    echo "Concatenating to ${CAT_CHA}"
    cat $DATA/Brown/$UPPER_NAME/${NAME}*.cha > $CAT_CHA
    ./clean_childes.py $CAT_CHA $CAT_CLEAN $FILTER clean
    echo "Converting to phonemic form"
    ./convert_corpus_phonemic.py $DATA/cmudict.0.7a_ext_reduced eng < $CAT_CLEAN > $SYLL
    ./convert_corpus_phonemic.py $DATA/cmudict.0.7a_ext eng < $CAT_CLEAN > $UNREAL_SYLL
    echo "Number of lines:"
    wc -l < $SYLL
    echo
done
