 #R script to generate data and estimate its distribution density using kernel estimation
 #Source: https://cran.r-project.org/web/packages/ks/vignettes/kde.pdf
 library(ks)
 set.seed(8192)
 samp <- 200
 mus <- rbind(c(-2,2), c(0,0), c(2,-2))
 Sigmas <- rbind(diag(2), matrix(c(0.8, -0.72, -0.72, 0.8), nrow=2), diag(2))
 cwt <- 3/11
 props <- c((1-cwt)/2, cwt, (1-cwt)/2)
 x <- rmvnorm.mixt(n=samp, mus=mus, Sigmas=Sigmas, props=props)
 points <- matrix( c( -1, -1, -1, 0, 0, 0, 1, 1, 1, -1, 0, 1, -1, 0, 1, -1, 0, 1), 9, 2 )
 density <- kde(x, Hpi(x), eval.points = points)
 print("density estimated with calculated bandwidth:")
 density["estimate"]
 
 densityNormal <- kde(x, Hns(x), eval.points = points)
 print("density estimated with normal-scaled bandwidth:")
 densityNormal["estimate"]