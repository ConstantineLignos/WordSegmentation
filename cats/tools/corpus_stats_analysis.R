library(ggplot2)

lexgrowth <- read.csv("out/brown_lexgrowth.csv")
# Lexicon growth
print(ggplot(lexgrowth, aes(utt, n.types)) + geom_line())
# New words per utterance
print(ggplot(lexgrowth, aes(utt, n.newwords)) + stat_smooth() + coord_cartesian(ylim=c(0, 2))) #  geom_jitter(aes(colour=n.newwords)
print(ggplot(lexgrowth, aes(utt, n.newwords)) + geom_jitter(aes(colour=n.newwords)))

lex <- read.csv("out/brown_lex.csv")
print(ggplot(lex, aes(log(rank), log(n.tokens))) + geom_line())
