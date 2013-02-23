#!/bin/sh
# Extract data for common analyses of the Bernstein-Ratner data
python seg_features.py diphones data/brent_ratner/br-phono.txt data/br_diphones.csv
python seg_features.py phonemes data/brent_ratner/br-phono.txt data/br_phonemes.csv
python seg_features.py dibs data/brent_ratner/br-phono.txt data/br_dibs.csv
