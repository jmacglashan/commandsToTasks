package generativemodel;

import java.util.List;

public abstract class RVariableValue {
	
	protected RVariable			owner;
	
	
	public abstract boolean valueEquals(RVariableValue other);
	public abstract String stringRep();
	
	public void setOwner(RVariable owner){
		this.owner = owner;
	}
	
	public RVariable getOwner(){
		return owner;
	}
	
	public boolean isValueFor(RVariable var){
		return owner.equals(var);
	}
	
	
	@Override
	public boolean equals(Object other){
		
		if(this == other){
			return true;
		}
		
		if(!(other instanceof RVariableValue)){
			return false;
		}
		
		RVariableValue that = (RVariableValue) other;
		if(!this.owner.equals(that.owner)){
			return false;
		}
		
		return this.valueEquals((RVariableValue) other);
		
	}
	
	@Override
	public int hashCode(){
		return this.toString().hashCode();
	}
	
	@Override
	public String toString(){
		return this.stringRep();
	}
	
	
	public static RVariableValue extractValueForVariable(RVariable var, List<RVariableValue> vals){
		for(RVariableValue rval : vals){
			if(rval.owner.equals(var)){
				return rval;
			}
		}
		return null;
	}
	
}
