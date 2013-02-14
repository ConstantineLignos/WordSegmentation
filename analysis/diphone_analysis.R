library(boot)
library(caTools)
library(pROC)
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
