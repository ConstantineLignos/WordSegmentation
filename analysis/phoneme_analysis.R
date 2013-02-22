library(ggplot2)



# Load data
phonemes <- read.csv('../data/br_phonemes.csv')

zipf_fit <- function(x) (.2/x)
# Overall distribution
ggplot(phonemes, aes(Rank, Prob, label = Phoneme)) + geom_text(aes(size = Prob)) + scale_size(to = c(4,9), legend = FALSE) + theme_bw()
# Log/log plot
ggplot(phonemes, aes(log(Rank), log(Prob))) + geom_point() + theme_bw()