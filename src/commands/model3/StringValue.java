package commands.model3;


import generativemodel.RVariable;
import generativemodel.RVariableValue;

public class StringValue extends RVariableValue {

	public String			s;

	public StringValue(String s, RVariable owner){
		this.s = s;
		//this.s = s.toLowerCase();
		this.setOwner(owner);
	}
	
	
	@Override
	public boolean valueEquals(RVariableValue other) {
		
		if(this == other){
			return true;
		}
		
		if(!(other instanceof StringValue)){
			return false;
		}
		
		if(!this.owner.equals(other.getOwner())){
			return false;
		}
		
		StringValue that = (StringValue)other;

		return this.s.equals(that.s);
	}

	@Override
	public String stringRep() {
		return s;
	}

}
