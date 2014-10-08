package commands.auxiliarytools.sokoban.turkexamples;

import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import domain.singleagent.sokoban.SokobanDomain;

public class TurkRoomLayout {

	public static State getRoomLayout(int nb){
		
		SokobanDomain constructor = new SokobanDomain();
		constructor.generateDomain();
		
		State st = constructor.getCleanStateNBlocksNDoors(nb, 3);
		
		ObjectInstance room = st.getObject(SokobanDomain.ROOMCLASS + 0);
		room.setValue(SokobanDomain.TOPXATTNAME, 0);
		room.setValue(SokobanDomain.TOPYATTNAME, 10);
		room.setValue(SokobanDomain.BOTTOMXATTNAME, 6);
		room.setValue(SokobanDomain.BOTTOMYATTNAME, 4);
		room.setValue(SokobanDomain.COLORATTNAME, "green");

		ObjectInstance room2 = st.getObject(SokobanDomain.ROOMCLASS + 1);
		room2.setValue(SokobanDomain.TOPXATTNAME, 6);
		room2.setValue(SokobanDomain.TOPYATTNAME, 10);
		room2.setValue(SokobanDomain.BOTTOMXATTNAME, 10);
		room2.setValue(SokobanDomain.BOTTOMYATTNAME, 4);
		room2.setValue(SokobanDomain.COLORATTNAME, "red");

		ObjectInstance room3 = st.getObject(SokobanDomain.ROOMCLASS + 2);
		room3.setValue(SokobanDomain.TOPXATTNAME, 0);
		room3.setValue(SokobanDomain.TOPYATTNAME, 4);
		room3.setValue(SokobanDomain.BOTTOMXATTNAME, 10);
		room3.setValue(SokobanDomain.BOTTOMYATTNAME, 0);
		room3.setValue(SokobanDomain.COLORATTNAME, "blue");

		
		
		
		
		ObjectInstance door = st.getObject(SokobanDomain.DOORCLASS + 0);
		door.setValue(SokobanDomain.TOPXATTNAME, 3);
		door.setValue(SokobanDomain.TOPYATTNAME, 4);
		door.setValue(SokobanDomain.BOTTOMXATTNAME, 3);
		door.setValue(SokobanDomain.BOTTOMYATTNAME, 3);

		ObjectInstance door2 = st.getObject(SokobanDomain.DOORCLASS + 1);
		door2.setValue(SokobanDomain.TOPXATTNAME, 8);
		door2.setValue(SokobanDomain.TOPYATTNAME, 4);
		door2.setValue(SokobanDomain.BOTTOMXATTNAME, 8);
		door2.setValue(SokobanDomain.BOTTOMYATTNAME, 3);
		
		ObjectInstance door3 = st.getObject(SokobanDomain.DOORCLASS + 2);
		door3.setValue(SokobanDomain.TOPXATTNAME, 5);
		door3.setValue(SokobanDomain.TOPYATTNAME, 6);
		door3.setValue(SokobanDomain.BOTTOMXATTNAME, 6);
		door3.setValue(SokobanDomain.BOTTOMYATTNAME, 6);
		
		return st;
		
	}
	
}

