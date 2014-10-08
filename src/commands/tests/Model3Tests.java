package commands.tests;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import generativemodel.GMQueryResult;
import generativemodel.GenerativeModel;
import generativemodel.RVariable;
import generativemodel.RVariableValue;
import burlap.behavior.statehashing.NameDependentStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.GroundedProp;
import burlap.oomdp.core.State;

import commands.model3.TaskModule;
import commands.model3.TaskModule.LiftedVarValue;
import commands.model3.TaskModule.StateRVValue;

import domain.singleagent.sokoban2.Sokoban2Domain;

public class Model3Tests {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		Sokoban2Domain dg = new Sokoban2Domain();
		Domain domain = dg.generateDomain();
		State s = Sokoban2Domain.getClassicState(domain);
		
		TaskModule ta = new TaskModule("task", domain);
		
		GenerativeModel gm = new GenerativeModel();
		gm.addGMModule(ta);
		RVariable liftedVar = gm.getRVarWithName(TaskModule.LIFTEDRFNAME);
		
		LiftedVarValue aToR = new LiftedVarValue(liftedVar);
		aToR.addProp(new GroundedProp(domain.getPropFunction(Sokoban2Domain.PFAGENTINROOM), new String[]{"a", "r"}));
		
		LiftedVarValue bToR = new LiftedVarValue(liftedVar);
		bToR.addProp(new GroundedProp(domain.getPropFunction(Sokoban2Domain.PFBLOCKINROOM), new String[]{"b", "r"}));
		
		ta.addLiftedVarValue(aToR);
		ta.addLiftedVarValue(bToR);
		
		StateHashFactory hashingFactory = new NameDependentStateHashFactory();
		
		StateRVValue sval = new StateRVValue(s, hashingFactory, gm.getRVarWithName(TaskModule.STATENAME));
		
		
		List<RVariableValue> sconds = new ArrayList<RVariableValue>(1);
		sconds.add(sval);
		Iterator<GMQueryResult> lrIter = ta.getNonZeroProbIterator(liftedVar, sconds);
		while(lrIter.hasNext()){
			GMQueryResult lrRes = lrIter.next();
			System.out.println(lrRes.probability + ": " + lrRes.getSingleQueryVar().toString());
			
			List<RVariableValue> lrConds = new ArrayList<RVariableValue>(2);
			lrConds.add(sval);
			lrConds.add(lrRes.getSingleQueryVar());
			Iterator<GMQueryResult> grIter = ta.getNonZeroProbIterator(gm.getRVarWithName(TaskModule.GROUNDEDRFNAME), lrConds);
			while(grIter.hasNext()){
				GMQueryResult grRes = grIter.next();
				System.out.println("\t" + grRes.probability + ": " + grRes.getSingleQueryVar().toString());
				
				List<RVariableValue> grConds = new ArrayList<RVariableValue>(3);
				grConds.add(sval);
				grConds.add(lrRes.getSingleQueryVar());
				grConds.add(grRes.getSingleQueryVar());
				Iterator<GMQueryResult> bIter = ta.getNonZeroProbIterator(gm.getRVarWithName(TaskModule.BINDINGNAME), grConds);
				while(bIter.hasNext()){
					GMQueryResult bRes = bIter.next();
					System.out.println("\t\t" + bRes.probability + ": " + bRes.getSingleQueryVar().toString());
				}
			}
		}
		

	}

}
