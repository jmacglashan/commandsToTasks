package generativemodel;

import java.util.Iterator;

/**
 * This class is a Iterator wrapper for GMQueryResults from a given module with some extra logic to cache the iterated results if
 * the Generative Model owner requests to do so. The standard hasNext() and remove() methods should be implemneted by the subclass.
 * The varSpecificNext() method is the effective next method that should be overriden to provide next functionaltiy.
 * @author James MacGlashan
 *
 */
public abstract class ModelTrackedVarIterator implements Iterator<GMQueryResult> {

	protected GenerativeModel	modelOwner;
	protected boolean			cache;
	

	
	public abstract GMQueryResult varSpecificNext();
	

	@Override
	public final GMQueryResult next() {
		
		GMQueryResult n = this.varSpecificNext();
		if(this.cache){
			modelOwner.manualCache(n);
		}
		
		return n;
	}

	
	public void GMIniter(GenerativeModel modelOwner, boolean cache){
		this.modelOwner = modelOwner;
		this.cache = cache;
	}
	

}
