package generativemodel;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class GMModule {

	protected String								name;
	protected GenerativeModel						owner;
	protected List<RVariable>						rVariables;
	protected Map<String, RVariable>				rVariableMap;
	protected List<RVariable>						externalDependencyList;
	
	
	
	abstract public GMQueryResult computeProb(GMQuery query);
	abstract public ModelTrackedVarIterator getNonZeroProbIterator(RVariable queryVar, List <RVariableValue> conditions);
	abstract public Iterator<RVariableValue> getRVariableValuesFor(RVariable queryVar);
	
	
	
	public GMModule(String name){
		this.name = name;
		this.initDataStructures();
	}
	
	
	protected void initDataStructures(){
		this.rVariables = new ArrayList<RVariable>();
		this.rVariableMap = new HashMap<String, RVariable>();
		this.externalDependencyList = new ArrayList<RVariable>();
	}
	
	
	public void setOwner(GenerativeModel owner){
		this.owner = owner;
	}
	
	void addVariable(RVariable var){
		if(!rVariableMap.containsKey(var.name)){
			rVariables.add(var);
			rVariableMap.put(var.name, var);
		}
	}
	
	public GMQueryResult getProb(GMQuery query){
		
		GMQueryResult cachedResult = owner.queryCache.get(query);
		if(cachedResult != null){
			return cachedResult;
		}
		
		return this.computeProb(query);
		
	}
	
	
	public GMQueryResult getLogProb(GMQuery query){
		GMQueryResult pRes = this.getProb(query);
		pRes.probability = Math.log(pRes.probability);
		return pRes;
	}
	
	
	
	public final RVariableValue extractValueForVariableFromConditions(RVariable var, Collection <RVariableValue> conditions){
		for(RVariableValue rv : conditions){
			if(rv.isValueFor(var)){
				return rv;
			}
		}
		
		return null;
	}
	
	
	public ModelTrackedVarIterator getNonInfiniteLogProbIterator(RVariable queryVar, List <RVariableValue> conditions){
		
		final ModelTrackedVarIterator src = this.getNonZeroProbIterator(queryVar, conditions);
		
		ModelTrackedVarIterator iter = new ModelTrackedVarIterator() {
			
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
			@Override
			public boolean hasNext() {
				return src.hasNext();
			}
			
			@Override
			public GMQueryResult varSpecificNext() {
				GMQueryResult nextSrc = src.next();
				GMQueryResult logged = new GMQueryResult(nextSrc, Math.log(nextSrc.probability));
				return logged;
			}
		};
		
		return iter;
		
	}
	
	
	
	@Override
	public boolean equals(Object other){
		if(this == other){
			return true;
		}
		
		if(!(other instanceof GMModule)){
			return false;
		}
		
		return this.name.equals(((GMModule)other).name);
	}
	
}
