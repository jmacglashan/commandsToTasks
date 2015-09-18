package behavior.irl;

import behavior.planning.DeterministicGoalDirectedPartialVI;
import burlap.behavior.singleagent.planning.Planner;
import burlap.oomdp.core.Domain;
import burlap.oomdp.statehashing.HashableStateFactory;

public class DGDIRLFactory extends TabularIRLPlannerFactory {

	HashableStateFactory hashingFactory;
	
	public DGDIRLFactory(Domain domain, double gamma, HashableStateFactory hashingFactory){
		this.irlpInit(domain, gamma);
		this.hashingFactory = hashingFactory;
	}
	
	
	@Override
	public Planner generatePlanner() {
		DeterministicGoalDirectedPartialVI planner = new DeterministicGoalDirectedPartialVI(domain, rf, tf, gamma, hashingFactory);
		if(this.actions != null){
			planner.setActions(this.actions);
		}
		return planner;
	}

}
