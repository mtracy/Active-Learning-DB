package ML

import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.classification.{SVMModel, SVMWithSGD}
import org.apache.spark.mllib.regression.LabeledPoint

class ActiveManager() {
	
	var active: RDD[LabeledPoint] = null
	
	def analyze(predictions: RDD[LabeledPoint], threshold: Int) {				
		val newactive = predictions.filter{k=> math.abs(k.label) < threshold}
		if (active == null)
			active = newactive
		else
			active = active.union(newactive)
			
    }
    
    
}
