#------------------------------------------------
# Create and write EDDA classification model
#------------------------------------------------

library(mclust)

#------------------------------------------------
# Processing dataFrame
#------------------------------------------------
print("R : processing dataFrame from dataMatrix")
dataFrame <- data.frame(matrix(unlist(dataMatrix), nrow = length(dataMatrix), byrow = TRUE))
print(class(namesOfClassesForEachObject)); print("namesOfClassesForEachObject = "); print(namesOfClassesForEachObject);

#------------------------------------------------
# Create EDDA classification model #------------------------------------------------
print("R : Creation of EDDA classification model")
EDDAmodel <- MclustDA(dataFrame, namesOfClassesForEachObject, modelType = "EDDA",  modelNames = modelType)
print("R : summary on EDDA-model "); summary(EDDAmodel )

#------------------------------------------------
# predict indices of classes for input data matrix
#------------------------------------------------
print("R : predict indices of classes for input data matrix");
prediction <- predict.MclustDA(EDDAmodel, dataFrame)
predictedNamesOfClassesForEachObject <- prediction$classification
print("R : summary on predictions"); summary(predictedNamesOfClassesForEachObject)
cl <- class(predictedNamesOfClassesForEachObject); print("R : class = "); print(cl)
print("R : predictedNamesOfClassesForEachObject"); print(predictedNamesOfClassesForEachObject)

#------------------------------------------------
# Write EDDA model
#------------------------------------------------
print("R: write EDDA model")
save(EDDAmodel, file = tempFileForRobject)
print("R: O.K. EDDA model is written")
