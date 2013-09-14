package ir.classifiers;

import java.util.*;

/**
 * Wrapper class to test Rocchio classifier using 10-fold CV.
 */

public class TestRocchio2 {
   public static void main(String args[]) throws Exception {
    String dirName = "/u/mooney/ir-code/corpora/yahoo-top/";
    String[] categories = {"comp", "gov", "rec"};
    System.out.println("Loading Examples from " + dirName + "...");
    List<Example> examples = new DirectoryExamplesConstructor(dirName, categories).getExamples();

    System.out.println("Initializing Rocchio classifier...");
		boolean neg = false;

    // setting debug flag gives very detailed output, suitable for debugging
    if (args.length == 1 && args[0].equals("-neg"))
			neg = true;

		Rocchio roc = new Rocchio(categories, neg);

    // Perform 10-fold cross validation to generate learning curve
    CVLearningCurve cvCurve = new CVLearningCurve(roc, examples);
    cvCurve.run();
  }
}
