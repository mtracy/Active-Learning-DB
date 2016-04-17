package ML

import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.classification.{LogisticRegressionWithLBFGS, LogisticRegressionModel}
import org.apache.spark.mllib.regression.LabeledPoint


class LogReg() {
	
	var model: LogisticRegressionModel = null
	
	def train(training: RDD[LabeledPoint], numclasses: Int) : LogisticRegressionModel ={				
		val fitted = new LogisticRegressionWithLBFGS()
			.setNumClasses(numclasses)
			.run(training)		
		model = fitted
		return model       
    }
    
    def testLabeledPoints(testing: RDD[LabeledPoint]) : RDD[(Double, Double)] = {
		val thismodel = model
		val predictionAndLabels = testing.map { k => (thismodel.predict(k.features), k.label)}	
		return predictionAndLabels		
	}
    
}
