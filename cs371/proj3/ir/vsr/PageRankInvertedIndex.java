package ir.vsr;

import java.io.*;
import java.util.*;
import java.lang.*;

import ir.utilities.*;
import ir.classifiers.*;

/**
 * An inverted index for vector-space information retrieval. Contains
 * methods for creating an inverted index from a set of documents
 * and retrieving ranked matches to queries using standard TF/IDF
 * weighting and cosine similarity.
 *
 * @author Ray Mooney
 */
public class PageRankInvertedIndex extends InvertedIndex {
	private double pageRankWeight;
	private Map<String, Double> rank;

	/**
	 * Call the super constructor.
	 */
  public PageRankInvertedIndex(File dirFile, short docType, boolean stem, boolean feedback, double weight) {
		super(dirFile, docType, stem, feedback);
		pageRankWeight = weight;

		// Construct the document index -> rank value mapping
		pageRankRead();
	}

	/**
	 * Add scaled page rank to the score of that document.
	 */
  protected Retrieval getRetrieval(double queryLength, DocumentReference docRef, double score) {
    // Normalize score for the lengths of the two document vectors
    score = score / (queryLength * docRef.length);
		// add the scaled page rank value
		score += pageRankWeight * rank.get(docRef.toString());
    // Add a Retrieval for this document to the result array
    return new Retrieval(docRef, score);
  }

	private void pageRankRead() {
		rank = new HashMap<String, Double>();
		String line;

		try {
			FileReader fstream = new FileReader(dirFile + "/pageRanks");
			BufferedReader in = new BufferedReader(fstream);

			while ((line = in.readLine()) != null) {
				String[] elems = line.split(" ");
				String indexName = elems[0];
				Double rankValue = Double.parseDouble(elems[1]);

				rank.put(indexName, rankValue);
			}
		}
		catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		}
	}

	public static void main(String[] args) {
    // Parse the arguments into a directory name and optional flag
    String dirName = args[args.length - 1];
    short docType = DocumentIterator.TYPE_TEXT;
		double weight = 0; // this would be assigned from command line
    boolean stem = false, feedback = false, readWeight = false;
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
			else if (flag.equals("-weight"))
				readWeight = true;
			else if (readWeight) {
				weight = Double.parseDouble(flag);
				readWeight = false;
			}
      else {
        throw new IllegalArgumentException("Unknown flag: "+ flag);
      }
    }

    // Create an inverted index for the files in the given directory.
    PageRankInvertedIndex index = new PageRankInvertedIndex(new File(dirName), docType, stem, feedback, weight);
    // index.print();
    // Interactively process queries to this index.
    index.processQueries();
  }
}
