package ir.classifiers;

import java.util.*;

/**
 * Wrapper class to test KNN classifier using 10-fold CV.
 */

public class TestKNN {
  public static void main(String args[]) throws Exception {
    String dirName = "/u/mooney/ir-code/corpora/yahoo-science/";
    String[] categories = {"bio", "chem", "phys"};
    System.out.println("Loading Examples from " + dirName + "...");
    List<Example> examples = new DirectoryExamplesConstructor(dirName, categories).getExamples();

    System.out.println("Initializing KNN classifier...");
		int k = 5;

    // setting debug flag gives very detailed output, suitable for debugging
    if (args.length == 2 && args[0].equals("-K"))
      k = Integer.parseInt(args[1]);

		KNN knn = new KNN(categories, k);

    // Perform 10-fold cross validation to generate learning curve
    CVLearningCurve cvCurve = new CVLearningCurve(knn, examples);
    cvCurve.run();
  }

}
