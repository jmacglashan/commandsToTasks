package behavior.planning.sokoamdp;

import behavior.planning.PolicyGenerator;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.planning.deterministic.DDPlannerPolicy;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.GroundedProp;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.common.UniformCostRF;
import commands.model3.TrajectoryModule;
import domain.singleagent.sokoban2.Sokoban2Domain;

import java.util.ArrayList;
import java.util.List;

/**
 * @author James MacGlashan.
 */
public class SokoAMDPPlannerPolicyGen implements PolicyGenerator {

	protected Domain adomain;


	public SokoAMDPPlannerPolicyGen(){
		SokoAMDP amdp = new SokoAMDP();
		this.adomain = amdp.generateDomain();
	}

	@Override
	public Policy getPolicy(Domain domain, State initialState, RewardFunction rf, TerminalFunction tf, StateHashFactory hashingFactory) {

		SokoAMDPPlanner planner = new SokoAMDPPlanner(domain, this.adomain, tf, hashingFactory);
		planner.planFromState(initialState);
		Policy p = new DDPlannerPolicy(planner);

		return p;
	}


	public static void main(String [] args){

		Sokoban2Domain soko = new Sokoban2Domain();
		soko.includePullAction(true);

		Domain domain = soko.generateDomain();

		State s = Sokoban2Domain.getClassicState(domain);

		List<GroundedProp> goalgp = new ArrayList<GroundedProp>(1);
		goalgp.add(new GroundedProp(domain.getPropFunction(Sokoban2Domain.PFBLOCKINROOM), new String[]{"block0", "room1"}));
		TerminalFunction goalTF = new TrajectoryModule.ConjunctiveGroundedPropTF(goalgp);

		SokoAMDPPlannerPolicyGen pgen = new SokoAMDPPlannerPolicyGen();

		Policy p = pgen.getPolicy(domain, s, new UniformCostRF(), goalTF, new DiscreteStateHashFactory());

		EpisodeAnalysis ea = p.evaluateBehavior(s, new UniformCostRF(), goalTF, 100);

		System.out.println(ea.getActionSequenceString("\n"));


	}

}
