#------------------------------------------------
# Discrimination between two samples 'sample1' and 'sample2' with the help of SVM C-classification
#------------------------------------------------

library(e1071)

#------------------------------------------------
# Processing samples 'sample1' and 'sample2'
#------------------------------------------------
print("R: processing dataset1 (sample1)")
dataset1 <- data.frame(matrix(unlist(sample1), nrow = length(sample1), byrow = TRUE))
dataset1["Trust"] <- TRUE
print("R: processing dataset2")
dataset2 <- data.frame(matrix(unlist(sample2), nrow = length(sample2), byrow = TRUE))
dataset2["Trust"] <- FALSE
print("R: processing training set as unit of sample1 and sample2")
trainset <- rbind(dataset1, dataset2)

#------------------------------------------------
# Choosing parameters
#------------------------------------------------
print("R: choosing parameters for gamma and cost")
tuned <- tune.svm(Trust~., data = trainset, gamma = 10^(-6:-1), cost = 10^(-1:1), type = 'C-classification')
print(summary(tuned))
gammaTuned = tuned$best.parameter["gamma"]$gamma
costTuned = tuned$best.parameter["cost"]$cost
print(gammaTuned); print(costTuned)

#------------------------------------------------
# Create SVM model
#------------------------------------------------
svmModel <- svm(Trust~., data = trainset, kernel = "radial", gamma = gammaTuned, cost = costTuned, type = 'C-classification')
#print("R : summary on SVM-model")
#summary(svmModel)
print("R : summary on SVM-model"); print(summary(svmModel))
supportVectors <- svmModel$SV
print(paste("R: dimension of support vectors = ", dim(supportVectors)))
coefficients <- svmModel$coefs
print(paste("R: dimension of coefficients = ", dim(coefficients)))
indices <- svmModel$index
print(paste("R: dimension of indices = ", dim(indices)))
intercept <- svmModel$rho
print(paste("R: intercept = ", intercept))

#------------------------------------------------
# C-classification of the input samples sample1 and sample2
#------------------------------------------------
predictionsForSample1 <- predict(svmModel, dataset1)
print("R : summary on SVM-predictions for sample1")
summary(predictionsForSample1)
print(predictionsForSample1)
predictionsForSample2 <- predict(svmModel, dataset2)
print("R : summary on SVM-predictions for sample2")
summary(predictionsForSample2)
print(predictionsForSample2)

#------------------------------------------------
# Write SVM model
#------------------------------------------------
print("R: write SVM model")
save(svmModel, file = tempFileForModel)
print("R: O.K. SVM model is written")
