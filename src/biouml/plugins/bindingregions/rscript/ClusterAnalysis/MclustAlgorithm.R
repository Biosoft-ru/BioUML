#-------------------------------------------------------------
# Mclust : normal mixture model-based clustering algorithm
#		implementation
#-------------------------------------------------------------

library(mclust)

print("R : processing dataMatrixFrame")
dataMatrixFrame <- data.frame(matrix(unlist(dataMatrix), nrow = length(dataMatrix), byrow = TRUE))

#-------------------------------------------------------------
# Mclust implementation
#-------------------------------------------------------------
print("R : Mclust implementation")
clusters <- Mclust(dataMatrixFrame, G = numberOfClusters)
print("R : summary on Mclust results"); print(summary(clusters))
membershipProbabilities <- clusters$z
print("R : dim(membershipProbabilities) = "); print(dim(membershipProbabilities))
print("R : membershipProbabilities = "); print(membershipProbabilities)
