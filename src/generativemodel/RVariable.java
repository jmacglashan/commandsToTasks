package generativemodel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RVariable {

	protected String								name;
	protected GMModule								moduleOwner;
	protected List<RVariable>						dependencies;
	protected Map<RVParameterIndex, Double>			parameters;
	protected boolean								isInput;
	
	

	public RVariable(String name, GMModule moduleOwner){
		this.RVarInit(name, moduleOwner, new ArrayList<RVariable>(), new HashMap<RVParameterIndex, Double>(), false);
	}
	
	
	/**
	 * This constructor only takes a name and without a module owner is assumed to be an input
	 * @param name The name of the variable
	 */
	public RVariable(String name){
		this.RVarInit(name, null, new ArrayList<RVariable>(), new HashMap<RVParameterIndex, Double>(), true);
	}
	
	
	protected void RVarInit(String name, GMModule moduleOwner, List<RVariable> dependencies, Map<RVParameterIndex, Double> parameters, boolean isInput){
		this.name = name;
		this.moduleOwner = moduleOwner;
		this.dependencies = dependencies;
		this.parameters = parameters;
		this.isInput = isInput;
		
		if(this.moduleOwner != null){
			this.moduleOwner.addVariable(this);
		}
		
	}
	
	public String getName(){
		return name;
	}
	
	public GMModule getOwner(){
		return moduleOwner;
	}
	
	public boolean isInput(){
		return this.isInput;
	}
	
	public void setDependencies(List <RVariable> dependencies){
		this.dependencies = dependencies;
	}
	
	public void addDependency(RVariable dep){
		if(!this.dependencies.contains(dep)){
			this.dependencies.add(dep);
		}
	}
	
	public List <RVariable> getDependencies(){
		return this.dependencies;
	}
	
	public int geNumStoredParams(){
		return parameters.size();
	}
	
	public Double getParameter(RVParameterIndex paramInd){
		return parameters.get(paramInd);
	}
	
	public double getParameter(RVParameterIndex paramInd, double defaultValue){
		Double stored = this.parameters.get(paramInd);
		if(stored == null){
			this.parameters.put(paramInd, defaultValue);
			return defaultValue;
		}
		return stored;
	}
	
	public void setParam(RVParameterIndex paramInd, double val){
		this.parameters.put(paramInd, val);
	}
	
	public boolean isDependentOn(RVariable r){
		for(RVariable d : this.dependencies){
			if(r.equals(d)){
				return true;
			}
		}
		
		return false;
	}
	
	
	@Override
	public boolean equals(Object other){
		
		if(this == other){
			return true;
		}
		
		if(!(other instanceof RVariable)){
			return false;
		}
		
		RVariable rvother = (RVariable)other;
		
		return this.name.equals(rvother.name) && this.moduleOwner == rvother.moduleOwner;
		
	}
	
	@Override
	public int hashCode(){
		return this.name.hashCode();
	}
	
	
}
