package commands.auxiliarytools.sokoban;

import java.util.ArrayList;
import java.util.List;

import burlap.oomdp.auxiliary.StateGenerator;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import domain.singleagent.sokoban.SokobanDomain;

public class SokoSimpleStateGen implements StateGenerator {

	SokobanDomain constructor;
	List<State> stateConfigs;
	public int stateConfig;
	
	
	public SokoSimpleStateGen(){
		constructor = new SokobanDomain();
		constructor.generateDomain();
		stateConfigs = new ArrayList<State>();
		stateConfig = 0;
		this.initStates();
	}
	
	
	
	private void initStates(){
		
		//first mode
		
		State st = constructor.getCleanStateNBlocks(1);
		
		ObjectInstance room = st.getObject(SokobanDomain.ROOMCLASS + 0);
		room.setValue(SokobanDomain.TOPXATTNAME, 0);
		room.setValue(SokobanDomain.TOPYATTNAME, 8);
		room.setValue(SokobanDomain.BOTTOMXATTNAME, 4);
		room.setValue(SokobanDomain.BOTTOMYATTNAME, 4);
		room.setValue(SokobanDomain.COLORATTNAME, "green");

		ObjectInstance room2 = st.getObject(SokobanDomain.ROOMCLASS + 1);
		room2.setValue(SokobanDomain.TOPXATTNAME, 4);
		room2.setValue(SokobanDomain.TOPYATTNAME, 8);
		room2.setValue(SokobanDomain.BOTTOMXATTNAME, 8);
		room2.setValue(SokobanDomain.BOTTOMYATTNAME, 4);
		room2.setValue(SokobanDomain.COLORATTNAME, "blue");

		ObjectInstance room3 = st.getObject(SokobanDomain.ROOMCLASS + 2);
		room3.setValue(SokobanDomain.TOPXATTNAME, 0);
		room3.setValue(SokobanDomain.TOPYATTNAME, 4);
		room3.setValue(SokobanDomain.BOTTOMXATTNAME, 8);
		room3.setValue(SokobanDomain.BOTTOMYATTNAME, 0);
		room3.setValue(SokobanDomain.COLORATTNAME, "red");

		ObjectInstance door = st.getObject(SokobanDomain.DOORCLASS + 0);
		door.setValue(SokobanDomain.TOPXATTNAME, 2);
		door.setValue(SokobanDomain.TOPYATTNAME, 4);
		door.setValue(SokobanDomain.BOTTOMXATTNAME, 2);
		door.setValue(SokobanDomain.BOTTOMYATTNAME, 3);

		ObjectInstance door2 = st.getObject(SokobanDomain.DOORCLASS + 1);
		door2.setValue(SokobanDomain.TOPXATTNAME, 6);
		door2.setValue(SokobanDomain.TOPYATTNAME, 4);
		door2.setValue(SokobanDomain.BOTTOMXATTNAME, 6);
		door2.setValue(SokobanDomain.BOTTOMYATTNAME, 3);

	

		ObjectInstance agent = st.getObject(SokobanDomain.AGENTCLASS + 0);
		agent.setValue(SokobanDomain.XATTNAME, 6);
		agent.setValue(SokobanDomain.YATTNAME, 6);


		ObjectInstance block = st.getObject(SokobanDomain.BLOCKCLASS + 0);
		block.setValue(SokobanDomain.XATTNAME, 2);
		block.setValue(SokobanDomain.YATTNAME, 2);
		block.setValue(SokobanDomain.COLORATTNAME, "yellow");
		block.setValue(SokobanDomain.SHAPEATTNAME, "star");
		
		
		stateConfigs.add(st);
		
		
		
		
		//second version
		st = st.copy();
		
		room = st.getObject(SokobanDomain.ROOMCLASS + 0);
		room.setValue(SokobanDomain.COLORATTNAME, "red");
		
		room = st.getObject(SokobanDomain.ROOMCLASS + 1);
		room.setValue(SokobanDomain.COLORATTNAME, "green");
		
		room = st.getObject(SokobanDomain.ROOMCLASS + 2);
		room.setValue(SokobanDomain.COLORATTNAME, "blue");
		
		stateConfigs.add(st);
		
		
		//third version
		st = st.copy();
		
		block = st.getObject(SokobanDomain.BLOCKCLASS + 0);
		block.setValue(SokobanDomain.XATTNAME, 2);
		block.setValue(SokobanDomain.YATTNAME, 6);
		
		stateConfigs.add(st);
		
		
		//fourth version
		st = st.copy();
		
		block = st.getObject(SokobanDomain.BLOCKCLASS + 0);
		block.setValue(SokobanDomain.XATTNAME, 2);
		block.setValue(SokobanDomain.YATTNAME, 2);
		
		agent = st.getObject(SokobanDomain.AGENTCLASS + 0);
		agent.setValue(SokobanDomain.XATTNAME, 2);
		agent.setValue(SokobanDomain.YATTNAME, 6);
		
		stateConfigs.add(st);
		
		
	}
	
	
	@Override
	public State generateState() {
		
		
		State st = stateConfigs.get(stateConfig);
		
		
		SokobanDomain.createMap(st);
		
		return st;
	}

}
