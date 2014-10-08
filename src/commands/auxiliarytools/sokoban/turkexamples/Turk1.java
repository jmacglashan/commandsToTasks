package commands.auxiliarytools.sokoban.turkexamples;

import burlap.oomdp.auxiliary.StateGenerator;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import domain.singleagent.sokoban.SokobanDomain;

public class Turk1 implements StateGenerator {

	@Override
	public State generateState() {
		
		State st = TurkRoomLayout.getRoomLayout(1);
		
		ObjectInstance agent = st.getObject(SokobanDomain.AGENTCLASS + 0);
		agent.setValue(SokobanDomain.XATTNAME, 8);
		agent.setValue(SokobanDomain.YATTNAME, 6);


		ObjectInstance block = st.getObject(SokobanDomain.BLOCKCLASS + 0);
		block.setValue(SokobanDomain.XATTNAME, 2);
		block.setValue(SokobanDomain.YATTNAME, 2);
		block.setValue(SokobanDomain.COLORATTNAME, "yellow");
		block.setValue(SokobanDomain.SHAPEATTNAME, "star");
		
		
		SokobanDomain.createMap(st);
		
		return st;
	}

}
