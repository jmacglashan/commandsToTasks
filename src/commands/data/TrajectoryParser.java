package commands.data;

import java.util.ArrayList;
import java.util.List;

import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;

public class TrajectoryParser {

	StateParser sp;
	Domain d;
	
	public TrajectoryParser(Domain d, StateParser sp){
		this.sp = sp;
		this.d = d;
	}
	
	
	
	//assumes that actions are ',' delineated on a single line and that states are line delineated
	public Trajectory getTrajectoryFromString(String sdata){
		
		List <GroundedAction> actions = new ArrayList<GroundedAction>();
		List<State> states = new ArrayList<State>();
		
		String [] dcomps = sdata.split("\n");
		String [] acomps = dcomps[0].split(",");
		
		//first get aciton set
		for(int i = 0; i < acomps.length; i++){
			String [] aparams = acomps[i].split(" ");
			String aname = aparams[0].trim();
			String [] params = new String[aparams.length-1];
			for(int j = 1; j < aparams.length; j++){
				params[j-1] = aparams[j].trim();
			}
			Action action = d.getAction(aname);
			GroundedAction ga = new GroundedAction(action, params);
			actions.add(ga);
		}
		
		//then get states
		for(int i = 1; i < dcomps.length; i++){
			State s = sp.stringToState(dcomps[i].trim());
			states.add(s);
		}
		
		
		return new Trajectory(states, actions);
	}
	
	
	public String getStringRepForTrajectory(Trajectory t){
		
		StringBuffer buf = new StringBuffer(1024);
		
		//write action list
		for(int i = 0; i < t.numStates()-1; i++){ //one less action than states
			if(i > 0){
				buf.append(",");
			}
			GroundedAction ga = t.getAction(i);
			buf.append(ga.action.getName());
			for(int j = 0; j < ga.params.length; j++){
				buf.append(" ").append(ga.params[j]);
			}
		}
		
		buf.append("\n");
		
		//write state list
		for(int i = 0; i < t.numStates(); i++){
			buf.append(sp.stateToString(t.getState(i)));
			buf.append("\n");
		}
		
		return buf.toString();
	}
	
}
