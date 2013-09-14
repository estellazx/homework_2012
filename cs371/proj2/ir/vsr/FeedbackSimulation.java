package ir.vsr;

import java.io.*;
import java.util.*;
import java.lang.*;

import ir.utilities.*;

/**
 * Instead of getting feed back from user's input, this class would look up the
 * correct retrieval record to simulate feedback.
 *
 * @author Shun Zhang
 */
public class FeedbackSimulation extends Feedback {
  public FeedbackSimulation(HashMapVector queryVector, Retrieval[] retrievals, InvertedIndex invertedIndex, ArrayList<DocumentReference> goodDocRefs) {
    super(queryVector, retrievals, invertedIndex);
		
    // We have good doc refs directly.
    this.goodDocRefs = goodDocRefs;
		
		// Construct badDocRefs by checking what are in good doc refs.
		for (Retrieval retrieval : retrievals) {
			DocumentReference docRef = retrieval.docRef;
			if (!goodDocRefs.contains(docRef)) {
				badDocRefs.add(docRef);
			}
		}
  }
}
