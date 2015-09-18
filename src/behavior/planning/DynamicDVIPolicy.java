package behavior.planning;

import java.util.List;


import burlap.behavior.policy.BoltzmannQPolicy;
import burlap.behavior.policy.GreedyQPolicy;
import burlap.behavior.policy.Policy;
import burlap.behavior.policy.SolverDerivedPolicy;
import burlap.behavior.singleagent.MDPSolverInterface;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.states.State;


public class DynamicDVIPolicy extends Policy implements SolverDerivedPolicy {

	protected DeterministicGoalDirectedPartialVI		planner;
	protected Policy									vfPolicy;
	
	public DynamicDVIPolicy(DeterministicGoalDirectedPartialVI planner, double boltzTemp){
		this.planner = planner;
		if(boltzTemp == 0.){
			this.vfPolicy = new GreedyQPolicy(planner);
		}
		else{
			this.vfPolicy = new BoltzmannQPolicy(planner, boltzTemp);
		}
	}

	@Override
	public void setSolver(MDPSolverInterface planner) {
		this.planner = (DeterministicGoalDirectedPartialVI)planner;
		((SolverDerivedPolicy)this.vfPolicy).setSolver(planner);
	}

	@Override
	public AbstractGroundedAction getAction(State s) {
		if(!this.planner.planDefinedForState(s)){
			this.planner.planFromState(s);
		}
		return this.vfPolicy.getAction(s);
	}

	@Override
	public List<ActionProb> getActionDistributionForState(State s) {
		if(!this.planner.planDefinedForState(s)){
			this.planner.planFromState(s);
		}
		return this.vfPolicy.getActionDistributionForState(s);
	}

	@Override
	public boolean isStochastic() {
		return this.vfPolicy.isStochastic();
	}

	@Override
	public boolean isDefinedFor(State s) {
		return true;
	}

}
