package behavior.planning.sokoamdp;

import burlap.behavior.policy.Policy;
import burlap.behavior.singleagent.EpisodeAnalysis;

import burlap.behavior.singleagent.planning.deterministic.DDPlannerPolicy;
import burlap.behavior.singleagent.planning.deterministic.DeterministicPlanner;

import burlap.behavior.singleagent.planning.deterministic.SDPlannerPolicy;

import burlap.behavior.singleagent.planning.deterministic.uninformed.bfs.BFS;

import burlap.debugtools.DPrint;
import burlap.oomdp.auxiliary.common.GoalConditionTF;
import burlap.oomdp.auxiliary.stateconditiontest.StateConditionTest;
import burlap.oomdp.auxiliary.stateconditiontest.TFGoalCondition;
import burlap.oomdp.core.*;
import burlap.oomdp.core.objects.ObjectInstance;
import burlap.oomdp.core.states.State;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.ObjectParameterizedAction;
import burlap.oomdp.singleagent.common.UniformCostRF;
import burlap.oomdp.statehashing.HashableState;
import burlap.oomdp.statehashing.HashableStateFactory;
import burlap.oomdp.statehashing.SimpleHashableStateFactory;
import commands.model3.TrajectoryModule;
import domain.singleagent.sokoban2.Sokoban2Domain;

import java.util.ArrayList;
import java.util.List;

/**
 * @author James MacGlashan.
 */
public class SokoAMDPPlanner extends DeterministicPlanner {

	protected Domain adomain;
	protected SokoAStarPlanner astarPlanner;
	protected String blockName;
	protected String agentName;

	public SokoAMDPPlanner(Domain domain, Domain adomain, TerminalFunction tf, HashableStateFactory hashingFactory){

		if(!(tf instanceof TrajectoryModule.ConjunctiveGroundedPropTF)){
			throw new RuntimeException("Error; terminal function is not correct type.");
		}


		this.adomain = adomain;
		this.deterministicPlannerInit(domain, new UniformCostRF(), tf, new TFGoalCondition(tf), hashingFactory);

		this.astarPlanner = new SokoAStarPlanner();

	}

	@Override
	public Policy planFromState(State initialState) {

		TrajectoryModule.ConjunctiveGroundedPropTF ctf = (TrajectoryModule.ConjunctiveGroundedPropTF)tf;
		GroundedProp gp = ctf.gps.get(0);

		this.agentName = initialState.getFirstObjectOfClass(Sokoban2Domain.CLASSAGENT).getName();

		if(gp.pf.getName().equals(Sokoban2Domain.PFAGENTINROOM)){

			State as = SokoAMDP.projectToAMDPState(initialState, this.adomain);
			StateConditionTest asg = new SokoAMDP.InRegionGC(gp.params[1]);
			BFS bfs = new BFS(this.adomain, asg, new SimpleHashableStateFactory(false));
			DPrint.toggleCode(bfs.getDebugCode(), false);
			bfs.planFromState(as);
			SDPlannerPolicy policy = new SDPlannerPolicy(bfs);
			EpisodeAnalysis ea = policy.evaluateBehavior(as, new UniformCostRF(), new GoalConditionTF(asg));
			this.decompose(initialState, ea, true);


		}
		else{

			//first determine the room in which the block resides
			this.blockName = gp.params[0];
			ObjectInstance block = initialState.getObject(this.blockName);
			int bx = block.getIntValForAttribute(Sokoban2Domain.ATTX);
			int by = block.getIntValForAttribute(Sokoban2Domain.ATTY);
			ObjectInstance containingRegion = Sokoban2Domain.roomContainingPoint(initialState, bx, by);
			if(containingRegion == null){
				containingRegion = Sokoban2Domain.doorContainingPoint(initialState, bx, by);
			}
			String initialBlockRegionName = containingRegion.getName();

			State curState = initialState;

			//next set up planner for getting to block room and decompose
			State as = SokoAMDP.projectToAMDPState(curState, this.adomain);
			StateConditionTest asg = new SokoAMDP.InRegionGC(initialBlockRegionName);
			BFS ibfs = new BFS(this.adomain, asg, new SimpleHashableStateFactory(false));
			DPrint.toggleCode(ibfs.getDebugCode(), false);
			ibfs.planFromState(as);
			SDPlannerPolicy ipolicy = new SDPlannerPolicy(ibfs);
			EpisodeAnalysis iea = ipolicy.evaluateBehavior(as, new UniformCostRF(), new GoalConditionTF(asg));
			curState = this.decompose(curState, iea, true);

			//now do block movement to goal region planning
			as = SokoAMDP.projectToAMDPState(curState, this.adomain);
			asg = new SokoAMDP.InRegionGC(gp.params[1]);
			BFS fbfs = new BFS(this.adomain, asg, new SimpleHashableStateFactory(false));
			fbfs.planFromState(as);
			SDPlannerPolicy fpolicy = new SDPlannerPolicy(fbfs);
			EpisodeAnalysis fea = fpolicy.evaluateBehavior(as, new UniformCostRF(), new GoalConditionTF(asg));
			this.decompose(curState, fea, false);



		}

		return new DDPlannerPolicy(this);


	}

	protected State decompose(State curState, EpisodeAnalysis ea, boolean agentToRegion){

		for(int t = 0; t < ea.numTimeSteps()-1; t++){

			ObjectParameterizedAction.ObjectParameterizedGroundedAction aAction = (ObjectParameterizedAction.ObjectParameterizedGroundedAction)ea.getAction(t);
			TrajectoryModule.ConjunctiveGroundedPropTF subgoalTF;
			if(aAction.actionName().equals(SokoAMDP.ACTIONTODOOR)){
				List<GroundedProp> subgoalGPs = new ArrayList<GroundedProp>(1);

				if(agentToRegion){
					subgoalGPs.add(new GroundedProp(this.domain.getPropFunction(Sokoban2Domain.PFAGENTINDOOR),
							new String[]{agentName, aAction.params[0]}));
				}
				else{
					subgoalGPs.add(new GroundedProp(this.domain.getPropFunction(Sokoban2Domain.PFBLOCKINDOOR),
							new String[]{blockName, aAction.params[0]}));
				}

				subgoalTF = new TrajectoryModule.ConjunctiveGroundedPropTF(subgoalGPs);
			}
			else{

				List<GroundedProp> subgoalGPs = new ArrayList<GroundedProp>(1);

				if(agentToRegion){
					subgoalGPs.add(new GroundedProp(this.domain.getPropFunction(Sokoban2Domain.PFAGENTINROOM),
							new String[]{agentName, aAction.params[0]}));
				}
				else{
					subgoalGPs.add(new GroundedProp(this.domain.getPropFunction(Sokoban2Domain.PFBLOCKINROOM),
							new String[]{blockName, aAction.params[0]}));
				}

				subgoalTF = new TrajectoryModule.ConjunctiveGroundedPropTF(subgoalGPs);

			}

			Policy p = this.astarPlanner.getPolicy(this.domain, curState, this.rf, subgoalTF, this.hashingFactory);
			EpisodeAnalysis lowDecomp = p.evaluateBehavior(curState, this.rf, subgoalTF);
			for(int t2 = 0; t2 < lowDecomp.numTimeSteps()-1; t2++){
				State ls = lowDecomp.getState(t2);
				HashableState lsh = this.hashingFactory.hashState(ls);
				this.internalPolicy.put(lsh, lowDecomp.getAction(t2));
				this.mapToStateIndex.put(lsh,lsh);
			}
			curState = lowDecomp.getState(lowDecomp.numTimeSteps()-1);


		}

		return curState;

	}


}
