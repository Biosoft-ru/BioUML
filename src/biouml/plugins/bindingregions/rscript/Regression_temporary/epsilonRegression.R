library(e1071)

#------------------------------------------------
# Processing samples 'Yes' and 'No'
#------------------------------------------------

# dataMatrix = x, response = y 

#------------------------------------------------
# Choosing parameters
#------------------------------------------------
# perform a grid search
print("R: choosing parameters for epsilon and cost")
#tuned <- tune.svm(Y ~ .,  data = data, ranges = list(epsilon = seq(0,1,0.1), cost = 2^(2:9)), kernel = "radial")

# alternatively the traditional interface:
tuned <- tune.svm(x, y, ranges = list(epsilon = seq(0,1,0.1), cost = 2^(2:9)), kernel = "radial")
print(tuned)

#------------------------------------------------
# Create SVM-model
#------------------------------------------------

tunedModel <- tuned$best.model

#------------------------------------------------
# Write SVM model
#------------------------------------------------

print("write SVM model")
path1 <- file.path(pathToOutputs, "Rdata.svm")
path2 <- file.path(pathToOutputs, "Rdata.scale")
path3 <- file.path(pathToOutputs, "Rdata.yscale")
print(paste("path1 = ", path1))
print(paste("path2 = ", path2))
print(paste("path3 = ", path3))
#write.svm(tunedModel, svm.file = path1, scale.file = path2, yscale.file = path3)

#------------------------------------------------
# Prediction by regression
#------------------------------------------------
predictedY <- predict(tunedModel, newDataMatrix)