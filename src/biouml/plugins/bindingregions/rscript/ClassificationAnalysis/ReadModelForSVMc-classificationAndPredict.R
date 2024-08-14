#------------------------------------------------
# Read SVM model for C-classification and predict names of classes for each object when data matrix is given
#------------------------------------------------

library(e1071)

print("R : Read SVM model for C-classification and predict names of classes for each object when data matrix is given")
dataMatrixFrame <- data.frame(matrix(unlist(dataMatrix), nrow = length(dataMatrix), byrow = TRUE))
# print(paste("R : pathToModel = ", pathToModel))
# load(pathToModel)
print(paste("R : pathToModel = ", pathToRobject))
load(pathToRobject)
print("R : SVM model for C-classification is loaded")
print(svmModel)
print("R : summary on SVM model for C-classification"); print(summary(svmModel))
predictedNamesOfClassesForEachObject <- predict(svmModel, dataMatrixFrame)
#print("R : predictedNamesOfClassesForEachObject = "); print(predictedNamesOfClassesForEachObject)
