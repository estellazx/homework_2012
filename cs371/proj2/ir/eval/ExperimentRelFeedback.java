package ir.eval;

import java.io.*;
import java.util.*;
import java.lang.*;

import ir.utilities.*;
import ir.vsr.*;

/**
 * For project 2.
 *
 * @author Shun Zhang
 */
public class ExperimentRelFeedback extends Experiment {
	/** number of documents to be skipped for testing. */
	public static int M;
	/** number of documents to simulate feedback. */
	public static int N;
	/** whether using stemming - this is not an attribute in its super class. */
	private boolean stem;

  /**
   * Create an Experiment object for generating Recall/Precision curves
   *
   * @param corpusDir The directory of files to index.
   * @param queryFile The file of query/relevant-docs pairs to evaluate.
   * @param outFile   File for output precision/recall data.
   * @param docType   The type of documents to index (See docType in DocumentIterator).
   * @param stem      Whether tokens should be stemmed with Porter stemmer.
	 * @param N					Number of documents to simulate feedback.
   */
  public ExperimentRelFeedback(File corpusDir, File queryFile, File outFile, short docType, boolean stem, int N)
      throws IOException {
		super(corpusDir, queryFile, outFile, docType, stem);
		this.N = N;
		this.stem = stem;
	}

  /**
   * Process the next query read from the query file reader and evaluate
   * results compared to known relevant docs also read from the query file.
   *
   * @return true if query successfully read, else false if no more queries
   * in query file
   */
  boolean processQuery(BufferedReader in) throws IOException {
    String query = in.readLine();   // get the query
    if (query == null) return false;  // return false if end of file
    System.out.println("\nQuery " + (rpResults.size() + 1) + ": " + query);

    // Process the query and get the ranked retrievals
    Retrieval[] retrievals = index.retrieve(query);
    System.out.println("Returned " + retrievals.length + " documents.");

    // Read the known relevant docs from query file and parse them
    // into an ArrayList of String's of relevant file names.
    String line = in.readLine();
    ArrayList<String> correctRetrievals = MoreString.segment(line, ' ');
    System.out.println(correctRetrievals.size() + " truly relevant documents.");
		
		// Get the documents that are relevant, and use the FeedbackSimulation class to get expended query.
		// Then get the revised retrievals.
		ArrayList<DocumentReference> goodDocRefs = getGoodDocRef(retrievals, correctRetrievals);
		// create the hash map vector for the original query string
		HashMapVector queryVector = (new TextStringDocument(query, stem)).hashMapVector();
		FeedbackSimulation feedback = new FeedbackSimulation(
																											queryVector,
																											Arrays.copyOfRange(retrievals, 0, N),
																											index,
																											goodDocRefs);
		
		// remove the documents which need to be skipped, including test set.
		index = (new InvertedIndexFactory(index)).removeDocRefs(Arrays.copyOfRange(retrievals, 0, M));
		
		// re-execute the query
		HashMapVector vector = feedback.newQuery();
		Retrieval[] revisedRetrievals = index.retrieve(vector);

    // Generate Recall/Precision points from the document indexed M and save in rpResults
    rpResults.add(evalRetrievals(revisedRetrievals, correctRetrievals));

    // Read the blank line delimiter between queries in the query file
    line = in.readLine();
    if (!(line == null || line.trim().equals(""))) {
      System.out.println("\nCould not find blank line after query, bad queryFile format");
      System.exit(1);
    }
    return true;
  }

	/**
	 * Return the documents that are relevant.
	 */
	ArrayList<DocumentReference> getGoodDocRef(Retrieval[] retrievals, ArrayList<String> correctRetrievals) {
		ArrayList<DocumentReference> result = new ArrayList<DocumentReference>();
		for (int i = 0; i < retrievals.length; i++) {
      // Check if this retrieval is in the list of relevant docs
      if (correctRetrievals.contains(retrievals[i].docRef.file.getName())) {
				result.add(retrievals[i].docRef);
			}
		}

		return result;
	}

  /**
   * Evaluate retrieval performance on a given query test corpus and
   * generate a recall/precision graph.
   * Command format: "Experiment [OPTION]* [N] [DIR] [QUERIES] [OUTFILE]" where:
	 * N the number of documents for which to simulate feedback.
   * DIR is the name of the directory whose files should be indexed.
   * QUERIES is a file of queries paired with relevant docs (see queryFile).
   * OUTFILE is the name of the file to put the output. The plot
   * data for the recall precision curve is stored in this file and a
   * gnuplot file for the graph is the same name with a ".gplot" extension.
	 *
   * OPTIONs can be
   * "-html" to specify HTML files whose HTML tags should be removed, and
   * "-stem" to specify tokens should be stemmed with Porter stemmer.
	 * "-skip" followed by value M. Top M documents are not used for testing.
   */
  public static void main(String[] args) throws IOException {
    // Parse the arguments into a directory name and optional flag
		int N = Integer.valueOf(args[args.length - 4]);
    String corpusDir = args[args.length - 3];
    String queryFile = args[args.length - 2];
    String outFile = args[args.length - 1];
    short docType = DocumentIterator.TYPE_TEXT;
    boolean stem = false;
		boolean readM = false;
    for (int i = 0; i < args.length - 4; i++) {
      String flag = args[i];
      if (flag.equals("-html"))
        // Create HTMLFileDocuments to filter HTML tags
        docType = DocumentIterator.TYPE_HTML;
      else if (flag.equals("-stem"))
        // Stem tokens with Porter stemmer
        stem = true;
			else if (flag.equals("-skip"))
				readM = true; // next "flag" is the value of M
			else if (readM) {
				M = Integer.valueOf(flag);
				readM = false; // disable when finish reading M
			}
      else {
        throw new IllegalArgumentException("Unknown flag: " + flag);
      }
    }
    
    // check the relationship with M and N
    if (M < N) {
			throw new RuntimeException("The number of documents skiped cannot be smaller than the number of documents used for feedback.\n" +
					"There's a overlap between training set and test set!");
		}
    
    ExperimentRelFeedback exper = new ExperimentRelFeedback(new File(corpusDir), new File(queryFile),
        new File(outFile), docType, stem, N);
    exper.makeRpCurve();
  }
}
