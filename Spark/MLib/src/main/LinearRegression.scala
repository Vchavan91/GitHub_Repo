/**
 * Date :-  02/08/16.
 * Author :- Saddam
 */

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.examples.mllib.AbstractParams
import org.apache.spark.ml.{Pipeline, PipelineStage}
import org.apache.spark.ml.regression.{LinearRegression, LinearRegressionModel}
import org.apache.spark.sql.DataFrame
import scala.collection.mutable
import scala.language.reflectiveCalls
import scopt.OptionParser

case class Params(input: String = null, testInput: String = "", dataFormat: String = "text", regParam: Double = 0.0,  elasticNetParam: Double = 0.0,  maxIter: Int = 100, tol: Double = 1E-6, fracTest: Double = 0.2) extends AbstractParams[Params]

object linearRegression {
  def main(args: Array[String]) {
    val defaultParams = Params()

    // Creating the Parser
    val parser = new OptionParser[Params]("LinearRegressionExample") {
      head("LinearRegressionExample: an example Linear Regression with Elastic-Net app.")
      opt[Double]("regParam").text(s"regularization parameter, default: ${defaultParams.regParam}").action((x, c) => c.copy(regParam = x))
      opt[Double]("elasticNetParam").text(s"ElasticNet mixing parameter. For alpha = 0, the penalty is an L2 penalty. " +
        s"For alpha = 1, it is an L1 penalty. For 0 < alpha < 1, the penalty is a combination of " + s"L1 and L2, default: ${defaultParams.elasticNetParam}")
        .action((x, c) => c.copy(elasticNetParam = x))
      opt[Int]("maxIter").text(s"maximum number of iterations, default: ${defaultParams.maxIter}")
        .action((x, c) => c.copy(maxIter = x))
      opt[Double]("tol").text(s"the convergence tolerance of iterations, Smaller value will lead " +  s"to higher accuracy with the cost of more iterations, default: ${defaultParams.tol}")
        .action((x, c) => c.copy(tol = x))
      opt[Double]("fracTest").text(s"fraction of data to hold out for testing.  If given option testInput, " +  s"this option is ignored. default: ${defaultParams.fracTest}")
        .action((x, c) => c.copy(fracTest = x))
      opt[String]("testInput").text(s"input path to test dataset.  If given, option fracTest is ignored." +   s" default: ${defaultParams.testInput}")
        .action((x, c) => c.copy(testInput = x))
      opt[String]("dataFormat").text("data format: libsvm (default), dense (deprecated in Spark v1.1)")
        .action((x, c) => c.copy(dataFormat = x))
      arg[String]("<input>").text("input path to labeled examples").required()
        .action((x, c) => c.copy(input = x))
        
      // Verifying the config parameter
      checkConfig { params =>
        if (params.fracTest < 0 || params.fracTest >= 1) {
          failure(s"fracTest ${params.fracTest} value incorrect; should be in [0,1).")
        } else {
          success
        }
      }
    }

    parser.parse(args, defaultParams).map { params => run(params)}.getOrElse {sys.exit(1)}
  }

  def run(params: Params) {
    val conf = new SparkConf().setAppName(s"LinearRegressionExample with $params")
    val sc = new SparkContext(conf)
    
    println("Start...")

    println(s"LinearRegressionExample with parameters:\n$params")

    // Load training and test data and cache it.
    val (training: DataFrame, test: DataFrame) = DecisionTreeExample.loadDatasets(sc, params.input,
      params.dataFormat, params.testInput, "regression", params.fracTest)
    
    // Creating the LinearRegression model
    val lir = new LinearRegression().setFeaturesCol("features").setLabelCol("label").setRegParam(params.regParam).setElasticNetParam(params.elasticNetParam).setMaxIter(params.maxIter).setTol(params.tol)

    // Trainning the Model
    val startTime = System.nanoTime()
    val lirModel = lir.fit(training)
    val elapsedTime = (System.nanoTime() - startTime) / 1e9
    println(s"Training time: $elapsedTime seconds")

    // Printing the weights and intercept for linear regression.
    println(s"Weights: ${lirModel.coefficients} Intercept: ${lirModel.intercept}")

    println("Training data results:")
    DecisionTreeExample.evaluateRegressionModel(lirModel, training, "label")
    println("Test data results:")
    DecisionTreeExample.evaluateRegressionModel(lirModel, test, "label")
    
    println("End...")

    sc.stop()
  }
}
