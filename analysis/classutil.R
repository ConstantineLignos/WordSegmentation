# accuracy computes accuracy of classification
accuracy <- function(gold, predicted) {
  return(length(gold[gold == predicted]) / length(gold))
}
