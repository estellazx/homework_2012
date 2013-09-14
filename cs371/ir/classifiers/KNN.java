package ir.classifiers;

import java.util.*;

import ir.vsr.*;

/**
 * Implements KNN Classifier.
 */

public class KNN extends Classifier {
	private InvertedIndex invertedIndex;
	private Map<String, Integer> examplesCat;
	private int K; // K for K-neighbor

	public KNN(String[] categories, int k) {
		this.categories = categories;
		this.K = k;
	}

	@Override
  public String getName() {
		return "KNN-" + this.K;
	}

	@Override
  public void train(List<Example> trainingExamples) {
		// construct InvertedIndex using its list constructor
		invertedIndex = new InvertedIndex(trainingExamples);

		// build up a hash maping for test to look up
		examplesCat = new HashMap<String, Integer>();
		for (Example example : trainingExamples) {
			examplesCat.put(example.getName(), example.getCategory());
		}
	}

	@Override
  public boolean test(Example testExample) {
		// query the testExample, get the result of sorted documents
		Retrieval[] retrievals = invertedIndex.retrieve(testExample.getHashMapVector());
		int[] votes = new int[ categories.length ];

		// initialize votes
		for (int i = 0; i < categories.length; i++) {
			votes[i] = 0;
		}

		for (int i = 0; i < this.K; i++) {
			// in case K is larger than size of training set, stop here.
			if (i == retrievals.length) {
				break;
			}

			String url = retrievals[i].docRef.file.toString();
			String filename = url.substring(url.lastIndexOf('/') + 1);
			int category = examplesCat.get(filename);
			votes[category]++;
		}

		// initialize with a random class
		int maxIndex = random.nextInt(categories.length), maxVote = 0;

		for (int i = 0; i < votes.length; i++) {
			if (votes[i] > maxVote) {
				maxIndex = i;
				maxVote = votes[i];
			}
		}

		return maxIndex == testExample.getCategory();
	}
}
