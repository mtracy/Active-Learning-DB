# script to generate and evaluate topic models
# cmsc724 db final project

library('xlsx')
library('stm')
library('tm')
library('lda')
library('SnowballC')
library('sqldf')
library('data.table')
library('plyr')
library('ggplot2')
library('scales')
library('slam')
library('car')
library('psych')
library('RODBC')

#############
# Load data and process
# data source: http://www.cs.cornell.edu/people/pabo/movie-review-data/
#############
neg = read.delim(file="rt-polaritydata/rt-polarity.neg", header=FALSE, stringsAsFactors = FALSE)
pos = read.delim(file="rt-polaritydata/rt-polarity.pos", header=FALSE, stringsAsFactors = FALSE)
text = rbind(neg, pos)

prepareText <- function(text) {
  temp = textProcessor(text$V1)
  vocab = temp$vocab
  docs = temp$documents
  out = prepDocuments(docs, vocab)  
  docs = out$documents
  vocab = out$vocab
  return (list(docs = docs, vocab=vocab))
}

preparedText = prepareText(text)
docs = preparedText$docs
vocab = preparedText$vocab

#############
# Generate and evaluate topic models with LDA
# options: run once or loop
#############

# store results
results = matrix(0, nrow=4, ncol=3)

# parameters for lda
em = 1000
i = 1
for (k in c(50, 100, 150, 200)) {

  mod.out = stm(docs, vocab, k, max.em.its = em)

  M = 10
  excScore = exclusivity(mod.out, M)
  semScore = semanticCoherence(mod.out, docs, M)

  results[i, 1] = k
  results[i, 2] = sum(excScore)
  results[i, 3] = sum(semScore)
  
  i = i+1
}


# model outputs
theta = mod.out$theta

#############
# Evaluate topic models (coherence and exclusivity)
# Functions adopted from stm: https://cran.r-project.org/web/packages/stm/index.html
#############
exclusivity <- function(mod.out, M, frexw=.7){
  w <- frexw
  if(length(mod.out$beta$logbeta)!=1) stop("Exclusivity calculation only designed for models without content covariates")
  tbeta <- t(exp(mod.out$beta$logbeta[[1]]))
  s <- rowSums(tbeta)
  mat <- tbeta/s #normed by columns of beta now.
  
  ex <- apply(mat,2,rank)/nrow(mat)
  fr <- apply(tbeta,2,rank)/nrow(mat)
  frex<- 1/(w/ex + (1-w)/fr)
  index <- apply(tbeta, 2, order, decreasing=TRUE)[1:M,]
  out <- vector(length=ncol(tbeta)) 
  for(i in 1:ncol(frex)) {
    out[i] <- sum(frex[index[,i],i])
  }
  out
}
semanticCoherence <- function(model.out, documents, M){
  if(length(model.out$beta$logbeta)!=1) {
    result <- 0
    for(i in 1:length(model.out$beta$logbeta)){
      subset <- which(model.out$settings$covariates$betaindex==i)
      triplet <- doc.to.ijv(documents[subset])
      mat <- simple_triplet_matrix(triplet$i, triplet$j,triplet$v, ncol=model.out$settings$dim$V)
      result = result + semCoh1beta(mat, M, beta=model.out$beta$logbeta[[i]])*length(subset)
    }
    return(result/length(documents))
  }
  else {
    beta <- model.out$beta$logbeta[[1]]
    #Get the Top N Words
    top.words <- apply(beta, 1, order, decreasing=TRUE)[1:M,]
    wordlist <- unique(as.vector(top.words))
    triplet <- doc.to.ijv(documents)
    mat <- simple_triplet_matrix(triplet$i, triplet$j,triplet$v, ncol=model.out$settings$dim$V)
    result = semCoh1beta(mat, M, beta=beta)
    return(result)
  }
}
semCoh1beta <- function(mat, M, beta){
  #Get the Top N Words
  top.words <- apply(beta, 1, order, decreasing=TRUE)[1:M,]
  wordlist <- unique(as.vector(top.words))
  mat <- mat[,wordlist]
  mat$v <- ifelse(mat$v>1, 1,mat$v) #binarize
  
  #do the cross product to get co-occurences
  cross <- tcrossprod_simple_triplet_matrix(t(mat))
  
  #create a list object with the renumbered words (so now it corresponds to the rows in the table)
  temp <- match(as.vector(top.words),wordlist)
  labels <- split(temp, rep(1:nrow(beta), each=M))
  
  #Note this could be done with recursion in an elegant way, but let's just be simpler about it.
  sem <- function(ml,cross) {
    m <- ml[1]; l <- ml[2]
    log(.01 + cross[m,l]) - log(cross[l,l] + .01)
  }
  result <- vector(length=nrow(beta))
  for(k in 1:nrow(beta)) {
    grid <- expand.grid(labels[[k]],labels[[k]])
    colnames(grid) <- c("m", "l") #corresponds to original paper
    grid <- grid[grid$m > grid$l,]
    calc <- apply(grid,1,sem,cross)
    result[k] <- sum(calc)
  }
  return(result)
}
doc.to.ijv <- function(documents, fixzeroindex=TRUE) {
  #Turns our format into triplet format (can be zero indexed)
  indices <- unlist(lapply(documents, '[',1,)) #grab the first row
  if((0 %in% indices) & fixzeroindex) indices <- indices + 1 #if zero-indexed, fix it.
  counts <- lapply(documents, '[',2,)  #grab the second row but preserve the list structure for a moment
  VsubD <- unlist(lapply(counts,length)) #grab the number of unique words per document
  rowsums <- unlist(lapply(counts,sum)) #grab the number of tokens per documents
  docids <- rep(1:length(documents), times=VsubD) #add row numbers
  counts <- unlist(counts) #unlist the count structure
  #now we return using the convention for the simple triplet matrix,
  #plus the row sums which we use in DMR.
  return(list(i=as.integer(docids), j=as.integer(indices), v=as.integer(counts), rowsums=as.integer(rowsums)))
}


#############
# Write to topic cube
#############

# Write to DB
c <-odbcDriverConnect('driver={SQL Server}; server=XX.XX.XX.XX; database=azMeds;uid=***; pwd=***')
toWriteStr = "data fields"
queryStr = paste("UPDATE ", toWriteStr," FROM pseTopicCube", sep="")
r <- sqlQuery(c, queryStr)
close(c)

# Export to local
write.table(theta, file="results/theta.csv", col.names = FALSE, row.names = FALSE, sep=",")
write.table(labels, file="results/labels.csv", col.names = FALSE, row.names = FALSE, sep = ",")



##############
# END
##############
#labels = matrix(0, nrow = 4247, ncol = 1)
#labels[1:1679,] = -1
#labels[1680:4247,] = 1

#labelTopics(mod.out)

#beta = mod.out$beta
