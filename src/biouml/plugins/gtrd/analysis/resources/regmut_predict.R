#variables available from scope
#modelFile, featuresFile, outFile
library(randomForest)
load(modelFile)
x <- read.table(featuresFile)
pred <- predict(model, x, type="prob")
#prob.n <- pred[,'yes']
write.table(pred[,'yes',drop=F], file=outFile, col.names=F, quote=F, sep='\t')
