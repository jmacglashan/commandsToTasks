package generativemodel.common;

import java.util.Iterator;
import java.util.List;

import generativemodel.GMQuery;
import generativemodel.GMQueryResult;
import generativemodel.GenerativeModel;
import generativemodel.ModelTrackedVarIterator;
import generativemodel.RVariableValue;

public class LPInfRejectQRIterator extends ModelTrackedVarIterator {

	Iterator<RVariableValue>			varIter;
	List<RVariableValue>				conditions;
	GMQueryResult						nextResult;
	
	public LPInfRejectQRIterator(Iterator<RVariableValue> varIter, List<RVariableValue> conditions, GenerativeModel owner){
		this.varIter = varIter;
		this.conditions = conditions;
		this.modelOwner = owner;
		nextResult = null;
		this.setNext();
	}
	
	@Override
	public boolean hasNext() {
		return nextResult != null;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public GMQueryResult varSpecificNext() {
		GMQueryResult result = nextResult;
		this.setNext();
		return result;
	}

	protected void setNext(){
		
		do{
			
			if(!varIter.hasNext()){
				nextResult = null;
				break ;
			}
			
			RVariableValue rvv = varIter.next();
			GMQuery gq = new GMQuery();
			gq.addQuery(rvv);
			for(RVariableValue crvv : conditions){
				gq.addCondition(crvv);
			}
			
			nextResult = modelOwner.getLogProb(gq, cache);
			
		}while(nextResult != null && nextResult.probability == Double.NEGATIVE_INFINITY);
		
	}

}
