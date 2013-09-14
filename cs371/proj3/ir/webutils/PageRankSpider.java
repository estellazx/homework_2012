package ir.webutils;

import java.util.*;
import java.io.*;

import ir.utilities.*;

/**
 * Spider defines a framework for writing a web crawler.  Users can
 * change the behavior of the spider by overriding methods.
 * Default spider does a breadth first crawl starting from a
 * given URL up to a specified maximum number of pages, saving (caching)
 * the pages in a given directory.  Also adds a "BASE" HTML command to
 * cached pages so links can be followed from the cached version.
 *
 * @author Ted Wild and Ray Mooney
 */

public class PageRankSpider extends Spider {
	private Graph graph;
	private Map<Node, Double> rank;
	private Map<String, String> fileNameMap;
	private List<Link> linkFrom; // keep in same index with linksToVisit, record where it linked from

	private double alpha = 0.15;
	private double iterations = 50;

  /**
   * Performs the crawl.  Should be called after
   * <code>processArgs</code> has been called.  Assumes that
   * starting url has been set.  <p> This implementation iterates
   * through a list of links to visit.  For each link a check is
   * performed using {@link #visited visited} to make sure the link
   * has not already been visited.  If it has not, the link is added
   * to <code>visited</code>, and the page is retrieved.  If access
   * to the page has been disallowed by a robots.txt file or a
   * robots META tag, or if there is some other problem retrieving
   * the page, then the page is skipped.  If the page is downloaded
   * successfully {@link #indexPage indexPage} and {@link
   * #getNewLinks getNewLinks} are called if allowed.
   * <code>go</code> terminates when there are no more links to visit
   * or <code>count &gt;= maxCount</code>
	 *
	 * It also construct the pagerank graph.
   */
  public void doCrawl() {
    if (linksToVisit.size() == 0) {
      System.err.println("Exiting: No pages to visit.");
      System.exit(0);
    }
    visited = new HashSet<Link>();
		graph = new Graph();
		fileNameMap = new HashMap<String, String>();
		linkFrom = new ArrayList<Link>();

		linkFrom.add(null); // null points to root

    while (linksToVisit.size() > 0 && count < maxCount) {
      // Pause if in slow mode
      if (slow) {
        synchronized (this) {
          try {
            wait(1000);
          }
          catch (InterruptedException e) {
          }
        }
      }
      // Take the top link off the queue
      Link link = linksToVisit.remove(0);
			Link parent = linkFrom.remove(0);
      System.out.println("Trying: " + link);
      // Skip if already visited this page
      if (!visited.add(link)) {
        System.out.println("Already visited");

				// Make sure this is a valid node - not a bad link or something
				if (parent != null)
					for (Node node : graph.nodeArray())
						if (node.toString().equals(link.toString()) && !parent.toString().equals(link.toString()))
							// It's a node in the graph, so it's not only visited but also valid
							graph.addEdge(parent.toString(), link.toString());

				continue;
      }
      if (!linkToHTMLPage(link)) {
        System.out.println("Not HTML Page");
        continue;
      }
      HTMLPage currentPage = null;
      // Use the page retriever to get the page
      try {
        currentPage = retriever.getHTMLPage(link);
      }
      catch (PathDisallowedException e) {
        System.out.println(e);
        continue;
      }
      if (currentPage.empty()) {
        System.out.println("No Page Found");
        continue;
      }
      if (currentPage.indexAllowed()) {
        count++;
        System.out.println("Indexing" + "(" + count + "): " + link);
        indexPage(currentPage);

				graph.addNode(link.toString()); // just make sure this node has been in the graph
				if (parent != null && !parent.toString().equals(link.toString()))
					graph.addEdge(parent.toString(), link.toString());

				fileNameMap.put(link.toString(), indexName()); // mapping from link to indexed file name
      }
      if (count < maxCount) {
        List<Link> newLinks = getNewLinks(currentPage);
        // System.out.println("Adding the following links" + newLinks);
        // Add new links to end of queue
        linksToVisit.addAll(newLinks);

				for (Link newLink : newLinks) {
					boolean nodeAppeared = false;
					for (Node node : graph.nodeArray())
						if (node.toString().equals(newLink.toString()) && !link.toString().equals(newLink.toString())) {
							graph.addEdge(link.toString(), newLink.toString());
							linkFrom.add(null); // no need to care link as parent later
							nodeAppeared = true;
							break;
						}

					if (!nodeAppeared) {
						// if this link is already in nodes, which is valid, add the edge directly
						linkFrom.add(link); // specify the parent of these links
					}
				}
      }
    }

		graphTest();

		pageRank();
  }

	protected String indexName() {
		return "P" + MoreString.padWithZeros(count, (int) Math.floor(MoreMath.log(maxCount, 10)) + 1) + ".html";
	}

	/**
	 * Based on the graph structure, calculate the pagerank value.
	 */
	protected void pageRank() {
		int numOfPages = graph.nodeArray().length;
		double e = alpha / numOfPages;
		rank = new HashMap<Node, Double>();

		// initialize rank
		for (Node node : graph.nodeArray()) {
			rank.put(node, 1.0 / numOfPages);
		}

		for (int iter = 0; iter < iterations; iter++) {
			Map<Node, Double> tmpRank = new HashMap<Node, Double>(); // which is R'

			for (Node node : graph.nodeArray()) {
				double sum = 0;

				// calculate the sum of input flows
				for (Node priNode : node.getEdgesIn()) {
					sum += rank.get(priNode) / priNode.getEdgesOut().size();
				}

				tmpRank.put(node, (1 - alpha) * sum + e);
			}

			double c = 0;
			// calculate the sum of ranks, for normalization.
			for (double value : tmpRank.values()) {
				c += value;
			}

			// update the rank
			for (Node node : graph.nodeArray()) {
				rank.put(node, tmpRank.get(node) / c);
			}
		}

		pageRankTest();
		pageRankWrite();
	}

	protected void pageRankWrite() {
		try {
			FileWriter fstream = new FileWriter(saveDir + "/pageRanks");
			BufferedWriter out = new BufferedWriter(fstream);

			for (Map.Entry<Node, Double> entry : rank.entrySet()) {
				String link = entry.getKey().toString();
				String filename = fileNameMap.get(link);
				Double value = entry.getValue();

				out.write(filename + " " + value + "\n");
			}

			out.close();
		}
		catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}
	}

	private void pageRankTest() {
		System.out.println("Page Rank:");
		for (Node node : graph.nodeArray()) {
			System.out.println(node.toString() + ": " + rank.get(node));
		}
	}

	private void graphTest() {
		System.out.println("Graph Structure:");

		for (Node node : graph.nodeArray()) {
			System.out.println(node + "-> " + node.getEdgesOut());
		}
	}

  public static void main(String args[]) {
    new PageRankSpider().go(args);
  }
}
