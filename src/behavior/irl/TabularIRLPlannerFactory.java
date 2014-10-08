package behavior.irl;

import java.util.List;

import burlap.behavior.singleagent.planning.ValueFunctionPlanner;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.RewardFunction;

public abstract class TabularIRLPlannerFactory {

	protected Domain				domain;
	protected List<Action>			actions;
	protected RewardFunction		rf;
	protected TerminalFunction		tf;
	protected double				gamma;
	
	
	protected void irlpInit(Domain domain, double gamma){
		this.domain = domain;
		this.gamma = gamma;
		this.actions = null;
	}
	
	public void setActions(List<Action> actions){
		this.actions = actions;
	}
	
	public void changeGoal(RewardFunction rf, TerminalFunction tf){
		this.rf = rf;
		this.tf = tf;
	}
	
	public void chanageDomain(Domain domain){
		this.domain = domain;
	}
	
	public void changeGamma(double gamma){
		this.gamma = gamma;
	}
	
	
	
	public abstract ValueFunctionPlanner generatePlanner();
	
}
