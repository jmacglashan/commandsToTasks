package logicalexpressions;

import burlap.oomdp.core.State;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * A logical expression of OO-MDP propositional functions. This is the super abstract class that all
 * specific expression derive. It recursively consists of a set of child logical expressions whose relationship
 * is defined by the instance of the Logical Expression. For example, a {@link logicalexpressions.Conjunction}
 * subclass means that each of the child expressions are assume to be "anded" together, but may recursively
 * decompose themselves into complex non-terminal logical expressions.
 * <p/>
 * At the lowest level, you have the terminal logical expression {@link logicalexpressions.PFAtom} which
 * is defined by a single OO-MDP propositional function and variables (or object constants) on which it operates) using
 * a {@link burlap.oomdp.core.GroundedProp} reference.
 * <p/>
 * Each Logical expression maintains a map of each variable referenced somewhere in its child expressions (it
 * works recursively so if a child expression is a non-terminal it will know about the variables in its decedents)
 * to the OO-MDP object class type to which that variable must belong. You can retrieve this map with
 * the method {@link #getVariableAndTypes()}.
 *
 *
 * @author James MacGlashan.
 */
public abstract class LogicalExpression {

	protected Map<String, String>			variablesAndTypes = new HashMap<String, String>();
	protected LogicalExpression 			parentExpression = null;
	protected List<LogicalExpression> 		childExpressions = new ArrayList<LogicalExpression>();
	protected String						name; // For Debugging purposes


	/**
	 * Creates a new instance of this Logical expression *without* a reference to a parent expression.
	 * @return a duplicated version of this LogicalExpression instance.
	 */
	public abstract LogicalExpression duplicate();

	/**
	 * Evaluates whether this logical expression is true in the given OO-MDP {@link burlap.oomdp.core.State}.
	 * @param s that {@link burlap.oomdp.core.State} in which to evaluate this expression.
	 * @return true if this expression is true in s; false otherwise.
	 */
	public abstract boolean evaluateIn(State s);

	/**
	 * Find all variables in this expression non-recursively with name in the keyset of the provided {@link java.util.Map}
	 * and change its name to the varibale name in the corresponding value of the provided {@link java.util.Map}.
	 * This method only needs to do anything for terminal expressions (i.e. the {@link logicalexpressions.PFAtom}).
	 * @param fromToVariableMap the variable rename {@link java.util.Map}
	 */
	protected abstract void remapVariablesInThisExpression(Map<String, String> fromToVariableMap);


	/**
	 * First creates a duplicate of this logical expression and then recursively remaps its variable names according to the
	 * entries in the provided {@link java.util.Map}.
	 * @param fromToVariableMap the variable rename {@link java.util.Map}
	 * @return a new {@link logicalexpressions.LogicalExpression} with its variable names remapped.
	 */
	public LogicalExpression duplicateWithVariableRemap(Map<String, String> fromToVariableMap){
		LogicalExpression copy = this.duplicate();
		copy.remapVariables(fromToVariableMap);
		return copy;
	}

	/**
	 * Returns each variable expressed in this logical expression (found recursively) and returns a map from
	 * their name to the OO-MDP object class to which the variable is typed.
	 * @return a {@link java.util.Map} from variable names to their OO-MDP object class type.
	 */
	public Map<String, String> getVariableAndTypes(){
		return this.variablesAndTypes;
	}


	/**
	 * Returns the parent logical expression that holds this expression. Null if there is no parent.
	 * @return the parent logical expression that holds this expression. Null if there is no parent.
	 */
	public LogicalExpression getParentExpression(){
		return this.parentExpression;
	}

	public void setParentExpression(LogicalExpression parentExpression){
		this.parentExpression = parentExpression;
		for(Map.Entry<String, String> vt : this.variablesAndTypes.entrySet()){
			this.parentExpression.addVariable(vt.getKey(), vt.getValue());
		}
	}

	/**
	 * Sets the name of this logical expression which is only used for debugging purposes.
	 * @param name the name of this logical expression
	 */
	public void setName(String name) {
		this.name = name;
	}


	/**
	 * Returns all logical expression children of this expression.
	 * @return all logical expression children of this expression.
	 */
	public List<LogicalExpression> getChildExpressions(){ return this.childExpressions; }


	protected void addVariable(String variableName, String variableType){

		if(this.variablesAndTypes.containsKey(variableName)){
			throw new VariableAlreadyInUseException(variableName);
		}

		this.variablesAndTypes.put(variableName, variableType);
		if(this.parentExpression != null){
			this.parentExpression.addVariable(variableName, variableType);
		}
	}


	/**
	 * Recursively remaps variable names telling its parent about the changes.
	 * @param fromToVariableMap the current variable names and their new target name
	 */
	protected void remapVariables(Map<String, String> fromToVariableMap){
		this.remapVariablesInVariableAndTypeMap(fromToVariableMap);
		this.remapVariablesInThisExpression(fromToVariableMap);
		this.remapVariablesUpStream(fromToVariableMap);
		this.remapVariablesDownStream(fromToVariableMap);
	}


	protected void remapVariablesInVariableAndTypeMap(Map<String, String> fromToVariableMap){
		//to protect against variable mapping name swaps create a copy
		Map<String, String> newVT = new HashMap<String, String>();
		for(Map.Entry<String, String> nn : fromToVariableMap.entrySet()){
			String from = nn.getKey();
			String to = nn.getValue();
			String type = this.variablesAndTypes.get(from);
			newVT.put(to, type);
		}

		for(Map.Entry<String, String> on : this.variablesAndTypes.entrySet()){
			String oldName = on.getKey();
			if(!fromToVariableMap.containsKey(oldName)){
				newVT.put(oldName, on.getValue());
			}
		}

		this.variablesAndTypes = newVT;

	}


	protected void remapVariablesDownStream(Map<String, String> fromToVariableMap){
		for(LogicalExpression exp : this.childExpressions){
			exp.remapVariablesInVariableAndTypeMap(fromToVariableMap);
			exp.remapVariablesInThisExpression(fromToVariableMap);
			exp.remapVariablesDownStream(fromToVariableMap);
		}
	}

	protected void remapVariablesUpStream(Map<String, String> fromToVariableMap){
		if(this.parentExpression != null){
			this.parentExpression.remapVariablesInVariableAndTypeMap(fromToVariableMap);
			this.parentExpression.remapVariablesInThisExpression(fromToVariableMap);
			this.parentExpression.remapVariablesUpStream(fromToVariableMap);
		}
	}



	public class VariableAlreadyInUseException extends RuntimeException{

		private static final long serialVersionUID = 4641273304404441272L;
		public final String variableName;

		public VariableAlreadyInUseException(String variableName){
			super("The variable name " + variableName + "is already in use in this expression");
			this.variableName = variableName;
		}

	}




}
