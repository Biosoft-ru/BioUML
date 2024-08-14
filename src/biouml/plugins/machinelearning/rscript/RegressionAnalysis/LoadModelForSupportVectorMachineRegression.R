#------------------------------------------------
# Read SVM-epsilon regression model and predict response for given data matrix
#------------------------------------------------

library(e1071)

print("R: Read SVM-epsilon regression model and predict response for given data matrix")
#dataMatrixFrame <- data.frame(matrix(unlist(matrixInput), nrow = length(matrixInput), byrow = TRUE))
dataMatrixFrame <- data.frame( read.table(matrixFilePath, header = FALSE, sep = "\t") )

#### print(paste("R : pathToModel = ", pathToModel))
#### load(pathToModel)
print(paste("R : pathToModel = ", pathToRobject))
load(pathToRobject)
print("R: SVM-epsilon regression model is loaded")
print(svmEpsilonModel)
print("R : summary on SVM-epsilon regression model"); print(summary(svmEpsilonModel))
predictedResponse <- predict(svmEpsilonModel, dataMatrixFrame)
#print("R: predictedResponse = "); print(predictedResponse)
