package behavior.irl;

import java.util.ArrayList;
import java.util.List;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.planning.PlannerDerivedPolicy;
import burlap.behavior.singleagent.planning.ValueFunctionPlanner;
import burlap.behavior.singleagent.planning.commonpolicies.BoltzmannQPolicy;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.core.Attribute;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectClass;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.core.TransitionProbability;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

public class TabularIRL {
	
	public static final String				TERMINATEATTNAME="terminateAtt";
	public static final String				TERMINATECLASSNAME="terminateMarker";
	
	protected List<Action>					actions;
	protected PlannerDerivedPolicy			policy;
	protected TabularIRLPlannerFactory		plannerFactory;
	
	
	protected boolean						useTerminateAction;
	
	protected ObjectClass					terminateClassMarker;
	protected Attribute						terminateAttribute;
	protected Action						terminateAction;
	
	
	protected static double					baseTemp = 0.005;
	
	
	
	public TabularIRL(Domain domain, TabularIRLPlannerFactory plannerFactory){
		this.actions = domain.getActions();
		this.policy = new BoltzmannQPolicy(baseTemp);
		this.plannerFactory = plannerFactory;
	}
	
	public TabularIRL(Domain domain, TabularIRLPlannerFactory plannerFactory, boolean useTerminateAction){
		
		this.useTerminateAction = useTerminateAction;
		this.actions = domain.getActions();
		
		if(useTerminateAction){
			domain = domain.getNewDomainWithCopiedObjectClasses();
			this.setUpTerminateStructures(domain);
			terminateAction = new TerminateAction("#terminate#", domain, "");
			this.actions.add(terminateAction);
			plannerFactory.chanageDomain(domain);
			plannerFactory.setActions(actions);
		}
		
		
		this.policy = new BoltzmannQPolicy(baseTemp);
		this.plannerFactory = plannerFactory;
	}
	
	
	public void setTemperature(double t){
		this.policy = new BoltzmannQPolicy(t);
	}
	
	
	public double [] getBehaviorProbabilities(EpisodeAnalysis t, List<TaskCondition> conds){
		
		if(this.useTerminateAction){
			t = this.getTerminateAugmentedTrajectory(t);
		}
		
		double [] probs = new double[conds.size()];
		for(int i = 0; i < conds.size(); i++){
			
			TaskCondition cond = conds.get(i);
			probs[i] = this.getBehaviorProbabilityHelper(t, cond, true);
			
		}
		
		return probs;
	}
	
	public double [] getBehaviorLogProbabilities(EpisodeAnalysis t, List<TaskCondition> conds){
		
		if(this.useTerminateAction){
			t = this.getTerminateAugmentedTrajectory(t);
		}
		
		double [] logProbs = new double[conds.size()];
		for(int i = 0; i < conds.size(); i++){
			
			TaskCondition cond = conds.get(i);
			logProbs[i] = this.getBehaviorLogProbabilityHelper(t, cond, true);
			
		}
		
		return logProbs;
	}
	
	
	public double getBehaviorProbability(EpisodeAnalysis t, TaskCondition cond){
		
		//return this.getBehaviorProbabilityHelper(t, cond, false);
		return Math.exp(this.getBehaviorLogProbabilityHelper(t, cond, false));
		
	}
	
	public double getBehaviorLogProbability(EpisodeAnalysis t, TaskCondition cond){
		
		return this.getBehaviorLogProbabilityHelper(t, cond, false);
		
	}
	
	
	protected double getBehaviorProbabilityHelper(EpisodeAnalysis t, TaskCondition cond, boolean trajectoryConverted){
		if(this.useTerminateAction){
			TerminalFunction tf = new TFTerminalAction();
			RewardFunction rf = new RFTerminalActionWrapper(cond.rf);
			cond = new TaskCondition(rf, tf);
			if(!trajectoryConverted){
				t = this.getTerminateAugmentedTrajectory(t);
			}
		}
		
		this.setupPolicy(t, cond);
		
		//compute the probability of the trajectory
		double p = 1.;
		for(int i = 0; i < t.numTimeSteps()-1; i++){
			State s = t.getState(i);
			GroundedAction ga = t.getAction(i);
			double actionP = ((Policy)policy).getProbOfAction(s, ga);
			p *= actionP;
			
		}
		
		return p;
	}
	
	
	protected double getBehaviorLogProbabilityHelper(EpisodeAnalysis t, TaskCondition cond, boolean trajectoryConverted){
		if(this.useTerminateAction){
			TerminalFunction tf = new TFTerminalAction();
			RewardFunction rf = new RFTerminalActionWrapper(cond.rf);
			cond = new TaskCondition(rf, tf);
			if(!trajectoryConverted){
				t = this.getTerminateAugmentedTrajectory(t);
			}
		}
		
		this.setupPolicy(t, cond);
		
		//compute the probability of the trajectory
		double logsum = 0.;
		for(int i = 0; i < t.numTimeSteps()-1; i++){
			State s = t.getState(i);
			GroundedAction ga = t.getAction(i);
			double actionP = ((Policy)policy).getProbOfAction(s, ga);
			logsum += Math.log(actionP);
			
		}
		
		return logsum;
	}
	
	
	
