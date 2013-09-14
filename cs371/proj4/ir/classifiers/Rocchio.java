package ir.classifiers;

import java.util.*;

import ir.vsr.*;

/**
 * Implements Rocchio Classifier.
 */
public class Rocchio extends Classifier {
	private boolean neg; // true if vectors not in thatcategory are subtracted.
	private Map<Integer, HashMapVector> prototypes;

	public Rocchio(String[] categories, boolean neg) {
		this.categories = categories;
		this.neg = neg;
	}

	@Override
  public String getName() {
		if (neg) {
			return "Rocchio-neg";
		}
		else {
			return "Rocchio";
		}
	}

	@Override
  public void train(List<Example> trainingExamples) {
		prototypes = new HashMap<Integer, HashMapVector>();

		// initialize prototype vector for each category.
		for (int i = 0; i < categories.length; i++) {
			prototypes.put(i, new HashMapVector());
		}

		for (Example example : trainingExamples) {
			HashMapVector vector = example.getHashMapVector();
			int category = example.getCategory();
			
			// normalize it by its max weight
			prototypes.get(category).addScaled(vector, 1.0 / vector.maxWeight());

			// subtract this to other categories
			if (neg) {
				for (int i = 0; i < categories.length; i++) {
					if (i != category) {
						prototypes.get(i).addScaled(vector, - 1.0 / vector.maxWeight());
					}
				}
			}
		}
	}

	@Override
  public boolean test(Example testExample) {
		// initialize maximum cosine sim.
		double maxCos = -2;
		// category it belongs to. initialize with a random category.
		int category = random.nextInt(categories.length);
		HashMapVector vector = testExample.getHashMapVector();

		for (int i = 0; i < categories.length; i++) {
			double cos = vector.cosineTo(prototypes.get(i));
			if (cos > maxCos) {
				category = i;
				maxCos = cos;
			}
		}

		return testExample.getCategory() == category;
	}
}
