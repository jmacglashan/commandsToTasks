package logicalexpressions;

import burlap.oomdp.core.Domain;
import burlap.oomdp.core.GroundedProp;
import burlap.oomdp.core.states.State;


import java.util.Map;

/**
 * @author James MacGlashan.
 */
public class PFAtom extends LogicalExpression implements java.io.Serializable{

	public String [] pfParams;

	protected transient GroundedProp gp;

	/**
	 * Instantiates with the {@link burlap.oomdp.core.GroundedProp} that defines this {@link logicalexpressions.PFAtom}.
	 * @param gp the {@link burlap.oomdp.core.GroundedProp} that defines this {@link logicalexpressions.PFAtom}.
	 */
	public PFAtom(GroundedProp gp){
		this.gp = gp;
		String [] gpParamClasses = this.gp.pf.getParameterClasses();
		for(int i = 0; i < this.gp.params.length; i++){
			String varName = this.gp.params[i];
			String varType = gpParamClasses[i];
			if(!this.variablesAndTypes.containsKey(varName)){
				this.addVariable(varName, varType);
			}
		}
		this.setName(this.gp.pf.getName());
		this.pfParams = gp.params.clone();
	}

	public PFAtom() {
		// Blank constructor for use in parsing from knowledge base.
	}

	@Override
	public void instantiatedPropositionalFunctionsFromDomain(Domain domain){
		this.gp = new GroundedProp(domain.getPropFunction(this.name), this.pfParams);
	}

	/**
	 * Gets the {@link burlap.oomdp.core.GroundedProp} defining this {@link logicalexpressions.PFAtom}.
	 * @return the {@link burlap.oomdp.core.GroundedProp} defining this {@link logicalexpressions.PFAtom}.
	 */
	public GroundedProp getGroundedProp(){
		return this.gp;
	}


	/**
	 * Sets the {@link burlap.oomdp.core.GroundedProp} defining this {@link logicalexpressions.PFAtom}.
	 * @param gp the {@link burlap.oomdp.core.GroundedProp} defining this {@link logicalexpressions.PFAtom}.
	 */
	public void setGroundedProp(GroundedProp gp){
		this.gp = gp;
	}

	@Override
	public LogicalExpression duplicate() {
		if(this.gp != null) {
			GroundedProp ngp = new GroundedProp(this.gp.pf, this.gp.params);
			PFAtom natom = new PFAtom(ngp);
			return natom;
		}
		else{
			PFAtom natom = new PFAtom();
			natom.setName(this.name);
			natom.pfParams = this.pfParams.clone();
			return natom;
		}
	}

	@Override
	public boolean evaluateIn(State s) {
		return this.gp.isTrue(s);
	}

	@Override
	protected void remapVariablesInThisExpression(Map<String, String> fromToVariableMap) {
		for(int i = 0; i < this.gp.params.length; i++){
			this.gp.params[i] = fromToVariableMap.get(this.gp.params[i]);
		}
	}

	@Override
	public String toString() {
		return this.gp.pf.getClassName();
	}

}
