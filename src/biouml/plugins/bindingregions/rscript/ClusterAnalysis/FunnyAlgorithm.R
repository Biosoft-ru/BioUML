#--------------------------------------------------------------
# FUNNY : fuzzy k-means clusterization algorithm implementation
#--------------------------------------------------------------

library(cluster)

print("R : processing dataMatrixFrame")
dataMatrixFrame <- data.frame(matrix(unlist(dataMatrix), nrow = length(dataMatrix), byrow = TRUE))
print("R : processing initialMembershipProbabilities")
initialMembershipProbabilities <- data.frame(matrix(unlist(initialMembershipProbabilities), nrow = length(initialMembershipProbabilities), byrow = TRUE))
initialMembershipProbabilities <- as.matrix(initialMembershipProbabilities)
print("R : initialMembershipProbabilities = "); print(initialMembershipProbabilities)
print("R : class of initialMembershipProbabilities = "); print(class(initialMembershipProbabilities))

#--------------------------------------------------------------
# FUNNY implementation
#--------------------------------------------------------------
print("R : FUNNY implementation")
clusters <- fanny(x = dataMatrixFrame, k = numberOfClusters, diss = FALSE, metric = distanceMeasure, iniMem.p = initialMembershipProbabilities, keep.diss = FALSE, keep.data = TRUE, maxit = 3000)
print("R : summary on FUNNY results"); print(summary(clusters))
membershipProbabilities <- clusters$membership
print("R : dim(membershipProbabilities) = "); print(dim(membershipProbabilities))
print("R : membershipProbabilities = "); print(membershipProbabilities)
