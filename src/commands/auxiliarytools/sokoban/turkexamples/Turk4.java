package commands.auxiliarytools.sokoban.turkexamples;

import burlap.oomdp.auxiliary.StateGenerator;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import domain.singleagent.sokoban.SokobanDomain;

public class Turk4 implements StateGenerator {

	@Override
	public State generateState() {
		
		State st = TurkRoomLayout.getRoomLayout(0);
		
		ObjectInstance agent = st.getObject(SokobanDomain.AGENTCLASS + 0);
		agent.setValue(SokobanDomain.XATTNAME, 8);
		agent.setValue(SokobanDomain.YATTNAME, 6);

		
		
		SokobanDomain.createMap(st);
		
		return st;
		
	}

}
