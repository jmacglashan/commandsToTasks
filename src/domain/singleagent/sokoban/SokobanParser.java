package domain.singleagent.sokoban;


import burlap.oomdp.core.states.State;
import burlap.oomdp.legacy.StateParser;

public class SokobanParser implements StateParser {

	SokobanDomain constructor;
	
	public SokobanParser(){
		constructor = new SokobanDomain();
	}
	
	@Override
	public String stateToString(State s) {
		return constructor.stateToString(s);
	}

	@Override
	public State stringToState(String str) {
		return constructor.stringToState(str);
	}

}
