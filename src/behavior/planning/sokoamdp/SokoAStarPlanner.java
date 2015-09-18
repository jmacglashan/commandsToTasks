package behavior.planning.sokoamdp;

import behavior.planning.PolicyGenerator;

import burlap.behavior.policy.Policy;
import burlap.behavior.singleagent.planning.deterministic.SDPlannerPolicy;

import burlap.behavior.singleagent.planning.deterministic.informed.Heuristic;
import burlap.behavior.singleagent.planning.deterministic.informed.astar.AStar;

import burlap.debugtools.DPrint;
import burlap.oomdp.auxiliary.stateconditiontest.TFGoalCondition;
import burlap.oomdp.core.*;
import burlap.oomdp.core.objects.ObjectInstance;
import burlap.oomdp.core.states.State;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.common.UniformCostRF;
import burlap.oomdp.statehashing.HashableStateFactory;
import commands.model3.TrajectoryModule;
import domain.singleagent.sokoban2.Sokoban2Domain;

/**
 * @author James MacGlashan.
 */
public class SokoAStarPlanner implements PolicyGenerator {


	@Override
	public Policy getPolicy(Domain domain, State initialState,
							RewardFunction rf, TerminalFunction tf,
							HashableStateFactory hashingFactory) {
		TFGoalCondition gc = new TFGoalCondition(tf);

		AStar planner = new AStar(domain, new UniformCostRF(), gc, hashingFactory, this.getHeuristic(tf));
		DPrint.toggleCode(planner.getDebugCode(), false);
		planner.planFromState(initialState);

		//Policy p = new DDPlannerPolicy(planner);
		Policy p = new SDPlannerPolicy(planner);

		return p;
	}

	protected Heuristic getHeuristic(TerminalFunction tf){

		if(!(tf instanceof TrajectoryModule.ConjunctiveGroundedPropTF)){
			throw new RuntimeException("Error; terminal function is not correct type.");
		}

		TrajectoryModule.ConjunctiveGroundedPropTF ctf = (TrajectoryModule.ConjunctiveGroundedPropTF)tf;
		GroundedProp gp = ctf.gps.get(0);
		if(gp.pf.getName().equals(Sokoban2Domain.PFAGENTINROOM)){
			return new ToRoomHeuristic(gp.params[1]);
		}
		else if(gp.pf.getName().equals(Sokoban2Domain.PFAGENTINDOOR)){
			return new ToRoomHeuristic(gp.params[1], 0);
		}
		else if(gp.pf.getName().equals(Sokoban2Domain.PFBLOCKINROOM)){
			return new BlockToRoomHeuristic(gp.params[0], gp.params[1]);
		}
		else if(gp.pf.getName().equals(Sokoban2Domain.PFBLOCKINDOOR)){
			return new BlockToRoomHeuristic(gp.params[0], gp.params[1]);
		}
//		else if(gp.pf.getName().equals(Sokoban2Domain.PFTOUCHINGBLOCK)){
//			return new ToBlockHeuristic(gp.params[1]);
//		}

		throw new RuntimeException("No heuristic for task defined with: " + gp.toString());
	}


	protected int manDistance(int x0, int y0, int x1, int y1){
		return Math.abs(x0-x1) + Math.abs(y0-y1);
	}


	/**
	 * Manhatten distance to a room or door.
	 * @param x
	 * @param y
	 * @param l
	 * @param r
	 * @param b
	 * @param t
	 * @param delta set to 1 for rooms because boundaries are walls which are not sufficient to be in room; 0 for doors
	 * @return
	 */
	protected int toRoomManDistance(int x, int y, int l, int r, int b, int t, int delta){
		int dist = 0;

		//use +1s because boundaries define wall, which is not sufficient to be in the room
		if(x <= l){
			dist += l-x + delta;
		}
		else if(x >= r){
			dist += x - r + delta;
		}

		if(y <= b){
			dist += b - y + delta;
		}
		else if(y >= t){
			dist += y - t + delta;
		}

		return dist;
	}


