library(ggplot2)

# Set paths
corpus <- "eng"
inputbase.seg <- paste("out/", corpus, "seg", sep="")
inputbase.gold <- paste("out/", corpus, "gold", sep="")

# Load data
lex.gold <- read.csv(paste(inputbase.gold, "_lexgrowth.csv", sep=""))
lex.seg <- read.csv(paste(inputbase.seg, "_lexgrowth.csv", sep=""))
pdf(paste(corpus, "_plots.pdf", sep=""))

# Mark each
segfactor <- factor(c("gold", "seg"))
lex.gold$source <-segfactor[1]
lex.seg$source <-segfactor[2]
# Merge
lex <- rbind(lex.gold, lex.seg)

# Lexicon growth
lexgrowth <- ggplot(lex, aes(utt, n.types)) + geom_line() + facet_wrap(~ source)
print(lexgrowth)

# New words per utterance, points
newwords.points <- ggplot(lex, aes(utt, n.newwords)) + geom_jitter(aes(colour=n.newwords)) + facet_wrap(~ source)
print(newwords.points)

# New words per utterance, smoothed line
newwords.line <- ggplot(lex, aes(utt, n.newwords)) + stat_smooth() + facet_wrap(~ source)
print(newwords.line)

# Turnover per utterance, points
turnover.points <- ggplot(lex, aes(utt, n.buffturnover)) + geom_jitter(aes(colour=n.buffturnover)) + facet_wrap(~ source)
print(turnover.points)

# New words per utterance, smoothed line
turnover.line <- ggplot(lex, aes(utt, n.buffturnover)) + stat_smooth() + facet_wrap(~ source)
print(turnover.line)

# Load and merge the lexicons
words.gold <- read.csv(paste(inputbase.gold, "_lex.csv", sep=""))
words.seg <- read.csv(paste(inputbase.seg, "_lex.csv", sep=""))
words.gold$source <- segfactor[1]
words.seg$source <- segfactor[2]
# Merge
words <- rbind(words.gold, words.seg)

wordfreq <- ggplot(words, aes(log(rank), log(n.tokens))) + stat_smooth() + geom_line() + facet_wrap(~ source)
print(wordfreq)
dev.off()
