#------------------------------------------------
# Creation of random forest regression model
#------------------------------------------------

#invisible(library(randomForest))
library(randomForest)
library(doMC) #library for parallel computing
cores <- 10 #number of threads
registerDoMC(cores)

print("R : processing dataMatrixFrame")
#dataMatrixFrame <- data.frame(matrix(unlist(dataMatrix), nrow = length(dataMatrix), byrow = TRUE))
#dataMatrixFrame <- data.frame(matrix(unlist(matrixInput), nrow = length(matrixInput), byrow = TRUE))
dataMatrixFrame <- data.frame( read.table(matrixFilePath, header = FALSE, sep = "\t") )

#------------------------------------------------
# Create random forest regression model
#------------------------------------------------
print("R : create random forest model")

# 500 trees / 10 cores = 50
rfModel <- foreach(ntree=rep(50, cores), .combine=randomForest::combine,
              .multicombine=TRUE, .packages='randomForest') %dopar% {
    randomForest(dataMatrixFrame, response, importance, ntree=ntree)
}

# old one-thread way
#rfModel <- randomForest(dataMatrixFrame, response, importance = TRUE)

predictedResponse <- predict(rfModel, dataMatrixFrame, type = "response", norm.votes = TRUE, predict.all = FALSE, proximity = FALSE, nodes = FALSE)

print("R : randomForest model is created")
print("R : summary on randomForest model")
print(rfModel)
#predictedResponse <- rfModel$predicted
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