	public class ToRoomHeuristic implements Heuristic{

		String roomName;
		int delta = 1;

		public ToRoomHeuristic(String roomName){
			this.roomName = roomName;
		}

		public ToRoomHeuristic(String roomName, int delta){
			this.roomName = roomName;
			this.delta = delta;
		}

		@Override
		public double h(State s) {

			//get the agent
			ObjectInstance agent = s.getFirstObjectOfClass(Sokoban2Domain.CLASSAGENT);
			int ax = agent.getIntValForAttribute(Sokoban2Domain.ATTX);
			int ay = agent.getIntValForAttribute(Sokoban2Domain.ATTY);

			//get room
			ObjectInstance room = s.getObject(this.roomName);
			int l = room.getIntValForAttribute(Sokoban2Domain.ATTLEFT);
			int r = room.getIntValForAttribute(Sokoban2Domain.ATTRIGHT);
			int b = room.getIntValForAttribute(Sokoban2Domain.ATTBOTTOM);
			int t = room.getIntValForAttribute(Sokoban2Domain.ATTTOP);

			int dist = toRoomManDistance(ax, ay, l, r, b, t, this.delta);

			//make negative because of negative reward
			return -dist;
		}


	}


	public class BlockToRoomHeuristic implements Heuristic{

		protected String blockName;
		protected String roomName;

		protected int delta = 1;

		public BlockToRoomHeuristic(String blockName, String roomName){
			this.blockName = blockName;
			this.roomName = roomName;
		}

		public BlockToRoomHeuristic(String blockName, String roomName, int delta){
			this.blockName = blockName;
			this.roomName = roomName;
			this.delta = delta;
		}

		@Override
		public double h(State s) {

			//get the agent
			ObjectInstance agent = s.getFirstObjectOfClass(Sokoban2Domain.CLASSAGENT);
			int ax = agent.getIntValForAttribute(Sokoban2Domain.ATTX);
			int ay = agent.getIntValForAttribute(Sokoban2Domain.ATTY);

			//get room
			ObjectInstance room = s.getObject(this.roomName);
			int l = room.getIntValForAttribute(Sokoban2Domain.ATTLEFT);
			int r = room.getIntValForAttribute(Sokoban2Domain.ATTRIGHT);
			int b = room.getIntValForAttribute(Sokoban2Domain.ATTBOTTOM);
			int t = room.getIntValForAttribute(Sokoban2Domain.ATTTOP);

			//get the block
			ObjectInstance block = s.getObject(this.blockName);
			int bx = block.getIntValForAttribute(Sokoban2Domain.ATTX);
			int by = block.getIntValForAttribute(Sokoban2Domain.ATTY);

			int dist = manDistance(ax, ay, bx, by)-1; //need to be one step away from block to push it

			//and then block needs to be at room
			dist += toRoomManDistance(bx, by, l, r, b, t, this.delta);

			//make negative because of negative reward
			return -dist;
		}


	}

	public class ToBlockHeuristic implements Heuristic{

		protected String blockName;

		public ToBlockHeuristic(String blockName){this.blockName = blockName;}

		@Override
		public double h(State s) {
			//get the agent
			ObjectInstance agent = s.getFirstObjectOfClass(Sokoban2Domain.CLASSAGENT);
			int ax = agent.getIntValForAttribute(Sokoban2Domain.ATTX);
			int ay = agent.getIntValForAttribute(Sokoban2Domain.ATTY);

			//get the block
			ObjectInstance block = s.getObject(this.blockName);
			int bx = block.getIntValForAttribute(Sokoban2Domain.ATTX);
			int by = block.getIntValForAttribute(Sokoban2Domain.ATTY);

			int dist = manDistance(ax, ay, bx, by)-1; //need to be one step away from block to push it

			return -dist;

		}
	}

}
