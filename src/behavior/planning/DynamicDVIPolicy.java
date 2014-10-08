package behavior.planning;

import java.util.List;

import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.PlannerDerivedPolicy;
import burlap.behavior.singleagent.planning.commonpolicies.BoltzmannQPolicy;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.State;

public class DynamicDVIPolicy extends Policy implements PlannerDerivedPolicy {

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
	public void setPlanner(OOMDPPlanner planner) {
		this.planner = (DeterministicGoalDirectedPartialVI)planner;
		((PlannerDerivedPolicy)this.vfPolicy).setPlanner(planner);
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
