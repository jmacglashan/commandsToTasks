package domain.singleagent.sokoban2;

import domain.singleagent.sokoban.SokobanDomain;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;

public class SokobanOldToNewParser extends Sokoban2Parser {

	public SokobanOldToNewParser(Domain domain) {
		super(domain);
	}
	
	@Override
	public State stringToState(String str) {
		
		State s = new State();
		int rooms = 0;
		int doors = 0;
		int blocks = 0;
		int agents = 0;
		String[] objects = str.split(" ");
		for (int i = 0; i < objects.length; i++){
			String[] splitobject = objects[i].split(",");
			if (splitobject[0].equals("room")){
				ObjectInstance room = new ObjectInstance(this.domain.getObjectClass(Sokoban2Domain.CLASSROOM), Sokoban2Domain.CLASSROOM + rooms);
				String colName = SokobanDomain.colors.get(Integer.parseInt(splitobject[1]));
				room.setValue(Sokoban2Domain.ATTCOLOR, colName);
				room.setValue(Sokoban2Domain.ATTLEFT, Integer.parseInt(splitobject[2]));
				room.setValue(Sokoban2Domain.ATTTOP, Integer.parseInt(splitobject[3]));
				room.setValue(Sokoban2Domain.ATTRIGHT, Integer.parseInt(splitobject[4]));
				room.setValue(Sokoban2Domain.ATTBOTTOM, Integer.parseInt(splitobject[5]));
				s.addObject(room);
				rooms++;
			}
			else if (splitobject[0].equals("door")){
				ObjectInstance door = new ObjectInstance(this.domain.getObjectClass(Sokoban2Domain.CLASSDOOR), Sokoban2Domain.CLASSDOOR + doors);
				door.setValue(Sokoban2Domain.ATTLEFT, Integer.parseInt(splitobject[1]));
				door.setValue(Sokoban2Domain.ATTTOP, Integer.parseInt(splitobject[2]));
				door.setValue(Sokoban2Domain.ATTRIGHT, Integer.parseInt(splitobject[3]));
				door.setValue(Sokoban2Domain.ATTBOTTOM, Integer.parseInt(splitobject[4]));
				s.addObject(door);
				doors++;
			}
			else if (splitobject[0].equals("block")){
				ObjectInstance block = new ObjectInstance(this.domain.getObjectClass(Sokoban2Domain.CLASSBLOCK), Sokoban2Domain.CLASSBLOCK + blocks);
				
				String colName = SokobanDomain.colors.get(Integer.parseInt(splitobject[1]));
				//String shapeName = SokobanDomain.shapes.get(Integer.parseInt(splitobject[2])); shapes are now unique between versions so use indexing instead
				block.setValue(Sokoban2Domain.ATTCOLOR, colName);
				block.setValue(Sokoban2Domain.ATTSHAPE, Integer.parseInt(splitobject[2]));
				block.setValue(Sokoban2Domain.ATTX, Integer.parseInt(splitobject[3]));
				block.setValue(Sokoban2Domain.ATTY, Integer.parseInt(splitobject[4]));
				s.addObject(block);
				blocks++;
			}
			else if (splitobject[0].equals("agent")){
				ObjectInstance agent = new ObjectInstance(this.domain.getObjectClass(Sokoban2Domain.CLASSAGENT), Sokoban2Domain.CLASSAGENT + agents);
				agent.setValue(Sokoban2Domain.ATTX, Integer.parseInt(splitobject[1].trim()));
				agent.setValue(Sokoban2Domain.ATTY, Integer.parseInt(splitobject[2].trim()));
				
				if(this.domain.getAttribute(Sokoban2Domain.ATTDIR) != null){
					agent.setValue(Sokoban2Domain.ATTDIR, "south");
				}
				
				s.addObject(agent);
				agents++;
			}
		}
		
		return s;
		
	}

}
