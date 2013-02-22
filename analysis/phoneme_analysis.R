library(ggplot2)

# Load data
phonemes <- read.csv('../data/br_phonemes.csv')

zipf_fit <- function(x) (.2/x)
# Overall distribution
ggplot(phonemes, aes(Rank, Prob, label = Phoneme)) + geom_text(aes(size = Prob)) + scale_size(range = c(3, 8), guide = "none") + theme_bw()
# Log/log plot
pdf(file="phoneme_zipf.pdf")
ggplot(phonemes, aes(log(Rank), log(Prob))) + geom_point() + xlab("Log Phoneme Rank") + ylab("Log Phoneme Frequency") + theme_bw()
dev.off()
