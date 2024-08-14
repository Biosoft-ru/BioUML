#------------------------------------------------
# Read EDDA classification model and predict names of classes for each object when data matrix is given
#------------------------------------------------

library(mclust)

print("R : Read EDDA classification model and predict names of classes for each object when data matrix is given")
dataFrame <- data.frame(matrix(unlist(dataMatrix), nrow = length(dataMatrix), byrow = TRUE))
# print(paste("R : pathToModel = ", pathToModel))
# load(pathToModel)
print(paste("R : pathToModel = ", pathToRobject))
load(pathToRobject)
print("R : EDDA classification model is loaded")
print("R : summary on EDDA classification model"); print(summary(EDDAmodel))
prediction <- predict.MclustDA(EDDAmodel, dataFrame)
predictedNamesOfClassesForEachObject <- prediction$classification