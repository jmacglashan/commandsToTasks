package domain.singleagent.sokoban2;

import java.util.List;

import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;

public class Sokoban2Parser implements StateParser {

	protected Domain		domain;
	
	public Sokoban2Parser(Domain domain) {
		this.domain = domain;
	}
	
	@Override
	public String stateToString(State s) {
		
		StringBuffer buf = new StringBuffer();
		List<ObjectInstance> rooms = s.getObjectsOfTrueClass(Sokoban2Domain.CLASSROOM);
		List<ObjectInstance> doors = s.getObjectsOfTrueClass(Sokoban2Domain.CLASSDOOR);
		List<ObjectInstance> blocks = s.getObjectsOfTrueClass(Sokoban2Domain.CLASSBLOCK);
		List<ObjectInstance> agents = s.getObjectsOfTrueClass(Sokoban2Domain.CLASSAGENT);

		for(ObjectInstance o : rooms){
			buf.append("room,");
			buf.append(o.getDiscValForAttribute(Sokoban2Domain.ATTCOLOR)).append(",");
			buf.append(o.getDiscValForAttribute(Sokoban2Domain.ATTLEFT)).append(",");
			buf.append(o.getDiscValForAttribute(Sokoban2Domain.ATTTOP)).append(",");
			buf.append(o.getDiscValForAttribute(Sokoban2Domain.ATTRIGHT)).append(",");
			buf.append(o.getDiscValForAttribute(Sokoban2Domain.ATTBOTTOM)).append(" ");
		}
		for(ObjectInstance o : doors){
			buf.append("door,");
			buf.append(o.getDiscValForAttribute(Sokoban2Domain.ATTLEFT)).append(",");
			buf.append(o.getDiscValForAttribute(Sokoban2Domain.ATTTOP)).append(",");
			buf.append(o.getDiscValForAttribute(Sokoban2Domain.ATTRIGHT)).append(",");
			buf.append(o.getDiscValForAttribute(Sokoban2Domain.ATTBOTTOM)).append(" ");
		}
		for(ObjectInstance o : blocks){
			buf.append("block,");
			buf.append(o.getDiscValForAttribute(Sokoban2Domain.ATTCOLOR)).append(",");
			buf.append(o.getDiscValForAttribute(Sokoban2Domain.ATTSHAPE)).append(",");
			buf.append(o.getDiscValForAttribute(Sokoban2Domain.ATTX)).append(",");
			buf.append(o.getDiscValForAttribute(Sokoban2Domain.ATTY)).append(" ");
		}
		boolean first = true;
		for(ObjectInstance o : agents){
			if(!first){
				buf.append(" ");
			}
			buf.append("agent,");
			buf.append(o.getDiscValForAttribute(Sokoban2Domain.ATTX)).append(",");
			buf.append(o.getDiscValForAttribute(Sokoban2Domain.ATTY));
			first = false;
		}
		
		return buf.toString();
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
				room.setValue(Sokoban2Domain.ATTCOLOR, Sokoban2Domain.COLORS[Integer.parseInt(splitobject[1])]);
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
				block.setValue(Sokoban2Domain.ATTCOLOR, Sokoban2Domain.COLORS[Integer.parseInt(splitobject[1])]);
				block.setValue(Sokoban2Domain.ATTSHAPE, Sokoban2Domain.SHAPES[Integer.parseInt(splitobject[2])]);
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
