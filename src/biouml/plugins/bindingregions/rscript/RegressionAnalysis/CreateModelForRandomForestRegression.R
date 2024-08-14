#------------------------------------------------
# Creation of random forest regression model
#------------------------------------------------

#invisible(library(randomForest))
library(randomForest)

print("R : processing dataMatrixFrame")
dataMatrixFrame <- data.frame(matrix(unlist(dataMatrix), nrow = length(dataMatrix), byrow = TRUE))

#------------------------------------------------
# Create random forest regression model
#------------------------------------------------
print("R : create random forest model")
rfModel <- randomForest(dataMatrixFrame, response, importance = TRUE)
print("R : randomForest model is created")
print("R : summary on randomForest model")
print(rfModel)
predictedResponse <- rfModel$predicted
print(paste("R : number of trees grown = ", rfModel$ntree))
#print("R : predictedResponse = "); print(predictedResponse);
importance <- rfModel$importance
importanceColumnNames <- colnames(importance); print("R : importanceColumnNames = "); print(importanceColumnNames)
# importanceRowNames <- rownames(importance); print("R : importanceRowNames = "); print(importanceRowNames)

#------------------------------------------------
# Write random forest regression model
#------------------------------------------------
print("R : write random forest regression model")
#### save(rfModel, file = tempFileForModel)
save(rfModel, file = tempFileForRobject)
print("R : random forest regression model is written")
