library(boot)
library(caTools)
library(pROC)
library(plyr)
library(ggplot2)
library(scales)

source('classutil.R')

scale_x_log2 <- scale_x_continuous(trans = log2_trans(), breaks = trans_breaks('log2', function(x) 2^x), labels = trans_format('log2', math_format(2^.x)))

# Load data
diphones <- read.csv('../data/br_diphones.csv')

# Plot the distributions of each class
# First, get an average boundary value for each diphone
diphones$Boundary.Numeric <- as.numeric(diphones$Boundary)

ggplot(diphones, aes(Boundary, Prob)) + geom_boxplot(outlier.size = 0) + theme_bw()
ggplot(diphones, aes(Boundary, Prob)) + geom_violin(fill = "grey90") + theme_bw() + coord_flip()
# TODO: Overplot a few diphones of interest
ggplot(diphones, aes(Prob)) + geom_density(aes(fill = Boundary), alpha = 0.7) + scale_fill_grey() + scale_x_log2 + theme_bw()

# First, check out the test set so we can come up with a baseline
summary(diphones)
# Since it's heavily biased toward boundary=FALSE, try the simplest baseline
baseline.preds <- rep(FALSE, nrow(diphones))
# Accuracy: 0.7255
accuracy(diphones$Boundary, baseline.preds)

# Start with the simplest model, a decision stump based on TP
# Separate features from labels
trainfeatures <- subset(diphones, select = Prob)
trainlabels <- diphones$Boundary

# We're going to allow it only one training iteration. This makes it a simple stump.
stump.model <- LogitBoost(trainfeatures, trainlabels, nIter=1)
# Check out the predictions
stump.preds <- predict(stump.model, trainfeatures)
table(stump.preds, trainlabels) # Confusion matrix
# Training accuracy: 0.7513
accuracy(trainlabels, stump.preds)

# ROC analysis
stump.roc <- roc(trainlabels, trainfeatures$Prob, plot = TRUE)

# Logistic regression
m1 <- glm(Boundary ~ Prob, diphones, family = "binomial")
drop1(m1, test = "Chisq")

### Diphone distributions
# Counts
diphone.counts <- count(diphones, "Diphone")
diphone.counts$Rank <- rank(-diphone.counts$freq)
# Proportion not a word boundary
diphone.boundaryprobs <- ddply(diphones, .(Diphone), summarise, mean.boundary = mean(Boundary))
# This switches from p(is boundary) to p(is not word boundary), as the latter
# should correlate with frequency
#diphone.boundaryprobs$mean.boundary <- 1 - diphone.boundaryprobs$mean.boundary
# Merge it all together
diphone.all <- merge(diphone.counts, diphone.boundaryprobs, by = "Diphone")
# Plots
ggplot(diphone.all, aes(log(freq), mean.boundary, label = Diphone)) + stat_smooth()  + geom_point() + theme_bw()
