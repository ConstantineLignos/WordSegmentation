library(ggplot2)
library(plyr)
library(scales)
library(caTools)
library(pROC)

source('classutil.R')

scale_x_log2 <- scale_x_continuous(trans = log2_trans(), breaks = trans_breaks('log2', function(x) 2^x), labels = trans_format('log2', math_format(2^.x)))
scale_y_log2 <- scale_y_continuous(trans = log2_trans(), breaks = trans_breaks('log2', function(x) 2^x), labels = trans_format('log2', math_format(2^.x)))

dibs <- read.csv('../data/br_dibs.csv')
dibs$boundary.factor <- factor(dibs$boundary, levels = c(TRUE, FALSE), labels = c("Word boundary", "Word Internal"))
summary(dibs)

# Parameter space explored by the DiBS paper, in score form
pdf(file = "dibs_stump_boundary.pdf", height = 4)
ggplot(dibs, aes(score + 0.01)) + geom_density(aes(fill = boundary.factor), alpha = 0.75, position = "fill") + scale_fill_grey(name = "Boundary Type") + scale_x_log2 + xlab("DiBS Score") + ylab("Probability") +  geom_vline(xintercept = 1 / .16, linetype = "longdash", alpha = 0.7) + geom_vline(xintercept = 1 / .40, linetype = "longdash", alpha = 0.7)+ theme_bw() + theme(legend.position = "bottom")
dev.off()

pdf(file = "true_boundary_density.pdf")
ggplot(dibs, aes(prob.true + 0.01)) + geom_density(aes(fill = boundary.factor), alpha = 0.75, position = "fill") + scale_fill_grey(name = "Boundary Type") + scale_x_log2 + xlab("True P(#|xy)") + ylab("Probability") + theme_bw() + theme(legend.position = "bottom")
dev.off()

pdf(file = "dibs_boundary_density.pdf")
ggplot(dibs, aes(prob.dibs + 0.01)) + geom_density(aes(fill = boundary.factor), alpha = 0.75, position = "fill") + scale_fill_grey(name = "Boundary Type") + scale_x_log2 + xlab("DiBS Estimated P(#|xy)") + ylab("Probability") + theme_bw() + theme(legend.position = "bottom")
dev.off()

pdf(file = "dibs_estimate_correlation.pdf")
ggplot(dibs, aes(prob.true + 0.01, prob.dibs + 0.01)) + scale_x_log2 + scale_y_log2 + geom_point() + stat_smooth(method = 'lm') + xlab("True P(#|xy)") + ylab("DiBS Estimated P(#|xy)") + theme_bw()
dev.off()
cor(dibs$prob.dibs, dibs$prob.true, method = "spearman")

pdf(file = "proposed_estimate_correlation.pdf")
ggplot(dibs, aes(prob.est2 + 0.01, prob.dibs + 0.01)) + scale_x_log2 + scale_y_log2 + geom_point() + xlab("DiBS Estimated P(#|xy)") + ylab("Proposed Estimated P(#|xy)") + theme_bw()
dev.off()
cor(dibs$prob.est2, dibs$prob.true, method = "spearman")
cor(dibs$prob.est2, dibs$prob.dibs, method = "spearman")

## Accuracy estimates, etc.
# Baseline: #0.7255
accuracy(dibs$boundary, rep(FALSE, length(dibs$boundary)))

# True P(#|xy)
m.true <- LogitBoost(subset(dibs, select = prob.true), dibs$boundary, nIter=1)
m.true.preds <- predict(m.true, subset(dibs, select = prob.true))
# Training accuracy: 0.9417
accuracy(dibs$boundary, m.true.preds)

# Classified by DiBS estimate of P(#|xy)
m.dibs <- LogitBoost(subset(dibs, select = prob.dibs), dibs$boundary, nIter=1)
m.dibs.preds <- predict(m.dibs, subset(dibs, select = prob.dibs))
# Training accuracy: 0.8587
accuracy(dibs$boundary, m.dibs.preds)

# Classified by DiBS score. Will be same as above.
m.score <- LogitBoost(subset(dibs, select = score), dibs$boundary, nIter=1)
m.score.preds <- predict(m.score, subset(dibs, select = score))
# Training accuracy: 0.8587
accuracy(dibs$boundary, m.score.preds)

# Classified by another estimate of P(#|xy)
m.est1 <- LogitBoost(subset(dibs, select = prob.est1), dibs$boundary, nIter=1)
m.est1.preds <- predict(m.est1, subset(dibs, select = prob.est1))
# Training accuracy: 0.8494
accuracy(dibs$boundary, m.est1.preds)

# Classified by another estimate of P(#|xy)
m.est2 <- LogitBoost(subset(dibs, select = prob.est2), dibs$boundary, nIter=1)
m.est2.preds <- predict(m.est2, subset(dibs, select = prob.est2))
# Training accuracy: 0.8587
accuracy(dibs$boundary, m.est2.preds)


## ROC
roc.true <- roc(dibs$boundary, dibs$prob.true, plot = TRUE)
roc.dibs <- roc(dibs$boundary, dibs$prob.dibs, plot = TRUE)