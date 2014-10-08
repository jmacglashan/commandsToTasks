package generativemodel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;



public class GMQuery {
	protected	Set<RVariableValue>				queryValues;
	protected	Set<RVariableValue>				condValues;
	
	protected	int								hashValue;
	protected	boolean							needsToRecomputeHash;
	
	public GMQuery(){
		this.queryValues = new HashSet<RVariableValue>();
		this.condValues = new HashSet<RVariableValue>();
		needsToRecomputeHash = true;
	}
	
	public GMQuery(GMQuery src){
		this.queryValues = new HashSet<RVariableValue>(src.queryValues);
		this.condValues = new HashSet<RVariableValue>(src.condValues);
		this.hashValue = src.hashValue;
		this.needsToRecomputeHash = src.needsToRecomputeHash;
	}
	
	
	public void addQuery(RVariableValue query){
		this.queryValues.add(query);
		needsToRecomputeHash = true;
	}
	public void addCondition(RVariableValue cond){
		this.condValues.add(cond);
		needsToRecomputeHash = true;
	}
	
	public void removeQuery(RVariableValue query){
		this.queryValues.remove(query);
		needsToRecomputeHash = true;
	}
	
	public void removeCondition(RVariableValue cond){
		this.condValues.remove(cond);
		needsToRecomputeHash = true;
	}
	
	public void setConditions(Collection <RVariableValue> conds){
		this.condValues = new HashSet<RVariableValue>(conds);
		needsToRecomputeHash = true;
	}
	
	public int getNumQueryVars(){
		return this.queryValues.size();
	}
	
	public int getNumConditionVars(){
		return this.condValues.size();
	}
	
	public Set<RVariableValue> getQueryValues(){
		return new HashSet<RVariableValue>(this.queryValues);
	}
	
	public Set<RVariableValue> getConditionValues(){
		return new HashSet<RVariableValue>(this.condValues);
	}
	
	public RVariableValue getSingleQueryVar(){
		Iterator<RVariableValue> iter = queryValues.iterator();
		RVariableValue rvv = iter.next();
		return rvv;
	}
	
	public RVariableValue getQueryForVariable(RVariable var){
		for(RVariableValue rvv : queryValues){
			if(rvv.isValueFor(var)){
				return rvv;
			}
		}
		return null;
	}
	
	public RVariableValue getConditionForVariable(RVariable var){
		for(RVariableValue rvv : condValues){
			if(rvv.isValueFor(var)){
				return rvv;
			}
		}
		return null;
	}
	
	@Override
	public boolean equals(Object other){
		if(this == other){
			return true;
		}
		
		if(!(other instanceof GMQuery)){
			return false;
		}
		
		GMQuery gmqo = (GMQuery)other;
		
		if(this.queryValues.size() != gmqo.queryValues.size() || this.condValues.size() != gmqo.condValues.size()){
			return false;
		}
		
		for(RVariableValue qv : this.queryValues){
			if(!gmqo.queryValues.contains(qv)){
				return false;
			}
		}
		
		for(RVariableValue cv : this.condValues){
			if(!gmqo.condValues.contains(cv)){
				return false;
			}
		}
		
		return true;
		
	}
	
	
	
	@Override
	public int hashCode(){
		if(needsToRecomputeHash){
			this.computeHash();
			needsToRecomputeHash = false;
		}
		return this.hashValue;
	}
	
	
	
	protected void computeHash(){
		
		List<String> allStringReps = new ArrayList<String>(this.queryValues.size()+this.condValues.size());
		
		int size = 0;
		
		for(RVariableValue v : this.queryValues){
			String vs = v.toString();
			size += vs.length();
			allStringReps.add(vs);
		}
		for(RVariableValue v : this.condValues){
			String vs = v.toString();
			size += vs.length();
			allStringReps.add(vs);
		}
		
		//sort the variable values to get order invariance
		Collections.sort(allStringReps);
		
		//join them
		String joinDelim = "&&^^";
		StringBuffer sbuf = new StringBuffer(size+allStringReps.size()*joinDelim.length());
		for(int i = 0; i < allStringReps.size(); i++){
			if(i > 0){
				sbuf.append(joinDelim);
			}
			sbuf.append(allStringReps.get(i));
		}
		
		
		this.hashValue = sbuf.toString().hashCode();
		
	}
	
	
}