	protected void setupPolicy(EpisodeAnalysis t, TaskCondition cond){
		plannerFactory.changeGoal(cond.rf, cond.tf);
		ValueFunctionPlanner planner = plannerFactory.generatePlanner();
		
		//compute v value for all neighbor states along the trajectory
		for(int i = 0; i < t.numTimeSteps(); i++){
			State s = t.getState(i);
			List <State> neighbors = this.getAllNeighbors(s);
			for(State n : neighbors){
				planner.planFromState(n);
			}
		}
		
		this.policy.setPlanner(planner);
	}
	
	

	
	protected List <State> getAllNeighbors(State s){
		
		List <State> neighbors = new ArrayList<State>();
		neighbors.add(s);
		
		for(Action a : actions){
			List <GroundedAction> gas = s.getAllGroundedActionsFor(a);
			for(GroundedAction ga : gas){
				List<TransitionProbability> transitions = ga.action.getTransitions(s, ga.params);
				for(TransitionProbability tp : transitions){
					neighbors.add(tp.s);
				}
			}
		}
		
		
		return neighbors;
	}
	
	
	protected EpisodeAnalysis getTerminateAugmentedTrajectory(EpisodeAnalysis t){
		
		State si = t.getState(0).copy();
		this.addTerminateWithValue(si, 0);
		
		EpisodeAnalysis nt = new EpisodeAnalysis(si);
		
		for(int i = 1; i < t.numTimeSteps(); i++){
			State sn = t.getState(i).copy();
			this.addTerminateWithValue(sn, 0);
			nt.recordTransitionTo(sn, t.getAction(i-1), 0.);
		}
		
		//add final state with terminate action
		State sf = t.getState(t.numTimeSteps()-1).copy();
		this.addTerminateWithValue(sf, 1);
		GroundedAction ga = new GroundedAction(terminateAction, "");
		nt.recordTransitionTo(sf, ga, 0.);
		
		return nt;
		
	}
	
	protected void addTerminateWithValue(State s, int v){
		ObjectInstance o = new ObjectInstance(terminateClassMarker, "terminatedMarker");
		o.setValue(TERMINATEATTNAME, v);
		s.addObject(o);
	}
	
	protected void setUpTerminateStructures(Domain d){
		terminateAttribute = new Attribute(d, TERMINATEATTNAME, Attribute.AttributeType.DISC);
		terminateAttribute.setDiscValuesForRange(0, 1, 1);
		
		terminateClassMarker = new ObjectClass(d, TERMINATECLASSNAME);
		terminateClassMarker.addAttribute(terminateAttribute);
	}
	
	
	public static class TaskCondition{
		
		public RewardFunction rf;
		public TerminalFunction tf;
		
		
		public TaskCondition(RewardFunction rf, TerminalFunction tf){
			this.rf = rf;
			this.tf = tf;
		}
		
		
	}
	
	
	public static class TerminateAction extends Action{

		public TerminateAction(String name, Domain domain,String parameterClasses) {
			super("#terminate#", domain, parameterClasses);
		}

		@Override
		protected State performActionHelper(State st, String[] params) {
			ObjectInstance o = st.getObject("terminatedMarker");
			o.setValue(TERMINATEATTNAME, 1);
			return st;
		}

		@Override
		public List<TransitionProbability> getTransitions(State s, String[] params) {
			return this.deterministicTransition(s, params);
		}
	}
	
	
	public static class TFTerminalAction implements TerminalFunction{

		@Override
		public boolean isTerminal(State s) {
			ObjectInstance o = s.getObject("terminatedMarker");
			int tv = o.getDiscValForAttribute(TERMINATEATTNAME);
			return tv == 1;
		}
		
		
	}
	
	public static class RFTerminalActionWrapper implements RewardFunction{

		protected RewardFunction rf;
		
		
		public RFTerminalActionWrapper(RewardFunction rf){
			this.rf = rf;
		}
		
		
		@Override
		public double reward(State s, GroundedAction a, State sprime) {
			
			if(a.action.getName().equals("#terminate#")){
				double r = rf.reward(s, a, sprime);
				return r;
			}
			
			return 0;
		}
		
		
		
		
	}
	
	
	
	
	
	
}
