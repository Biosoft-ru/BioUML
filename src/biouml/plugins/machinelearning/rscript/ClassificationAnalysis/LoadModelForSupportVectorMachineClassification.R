#------------------------------------------------
# Read SVM model for C-classification and predict names of classes for each object when data matrix is given
#------------------------------------------------

library(e1071)

print("R : Read SVM model for C-classification and predict names of classes for each object when data matrix is given")
dataMatrixFrame <- data.frame(matrix(unlist(matrixInput), nrow = length(matrixInput), byrow = TRUE))
# print(paste("R : pathToModel = ", pathToModel))
# load(pathToModel)
print(paste("R : pathToModel = ", pathToRobject))
load(pathToRobject)
print("R : SVM model for C-classification is loaded")
print(svmModel)
print("R : summary on SVM model for C-classification"); print(summary(svmModel))
predictedResponse <- predict(svmModel, dataMatrixFrame)
#print("R : predictedResponse = "); print(predictedResponse)
