#------------------------------------------------
# Creation SVM-epsilon regression model
#------------------------------------------------

library(e1071)

print("R: processing dataMatrixFrame")
#dataMatrixFrame <- data.frame(matrix(unlist(matrixInput), nrow = length(matrixInput), byrow = TRUE))
dataMatrixFrame <- data.frame( read.table(matrixFilePath, header = FALSE, sep = "\t") )

#------------------------------------------------
# Create SVM-epsilon regression model 'svmEpsilonModel'
#------------------------------------------------
print("R : create SVM-epsilon regression model")
# don't work; ERROR
#tuned <- tune.svm(dataMatrixFrame, response, ranges = list(epsilon = seq(0,1,0.1), cost = 2^(2:9)), kernel = "radial")
#print("R : tuned = "); print(tuned)
#svmEpsilonModel <- tuned$best.model
svmEpsilonModel <- best.svm(dataMatrixFrame, response, tunecontrol = tune.control(cross = 5), cost = c(1:10), epsilon = c(0.05, 0.10))
print("R : svmEpsilonModel = "); print(svmEpsilonModel)
print("R : summary on SVM-epsilon regression model"); print(summary(svmEpsilonModel))

#------------------------------------------------
# Write SVM-epsilon regression model
#------------------------------------------------
print("R : write SVM-epsilon regression model")
##### save(svmEpsilonModel, file = tempFileForModel)
save(svmEpsilonModel, file = tempFileForRobject)
print("R : SVM-epsilon regression model is written")

#------------------------------------------------
# Prediction by regression
#------------------------------------------------
predictedResponse <- predict(svmEpsilonModel, dataMatrixFrame)
#print("R : predictedResponse = "); print(predictedResponse);
