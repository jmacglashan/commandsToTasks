package commands.auxiliarytools.sokoban.soko2turkexamples;

import java.util.ArrayList;
import java.util.List;

import domain.singleagent.sokoban2.Sokoban2Domain;

import burlap.oomdp.auxiliary.StateGenerator;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;


/**
 * Generator for 3 examples of "push the yellow block to the green room"
 * @author James MacGlashan
 *
 */
public class Soko2Turk1 implements StateGenerator {

	protected List<State>	initialStates;
	protected int 			index = 0;
	
	public Soko2Turk1(Domain domain){
		
		this.initialStates = new ArrayList<State>(3);
		
		State s = Sokoban2Domain.getCleanState(domain, 3, 2, 1);
		Sokoban2Domain.setRoom(s, 0, 4, 0, 0, 8, "red");
		Sokoban2Domain.setRoom(s, 1, 8, 0, 4, 4, "green");
		Sokoban2Domain.setRoom(s, 2, 8, 4, 4, 8, "blue");
		
		Sokoban2Domain.setDoor(s, 0, 4, 6, 4, 6);
		Sokoban2Domain.setDoor(s, 1, 4, 2, 4, 2);
		
		Sokoban2Domain.setAgent(s, 6, 6);
		Sokoban2Domain.setBlock(s, 0, 2, 2, "backpack", "yellow");
		
		this.initialStates.add(s);
		
		
		s = Sokoban2Domain.getCleanState(domain, 3, 2, 1);
		Sokoban2Domain.setRoom(s, 0, 4, 0, 0, 8, "green");
		Sokoban2Domain.setRoom(s, 1, 8, 0, 4, 4, "blue");
		Sokoban2Domain.setRoom(s, 2, 8, 4, 4, 8, "red");
		
		Sokoban2Domain.setDoor(s, 0, 4, 6, 4, 6);
		Sokoban2Domain.setDoor(s, 1, 4, 2, 4, 2);
		
		Sokoban2Domain.setAgent(s, 6, 6);
		Sokoban2Domain.setBlock(s, 0, 2, 6, "backpack", "yellow");
		
		this.initialStates.add(s);
		
		
		s = Sokoban2Domain.getCleanState(domain, 3, 2, 2);
		Sokoban2Domain.setRoom(s, 0, 4, 0, 0, 8, "red");
		Sokoban2Domain.setRoom(s, 1, 8, 0, 4, 4, "blue");
		Sokoban2Domain.setRoom(s, 2, 8, 4, 4, 8, "green");
		
		Sokoban2Domain.setDoor(s, 0, 4, 6, 4, 6);
		Sokoban2Domain.setDoor(s, 1, 4, 2, 4, 2);
		
		Sokoban2Domain.setAgent(s, 6, 6);
		Sokoban2Domain.setBlock(s, 0, 2, 2, "backpack", "yellow");
		Sokoban2Domain.setBlock(s, 1, 2, 6, "chair", "red");
		
		this.initialStates.add(s);
		
		
		
		
		
	}
	
	@Override
	public State generateState() {
		
		State s = this.initialStates.get(this.index);
		
		this.index = (this.index + 1) % this.initialStates.size();
		
		return s;
	}

}
