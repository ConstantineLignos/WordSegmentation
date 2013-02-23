library(ggplot2)
library(plyr)
library(boot)
library(scales)

scale_x_log2 <- scale_x_continuous(trans = log2_trans(), breaks = trans_breaks('log2', function(x) 2^x), labels = trans_format('log2', math_format(2^.x)))

dibs <- read.csv('../data/br_dibs.csv')
dibs$Boundary <- factor(dibs$Boundary, levels = c(TRUE, FALSE), labels = c("Word Boundary", "Word Internal"))

pdf(file="dibs_stump_boundary.pdf", height = 4)
ggplot(dibs, aes(Score + 0.01)) + geom_density(aes(fill = Boundary), alpha = 0.75) + scale_fill_grey() + scale_x_log2 + xlab("DiBS Score") + ylab("Density") +  geom_vline(xintercept = 1 / .16, linetype = "longdash", alpha = 0.7) + geom_vline(xintercept = 1 / .40, linetype = "longdash", alpha = 0.7)+ theme_bw() + theme(legend.position = "bottom")
dev.off()