package generativemodel.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import generativemodel.RVParameterIndex;
import generativemodel.RVariableValue;

public class MultiNomialRVPI extends RVParameterIndex {

	protected RVariableValue		generatedVarValue;
	protected List<RVariableValue>	conditionalVarValues;
	
	public MultiNomialRVPI() {
		conditionalVarValues = new ArrayList<RVariableValue>();
	}
	
	public MultiNomialRVPI(RVariableValue val) {
		this.setGenerated(val);
		conditionalVarValues = new ArrayList<RVariableValue>();		
	}
	
	public MultiNomialRVPI(RVariableValue val, List <RVariableValue> conds) {
		this.set(val, conds);
	}
	
	public void setGenerated(RVariableValue val){
		this.generatedVarValue = val;
	}
	
	public void setConditiona(List <RVariableValue> conds){
		this.conditionalVarValues = conds;
	}
	
	public void set(RVariableValue val, List <RVariableValue> conds){
		this.generatedVarValue = val;
		this.conditionalVarValues = conds;
	}
	
	public void addConditional(RVariableValue c){
		this.conditionalVarValues.add(c);
	}

	@Override
	public void computeHashCode() {
		
		String gsrep = generatedVarValue.toString(); 
		List <String> csreps = new ArrayList<String>(conditionalVarValues.size());
		int size = gsrep.length();
		for(RVariableValue rv : conditionalVarValues){
			String srv = rv.toString();
			csreps.add(srv);
			size += srv.length();
		}
		
		Collections.sort(csreps);
		
		String joinDelim = "&&^^";
		StringBuffer sbuf = new StringBuffer(size+csreps.size()*joinDelim.length());
		sbuf.append(gsrep);
		
		for(String srv : csreps){
			sbuf.append(joinDelim).append(srv);
		}
		
		this.hashCodeValue = sbuf.toString().hashCode();
		
	}

}
