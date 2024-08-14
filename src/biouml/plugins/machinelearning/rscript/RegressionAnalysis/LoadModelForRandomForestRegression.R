#------------------------------------------------
# Read random forest regression model and predict response for given data matrix
#------------------------------------------------

library(randomForest)

print("R: Read random forest regression model and predict response for given matrix")
#dataMatrixFrame <- data.frame(matrix(unlist(matrixInput), nrow = length(matrixInput), byrow = TRUE))
dataMatrixFrame <- data.frame( read.table(matrixFilePath, header = FALSE, sep = "\t") )

#### print(paste("R : pathToModel = ", pathToModel))
#### load(pathToModel)
print(paste("R : pathToModel = ", pathToRobject))
load(pathToRobject)
print("R: random forest model is loaded")
print(rfModel)
predictedResponse <- predict(rfModel, dataMatrixFrame, type = "response", norm.votes = TRUE, predict.all = FALSE, proximity = FALSE, nodes = FALSE)
#print("R: predictedResponse = "); print(predictedResponse)
