package domain.singleagent.sokoban;

import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.State;

public class SokobanParser implements StateParser{

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
