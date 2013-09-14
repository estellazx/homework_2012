package ir.vsr;

import java.io.File;

/**
 * Inverted Index class with more methods.
 * @author menie482
 *
 */
public class InvertedIndexFactory{
	/** handler of object being operated */
	private InvertedIndex index;
	
	public InvertedIndexFactory(InvertedIndex index) {
		this.index = index;
	}

	/**
	 * Remove some documents from the corpus.
	 * @param retrievals the docRef of which need to be removed.
	 */
	public InvertedIndex removeDocRefs(Retrieval[] retrievals) {
		for (Retrieval retrieval : retrievals) {
			index.docRefs.remove(retrieval.docRef);
		}
		
		return index;
	}
}
