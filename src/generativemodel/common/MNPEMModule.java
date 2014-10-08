package generativemodel.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


import generativemodel.GMModule;
import generativemodel.RVariable;
import generativemodel.RVariableValue;

public abstract class MNPEMModule extends GMModule {

	public MNPEMModule(String name) {
		super(name);
	}




	
	
	protected double computeMNParamProb(RVariableValue qv, Set<RVariableValue> allConditionals){
		List <RVariableValue> deps = this.getVarDependencySet(qv, allConditionals);
		MultiNomialRVPI ind = new MultiNomialRVPI(qv, deps);
		double p = qv.getOwner().getParameter(ind);
		
		
		return p;
	}

	
	protected List <RVariableValue> getVarDependencySet(RVariableValue query, Set<RVariableValue> set){
		
		List <RVariable> dependencyList = query.getOwner().getDependencies();
		List <RVariableValue> dependencyValues = new ArrayList<RVariableValue>(dependencyList.size());
		
		for(RVariableValue cond : set){
			if(dependencyList.contains(cond.getOwner())){
				dependencyValues.add(cond);
			}
		}
		
		return dependencyValues;
		
	}
	
	
}
