library(e1071)

#------------------------------------------------
# Read SVM model and classify new sample
#------------------------------------------------
print("R: read the saved SVM model and classify new sample")
datasetForClassification <- data.frame(matrix(unlist(sampleForClassification), nrow=length(sampleForClassification), byrow=TRUE))
print(paste("R : pathToModel = ", pathToModel))
load(pathToModel)

print("R: prediction of classes for sampleForClassification")
predictionsForSampleForClassification <- predict(svmModel, datasetForClassification)
print("R : SVM-predictions for sampleForClassification")
print(predictionsForSampleForClassification)
