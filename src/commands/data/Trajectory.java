package commands.data;

import java.util.ArrayList;
import java.util.List;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;

public class Trajectory {

	public List <State>				states;
	public List <GroundedAction>		actions;
	
	public Trajectory(State initialState){
		states = new ArrayList<State>();
		states.add(initialState);
		actions = new ArrayList<GroundedAction>();
	}
	
	//assume this is well formed (states has one more element than actions)
	public Trajectory(List <State> states, List <GroundedAction> actions){
		this.states = states;
		this.actions = actions;
	}
	
	public Trajectory(Trajectory t){
		states = t.states;
		actions = t.actions;
	}
	
	public EpisodeAnalysis convertToZeroRewardEpisodeAnalysis(){
		
		EpisodeAnalysis ea = new EpisodeAnalysis(this.states.get(0));
		
		for(int i = 1; i < states.size(); i++){
			ea.recordTransitionTo(states.get(i), actions.get(i-1), 0.);
		}
		
		return ea;
		
	}
	
	public int numStates(){
		return states.size();
	}
	
	
	//returns the i'th state
	public State getState(int i){
		return states.get(i);
	}
	
	
	//return the action performed in the i'th state
	public GroundedAction getAction(int i){
		if(i > actions.size()){ //there are more states than actions since there is no action when the sequence stops
			return null;
		}
		return actions.get(i);
	}
	
	
	public void addActionStateTransition(GroundedAction a, State sprime){
		actions.add(a);
		states.add(sprime);
	}
	
	
	
	
	
	
}
