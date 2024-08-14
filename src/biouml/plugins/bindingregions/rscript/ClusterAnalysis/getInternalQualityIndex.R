#--------------------------------------------------------------
# get internal quality index of hard clusters
#--------------------------------------------------------------

library(clusterCrit)

print("R : processing dataMatrixFrame")
dataMatrixFrame <- data.frame(matrix(unlist(dataMatrix), nrow = length(dataMatrix), byrow = TRUE))
dataMatrix <- as.matrix(dataMatrixFrame)

#--------------------------------------------------------------
# Calculation of internal quality index
#--------------------------------------------------------------
print("R : Calculation of internal quality indices")
print("R : indicesOfClusters = "); print(indicesOfClusters);
print("R : length(indicesOfClusters) = "); print(length(indicesOfClusters));
qualityInds <- intCriteria(dataMatrix, indicesOfClusters, qualityIndexNamesInClusterCrit)
print("R : qualityInds ="); print(qualityInds)
length <- length(qualityInds)
qualityIndices <- as.numeric(qualityInds)
print("R : qualityIndexNamesInClusterCrit ="); print(qualityIndexNamesInClusterCrit)
print("R : qualityIndices ="); print(qualityIndices)



