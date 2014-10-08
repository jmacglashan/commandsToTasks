package behavior.irl;

import behavior.planning.DeterministicGoalDirectedPartialVI;
import burlap.behavior.singleagent.planning.ValueFunctionPlanner;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.core.Domain;

public class DGDIRLFactory extends TabularIRLPlannerFactory {

	StateHashFactory				hashingFactory;
	
	public DGDIRLFactory(Domain domain, double gamma, StateHashFactory hashingFactory){
		this.irlpInit(domain, gamma);
		this.hashingFactory = hashingFactory;
	}
	
	
	@Override
	public ValueFunctionPlanner generatePlanner() {
		DeterministicGoalDirectedPartialVI planner = new DeterministicGoalDirectedPartialVI(domain, rf, tf, gamma, hashingFactory);
		if(this.actions != null){
			planner.setActions(this.actions);
		}
		return planner;
	}

}
