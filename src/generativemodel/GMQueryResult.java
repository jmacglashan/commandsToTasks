package generativemodel;

import java.util.List;

public class GMQueryResult extends GMQuery{
	public double							probability;
	
	public GMQueryResult(){
		super();
	}
	
	public GMQueryResult(double p){
		super();
		this.probability = p;
	}
	
	public GMQueryResult(GMQuery superSrc){
		super(superSrc);
	}
	
	public GMQueryResult(GMQuery superSrc, double p){
		super(superSrc);
		this.probability = p;
	}
	
	public static GMQueryResult maxProb(List<GMQueryResult> distribution){
		double max = Double.NEGATIVE_INFINITY;
		GMQueryResult maxE = null;
		for(GMQueryResult dEntry : distribution){
			if(dEntry.probability > max){
				maxE = dEntry;
				max = dEntry.probability;
			}
		}
		
		return maxE;
	}
	
	
	
}
