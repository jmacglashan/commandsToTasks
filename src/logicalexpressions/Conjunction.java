package logicalexpressions;

import burlap.oomdp.core.State;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author James MacGlashan.
 */
public class Conjunction extends LogicalExpression{

	public Conjunction(LogicalExpression...expressions){
		for(LogicalExpression exp : expressions){
			this.childExpressions.add(exp);
			exp.setParentExpression(this);
		}

	}

	public Conjunction(List<LogicalExpression> expressions){
		for(LogicalExpression exp : expressions){
			this.childExpressions.add(exp);
			exp.setParentExpression(this);
		}

	}

	public void addChild(LogicalExpression newChild) {
		newChild.parentExpression = this;
		this.childExpressions.add(newChild);
	}

	@Override
	public LogicalExpression duplicate() {

		List<LogicalExpression> dupTerms = new ArrayList<LogicalExpression>(this.childExpressions.size());
		for(LogicalExpression exp : this.childExpressions){
			dupTerms.add(exp.duplicate());
		}

		Conjunction conjunction = new Conjunction(dupTerms);

		return conjunction;
	}

	@Override
	public boolean evaluateIn(State s) {

		for(LogicalExpression exp : this.childExpressions){
			if(!exp.evaluateIn(s)){
				return false;
			}
		}

		return true;

	}

	@Override
	protected void remapVariablesInThisExpression(Map<String, String> fromToVariableMap) {
		//nothing necssary, not an atomic expression
	}

	@Override
	public String toString() {
		String result = "";

		for(int i = 0; i < this.childExpressions.size() - 1; i++) {
			LogicalExpression c = this.childExpressions.get(i);
			if(!(c instanceof  PFAtom)){
				result = result + "(" + c.toString() + ") ^ ";
			}
			else{
				result = result + c.toString() + " ^ ^";
			}
		}
		LogicalExpression c = this.childExpressions.get(this.childExpressions.size()-1);
		if(!(c instanceof PFAtom)){
			result = result + "(" + c.toString() + ")";
		}
		else{
			result = result + c.toString();
		}

		return result;
	}

}
