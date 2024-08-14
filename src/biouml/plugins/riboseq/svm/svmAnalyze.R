library( e1071 )
library( MASS )

NUMBER_TRAINING_ROWS <- 500

args <- commandArgs( TRUE )
pathToData <- args[1]

# read tables
datasetYes <- read.csv( paste( pathToData,'yesFile.interval', sep = ''),
    head=TRUE, sep="\t")
datasetNo  <- read.csv( paste( pathToData,'noFile.interval' , sep = ''),
    head=TRUE, sep="\t")
datasetUn  <- read.csv( paste( pathToData,'unFile.interval' , sep = ''),
    head=TRUE, sep="\t")

# preparing yes dataset
datasetYes <- datasetYes[
    c( 'modasHistogram.5', 'modasHistogram.4', 'modasHistogram.3',
    'modasHistogram.2', 'modasHistogram.1', 'modasHistogram0',
    'modasHistogram1', 'modasHistogram2', 'modasHistogram3',
    'modasHistogram4', 'modasHistogram5', 'lengthCluster', 'sites',
    'startCodonOffset', 'initCodonPosition', 'startModaOffset')
]
datasetYes[, 1:11] <- datasetYes[, 1:11] / datasetYes$sites
datasetYes$initCodonPosition <-
    datasetYes$initCodonPosition - datasetYes$startModaOffset
datasetYes["Trust"] <- TRUE
dataYes <- subset(datasetYes, select=-c(startModaOffset))

# sorting by decreasing number of sites
dataYes <- dataYes[order(-dataYes$sites),]
# selecting first NUMBER_TRAINING_ROWS rows (for faster computing)
if( nrow(dataYes) > NUMBER_TRAINING_ROWS ){
    dataYes <- dataYes[1:NUMBER_TRAINING_ROWS,]
}

# preparing no dataset
datasetNo <- datasetNo[
    c( 'modasHistogram.5', 'modasHistogram.4', 'modasHistogram.3',
    'modasHistogram.2', 'modasHistogram.1', 'modasHistogram0',
    'modasHistogram1', 'modasHistogram2', 'modasHistogram3',
    'modasHistogram4', 'modasHistogram5', 'lengthCluster', 'sites',
    'startCodonOffset', 'initCodonPosition', 'startModaOffset')
]
datasetNo[, 1:11] <- datasetNo[, 1:11] / datasetNo$sites
datasetNo$initCodonPosition <-
    datasetNo$initCodonPosition - datasetNo$startModaOffset
datasetNo["Trust"] <- FALSE
dataNo <- subset(datasetNo, select=-c(startModaOffset))

# sorting by decreasing number of sites
dataNo <- dataNo[order(-dataNo$sites),]
# selecting first NUMBER_TRAINING_ROWS rows (for faster computing)
if( nrow(dataNo) > NUMBER_TRAINING_ROWS ){
    dataNo <- dataNo[1:NUMBER_TRAINING_ROWS,]
}

# merge
trainset <- rbind( dataYes, dataNo )

# preparing undefined dataset
datasetUn <- datasetUn[
c( 'modasHistogram.5', 'modasHistogram.4', 'modasHistogram.3',
    'modasHistogram.2', 'modasHistogram.1', 'modasHistogram0',
    'modasHistogram1', 'modasHistogram2', 'modasHistogram3',
    'modasHistogram4', 'modasHistogram5', 'lengthCluster', 'sites',
    'startCodonOffset', 'initCodonPosition', 'startModaOffset', 'idName')
]
datasetUn[, 1:11] <- datasetUn[, 1:11] / datasetUn$sites
datasetUn$initCodonPosition <-
    datasetUn$initCodonPosition - datasetUn$startModaOffset
dataUn <- subset(datasetUn, select=-c(startModaOffset))

dataForClassification <- subset(dataUn, select=-c(idName))

#------------------------------------------------
# Choosing Parameters
tuned <- tune.svm(
    Trust~., data = trainset,
    gamma = 10^(-6:-1), cost = 10^(-1:1), type = 'C')
# print( summary(tuned) )

# get tuned gamma and cost
gammaTuned = tuned$best.parameter["gamma"]$gamma
costTuned = tuned$best.parameter["cost"]$cost

# print( gammaTuned )
# print( costTuned )

# Training The Model
model <- svm(
    Trust~., data = trainset, kernel="radial",
    gamma = gammaTuned, cost = costTuned, type = 'C')
# summary(model)

# Classification
prediction <- predict(model, dataForClassification)
# summary(prediction)
# prediction

# add column with prediction
classificationResult <- cbind(dataUn, prediction)

# write prediction to output file
fileConn <- file( paste( pathToData, 'outputAnalysis.txt', sep = '') )
write.matrix( classificationResult[, c('idName','prediction')], fileConn )
close( fileConn )