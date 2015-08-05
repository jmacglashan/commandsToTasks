package behavior.planning.sokoamdp;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.behavior.singleagent.planning.deterministic.GoalConditionTF;
import burlap.behavior.singleagent.planning.deterministic.SDPlannerPolicy;
import burlap.behavior.singleagent.planning.deterministic.uninformed.bfs.BFS;
import burlap.behavior.statehashing.NameDependentStateHashFactory;
import burlap.oomdp.auxiliary.DomainGenerator;
import burlap.oomdp.core.*;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.common.UniformCostRF;
import domain.singleagent.sokoban2.Sokoban2Domain;

import java.util.List;

/**
 * @author James MacGlashan.
 */
public class SokoAMDP implements DomainGenerator {

	public static final String ATTCONNECTED = "connectedObjects";
	public static final String ATTINREGION = "inRegion";

	public static final String ACTIONTODOOR = "gotoDoor";
	public static final String ACTIONTOROOM = "gotoRoom";


	@Override
	public Domain generateDomain() {

		SADomain domain = new SADomain();

		Attribute conn = new Attribute(domain, ATTCONNECTED, Attribute.AttributeType.MULTITARGETRELATIONAL);
		Attribute inRegion = new Attribute(domain, ATTINREGION, Attribute.AttributeType.RELATIONAL);


		ObjectClass agent = new ObjectClass(domain, Sokoban2Domain.CLASSAGENT);
		agent.addAttribute(inRegion);

		ObjectClass room = new ObjectClass(domain, Sokoban2Domain.CLASSROOM);
		room.addAttribute(conn);

		ObjectClass door = new ObjectClass(domain, Sokoban2Domain.CLASSDOOR);
		door.addAttribute(conn);

		new GotoRegion(ACTIONTODOOR, domain, Sokoban2Domain.CLASSDOOR);
		new GotoRegion(ACTIONTOROOM, domain, Sokoban2Domain.CLASSROOM);


		return domain;
	}

	public static State projectToAMDPState(State s, Domain aDomain){

		State as = new State();

		ObjectInstance aagent = new ObjectInstance(aDomain.getObjectClass(Sokoban2Domain.CLASSAGENT), Sokoban2Domain.CLASSAGENT);
		as.addObject(aagent);


		List<ObjectInstance> rooms = s.getObjectsOfClass(Sokoban2Domain.CLASSROOM);
		for(ObjectInstance r : rooms){
			ObjectInstance ar = new ObjectInstance(aDomain.getObjectClass(Sokoban2Domain.CLASSROOM), r.getName());
			as.addObject(ar);
		}

		List<ObjectInstance> doors = s.getObjectsOfClass(Sokoban2Domain.CLASSDOOR);
		for(ObjectInstance d : doors){
			ObjectInstance dr = new ObjectInstance(aDomain.getObjectClass(Sokoban2Domain.CLASSDOOR), d.getName());
			as.addObject(dr);
		}


		//set agent position
		//first try room
		ObjectInstance agent = s.getFirstObjectOfClass(Sokoban2Domain.CLASSAGENT);
		int ax = agent.getIntValForAttribute(Sokoban2Domain.ATTX);
		int ay = agent.getIntValForAttribute(Sokoban2Domain.ATTY);
		ObjectInstance inRoom = Sokoban2Domain.roomContainingPoint(s, ax, ay);

		if(inRoom != null){
			aagent.setValue(ATTINREGION, inRoom.getName());
		}
		else{
			ObjectInstance inDoor = Sokoban2Domain.doorContainingPoint(s, ax, ay);
			aagent.setValue(ATTINREGION, inDoor.getName());
		}


		//now set room and door connections
		for(ObjectInstance r : rooms){

			int rt = r.getIntValForAttribute(Sokoban2Domain.ATTTOP);
			int rl = r.getIntValForAttribute(Sokoban2Domain.ATTLEFT);
			int rb = r.getIntValForAttribute(Sokoban2Domain.ATTBOTTOM);
			int rr = r.getIntValForAttribute(Sokoban2Domain.ATTRIGHT);

			ObjectInstance ar = as.getObject(r.getName());

			for(ObjectInstance d : doors){

				int dt = d.getIntValForAttribute(Sokoban2Domain.ATTTOP);
				int dl = d.getIntValForAttribute(Sokoban2Domain.ATTLEFT);
				int db = d.getIntValForAttribute(Sokoban2Domain.ATTBOTTOM);
				int dr = d.getIntValForAttribute(Sokoban2Domain.ATTRIGHT);

				if(rectanglesIntersect(rt, rl, rb, rr, dt, dl, db, dr)){
					ObjectInstance ad = as.getObject(d.getName());
					ar.addRelationalTarget(ATTCONNECTED, ad.getName());
					ad.addRelationalTarget(ATTCONNECTED, ar.getName());
				}

			}

		}


		return as;

	}


	protected static boolean rectanglesIntersect(int t1, int l1, int b1, int r1, int t2, int l2, int b2, int r2){

		return t2 >= b1 && b2 <= t1 && r2 >= l1 && l2 <= r1;

	}




	public static class GotoRegion extends Action {

		public GotoRegion(String name, Domain domain, String obClass){
			super(name, domain, obClass);
		}


		@Override
		public boolean applicableInState(State s, String[] params) {

			//get the region where the agent currently is
			ObjectInstance agent = s.getFirstObjectOfClass(Sokoban2Domain.CLASSAGENT);
			ObjectInstance curRegion = s.getObject(agent.getStringValForAttribute(ATTINREGION));

			//is the param connected to this region?
			if(curRegion.getAllRelationalTargets(ATTCONNECTED).contains(params[0])){
				return true;
			}

			return false;
		}

		@Override
		protected State performActionHelper(State s, String[] params) {
			ObjectInstance agent = s.getFirstObjectOfClass(Sokoban2Domain.CLASSAGENT);
			agent.addRelationalTarget(ATTINREGION, params[0]);
			return s;
		}

		@Override
		public List<TransitionProbability> getTransitions(State s, String[] params) {
			return this.deterministicTransition(s, params);
		}
	}



	public static class InRegionGC implements StateConditionTest {

		String roomName;

		public InRegionGC(String roomName){
			this.roomName = roomName;
		}

		@Override
		public boolean satisfies(State s) {
			ObjectInstance agent = s.getFirstObjectOfClass(Sokoban2Domain.CLASSAGENT);
			String curRoom = agent.getStringValForAttribute(ATTINREGION);
			if(curRoom.equals(this.roomName)){
				return true;
			}
			return false;
		}
	}


	public static void main(String [] args){

		Sokoban2Domain soko = new Sokoban2Domain();
		Domain domain = soko.generateDomain();

		State s = Sokoban2Domain.getClassicState(domain);

		SokoAMDP asoko = new SokoAMDP();
		Domain adomain = asoko.generateDomain();

		State as = SokoAMDP.projectToAMDPState(s, adomain);


		//TerminalExplorer exp = new TerminalExplorer(adomain);
		//exp.exploreFromState(as);

		StateConditionTest gc = new InRegionGC("room1");
		BFS bfs = new BFS(adomain, gc, new NameDependentStateHashFactory());
		bfs.planFromState(as);

		Policy p = new SDPlannerPolicy(bfs);
		EpisodeAnalysis ea = p.evaluateBehavior(as, new UniformCostRF(), new GoalConditionTF(gc), 100);
		System.out.println(ea.getActionSequenceString("\n"));



	}




}
