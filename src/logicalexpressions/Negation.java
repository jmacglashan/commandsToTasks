package logicalexpressions;

import burlap.oomdp.core.State;

import java.util.Map;

/**
 * @author James MacGlashan.
 */
public class Negation extends LogicalExpression{

	public Negation() {
		// Empty constructor in case child is not known yet (as in reading in from a file)
	}

	/**
	 * Instantiates a negation of the provided expression.
	 * @param childExpression the expression to negate.
	 */
	public Negation(LogicalExpression childExpression) {
		this.childExpressions.add(childExpression);
	}


	@Override
	public LogicalExpression duplicate() {
		return new Negation(this.childExpressions.get(0));
	}

	@Override
	public boolean evaluateIn(State s) {
		return (!this.childExpressions.get(0).evaluateIn(s));
	}

	@Override
	protected void remapVariablesInThisExpression(Map<String, String> fromToVariableMap) {
		// Nothing necessary, not an atomic expression
	}

	/**
	 * Sets the single child expression that is being negated.
	 * @param child the child expression that is being negated.
	 */
	public void setChild(LogicalExpression child) {
		this.childExpressions.add(child);
	}

	@Override
	public String toString() {

		LogicalExpression c = this.childExpressions.get(0);
		if(!(c instanceof PFAtom)){
			return "!(" + c.toString() + ")";
		}

		return "!" + this.childExpressions.get(0);
	}

}
