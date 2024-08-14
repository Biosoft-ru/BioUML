#------------------------------------------------
# Create and write C-classification model
#------------------------------------------------

library(e1071)

#------------------------------------------------
# Processing dataFrame
#------------------------------------------------
print("R : processing dataFrame from dataMatrix")
dataFrame <- data.frame(matrix(unlist(dataMatrix), nrow = length(dataMatrix), byrow = TRUE))
print(class(namesOfClassesForEachObject)); print("namesOfClassesForEachObject = "); print(namesOfClassesForEachObject);
dataFrame <- cbind(dataFrame, Trust = namesOfClassesForEachObject);
print(dataFrame)


#------------------------------------------------
# Choosing parameters
#------------------------------------------------
print("R : choosing parameters for gamma and cost")
tuned <- tune.svm(Trust~., data = dataFrame, gamma = 10^(-6:-1), cost = 10^(-1:1), type = 'C-classification')
summary(tuned)
gammaTuned <- tuned$best.parameter["gamma"]$gamma; print("R : gammaTuned = "); print(gammaTuned)
costTuned <- tuned$best.parameter["cost"]$cost; print("R : costTuned = "); print(costTuned)

#------------------------------------------------
# Create SVM model for C-classification
#------------------------------------------------
svmModel <- svm(Trust~., data = dataFrame, kernel = "radial", gamma = gammaTuned, cost = costTuned, type = 'C-classification')
print("R : summary on SVM-model"); summary(svmModel)
# supportVectors <- svmModel$SV
# print(paste("R: dimension of support vectors = ", dim(supportVectors)))
# coefficients <- svmModel$coefs
# print(paste("R: dimension of coefficients = ", dim(coefficients)))
# indices <- svmModel$index; print(paste("R: dimension of indices = ", dim(indices))); print(indices)
# intercept <- svmModel$rho; print(paste("R: intercept = ", intercept))

#------------------------------------------------
# predict indices of classes for input data matrix
#------------------------------------------------
predictedNamesOfClassesForEachObject <- predict(svmModel, dataFrame)
print("R : summary on predictions"); summary(predictedNamesOfClassesForEachObject)
cl <- class(predictedNamesOfClassesForEachObject); print("R : class = "); print(cl)
print("R : predictedNamesOfClassesForEachObject"); print(predictedNamesOfClassesForEachObject)

#------------------------------------------------
# Write SVM model
#------------------------------------------------
print("R: write SVM model")
save(svmModel, file = tempFileForRobject)
print("R: O.K. SVM model is written")
