package ir.vsr;

import java.io.*;
import java.util.*;
import java.lang.*;

import ir.utilities.*;
import ir.classifiers.*;

public class InvertedIndexWithQueryCount extends InvertedIndex {
  public InvertedIndexWithQueryCount(File dirFile, short docType, boolean stem, boolean feedback) {
  	super(dirFile, docType, stem, feedback);
	}

  public InvertedIndexWithQueryCount(List<Example> examples) {
		super(examples);
	}

  /**
   * Perform ranked retrieval on this input query Document vector.
   */
  public Retrieval[] retrieve(HashMapVector vector) {
    // Create a hashtable to store the retrieved documents.  Keys
    // are docRefs and values are DoubleValues which indicate the
    // partial score accumulated for this document so far.
    // As each token in the query is processed, each document
    // it indexes is added to this hashtable and its retrieval
    // score (similarity to the query) is appropriately updated.
    Map<DocumentReference, DoubleValue> retrievalHash =
        new HashMap<DocumentReference, DoubleValue>();
    // Initialize a variable to store the length of the query vector
    double queryLength = 0.0;
    // Iterate through each token in the query input Document
    for (Map.Entry<String, Weight> entry : vector.entrySet()) {
      String token = entry.getKey();
      double count = entry.getValue().getValue();
      // Determine the score added to the similarity of each document
      // indexed under this token and update the length of the
      // query vector with the square of the weight for this token.
      queryLength = queryLength + incorporateToken(token, count, retrievalHash);
    }
    // Finalize the length of the query vector by taking the square-root of the
    // final sum of squares of its token weights.
    queryLength = Math.sqrt(queryLength);
    // Make an array to store the final ranked Retrievals.
    Retrieval[] retrievals = new Retrieval[retrievalHash.size()];
    // Iterate through each of the retrieved documents stored in
    // the final retrievalHash.
    int retrievalCount = 0;
    for (Map.Entry<DocumentReference, DoubleValue> entry : retrievalHash.entrySet()) {
      DocumentReference docRef = entry.getKey();
      double score = entry.getValue().value;
      retrievals[retrievalCount] = getRetrieval(queryLength, docRef, score);
      
      // Award it by the number of query appeared
      retrievals[retrievalCount].score += numOfQueryAppeared(vector, docRef);
			retrievalCount++;
    }
    // Sort the retrievals to produce a final ranked list using the
    // Comparator for retrievals that produces a best to worst ordering.
    Arrays.sort(retrievals);
    return retrievals;
  }

  /*
   * Return the number of the query appeared
   */
  private int numOfQueryAppeared(HashMapVector vector, DocumentReference docRef) {
		// set the number of appearance of query
		int numAppeared = 0;

		// for each token in query, find out which documents it appears
		for (Map.Entry<String, Weight> entry : vector.entrySet()) {
			String token = entry.getKey();
			TokenInfo tokenInfo = tokenHash.get(token);

			if (tokenInfo == null) continue; // too bad! it does not appear in any document
			
			// check whether this token appears in current ducument
			for (TokenOccurrence occ : tokenInfo.occList) {
				if (occ.docRef == docRef) {
					// good, it appears here!
					numAppeared += 1;
					break;
				}
			}
		}

		return numAppeared;
  }

  /*
   * basically same as the main method of InvertedIndex
   */
  public static void main(String[] args) {
    // Parse the arguments into a directory name and optional flag
    String dirName = args[args.length - 1];
    short docType = DocumentIterator.TYPE_TEXT;
    boolean stem = false, feedback = false;
    for (int i = 0; i < args.length - 1; i++) {
      String flag = args[i];
      if (flag.equals("-html"))
        // Create HTMLFileDocuments to filter HTML tags
        docType = DocumentIterator.TYPE_HTML;
      else if (flag.equals("-stem"))
        // Stem tokens with Porter stemmer
        stem = true;
      else if (flag.equals("-feedback"))
        // Use relevance feedback
        feedback = true;
      else {
        throw new IllegalArgumentException("Unknown flag: "+ flag);
      }
    }

    // Create an inverted index for the files in the given directory.
    InvertedIndexWithQueryCount index = new InvertedIndexWithQueryCount(new File(dirName), docType, stem, feedback);
    // index.print();
    // Interactively process queries to this index.
    index.processQueries();
  }
}
