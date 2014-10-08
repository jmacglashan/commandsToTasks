package generativemodel;

public abstract class RVParameterIndex {
	
	protected int		hashCodeValue;
	protected boolean	needsToRecomputeHashCode = true;
	
	
	public abstract void computeHashCode();
	
	
	@Override
	public boolean equals(Object other){
		
		if(this == other){
			return true;
		}
		
		if(!(other instanceof RVParameterIndex)){
			return false;
		}
		
		RVParameterIndex rvpo = (RVParameterIndex)other;
		
		return this.hashCode() == rvpo.hashCode();
		
	}
	
	@Override
	public int hashCode(){
		if(this.needsToRecomputeHashCode){
			this.computeHashCode();
			this.needsToRecomputeHashCode = false;
		}
		return this.hashCodeValue;
	}
	
	
}
