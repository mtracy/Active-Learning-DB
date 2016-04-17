package ML

import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.classification.{SVMModel, SVMWithSGD}
import org.apache.spark.mllib.regression.LabeledPoint

class SVM() {
	
	var model: SVMModel = null
	
	def train(training: RDD[LabeledPoint], numIterations: Int) : SVMModel ={				
		val trained = SVMWithSGD.train(training, numIterations)      
		model = trained
		model.clearThreshold()
		return model
    }
    
    def testLabeledPoints(testing: RDD[LabeledPoint]) : RDD[(Double, Double)] = {
		val thismodel = model
		val predictionAndLabels = testing.map { k => (thismodel.predict(k.features), k.label)}	
		return predictionAndLabels		
	}
    
}
