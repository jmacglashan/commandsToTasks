package commands.model3.noisyor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import burlap.behavior.statehashing.StateHashFactory;

import commands.model3.Model3Controller;
import commands.model3.StringValue;
import commands.model3.TaskModule;
import commands.model3.TaskModule.StateRVValue;
import commands.model3.TrajectoryModule;
import commands.model3.TrajectoryModule.TrajectoryValue;
import commands.model3.bagofwords.BagOfWordsModule;
import commands.model3.noisyor.NoisyOr.TopicSetRVal;
import commands.model3.noisyor.NoisyOr.WordParam;

import burlap.datastructures.HashedAggregator;
import em.EMModule;
import generativemodel.GMQuery;
import generativemodel.GMQueryResult;
import generativemodel.RVariableValue;

public class NoisyOrEMModule extends EMModule {

	protected List<Map<TopicSetRVal, Double>>			jointTopicAndTrajectory;
	protected StateHashFactory							hashingFactory;
	
	protected HashedAggregator<String> 					jointWordCounts;
	protected HashedAggregator<String> 					singleTopicCounts;
	
	
	protected boolean									needsAggreatorReset = true;
	
	
	public NoisyOrEMModule(StateHashFactory hashingFactory){
		this.jointTopicAndTrajectory = new ArrayList<Map<TopicSetRVal,Double>>();
		this.hashingFactory = hashingFactory;
	}
	
	@Override
	public void runEStep(int dataInstanceId, List<RVariableValue> observables) {
		if(this.needsAggreatorReset){
			this.resetAggregators();
		}
		
		NoisyOr norMod = (NoisyOr)this.gm.getModuleWithName(Model3Controller.LANGMODNAME);
		Set<String> allNatWords = norMod.getNaturalWords();
		WordParam wp = norMod.getWp();
		
		TrajectoryValue trajectoryVal = null;
		StringValue naturalCommand = null;
		
		for(RVariableValue val : observables){
			if(val.getOwner().equals(this.gm.getRVarWithName(TrajectoryModule.TNAME))){
				trajectoryVal = (TrajectoryValue)val;
			}
			else if(val.getOwner().equals(this.gm.getRVarWithName(BagOfWordsModule.NNAME))){
				naturalCommand = (StringValue)val;
			}
		}
		
		if(trajectoryVal == null || naturalCommand == null){
			throw new RuntimeException("Cannot run EM because observables is incomplete.");
		}
		
		Map<TopicSetRVal, Double> jtt = null;
		
		if(this.jointTopicAndTrajectory.size() <= dataInstanceId){
			jtt = this.constructJointTopicAndTrajectoryEntry(dataInstanceId, trajectoryVal);
		}
		else{
			jtt = this.jointTopicAndTrajectory.get(dataInstanceId);
			if(jtt == null){
				jtt = this.constructJointTopicAndTrajectoryEntry(dataInstanceId, trajectoryVal);
			}
		}
		
		
		
		
		
		Set<String> natCommandSet = norMod.getWordSet(naturalCommand);
		
		HashedAggregator<String> tempJointCounts = new HashedAggregator<String>();
		HashedAggregator<String> tempTopicCounts = new HashedAggregator<String>();
		double probData = 0.;
		
		
		for(Map.Entry<TopicSetRVal, Double> jtte : jtt.entrySet()){
			
			double jttp = jtte.getValue();
			TopicSetRVal topicSet = jtte.getKey();
			
			double probCommand = norMod.computeNaturalCommandProb(natCommandSet, topicSet);
			double pCommandWithJoint = probCommand * jttp;
			probData += pCommandWithJoint;
			
			for(String topic : topicSet.topics){
				
				tempTopicCounts.add(topic, pCommandWithJoint);
				
				for(String natWord : allNatWords){
					
					double param = wp.prob(natWord, topic);
					
					
					double pCommandGivenNotTopicPrime;
					if(!natCommandSet.contains(natWord)){
						pCommandGivenNotTopicPrime = probCommand / param;
					}
					else{
						double probNotWord = norMod.probNotWordGivenTopicSet(natWord, topicSet);
						double num = probCommand * (1. - (1./param) * probNotWord);
						if(num == 0.){
							pCommandGivenNotTopicPrime = 0.;
						}
						else{
							pCommandGivenNotTopicPrime = num / (1. - probNotWord);
						}
					}
					
					if(Double.isNaN(pCommandGivenNotTopicPrime)){
						throw new RuntimeException("pCommandGivenNotTopicPrime is NaN.");
					}
					
					double jointP = param * pCommandGivenNotTopicPrime * jttp;
					tempJointCounts.add(tokenCombine(natWord, topic), jointP);
					
				}
				
				
			}
			
		}
		
		
		//now divide each sum by the prob of the data and add to our overall counts
		for(Map.Entry<String, Double> te : tempTopicCounts.entrySet()){
			
			String topic = te.getKey();
			double topicNum = te.getValue();
			double topicProb = 0.;
			if(probData > 0.){
				topicProb = topicNum / probData;
			}
			if(Double.isNaN(topicProb)){
				throw new RuntimeException("Prob of topic for data instance is NaN");
			}
			this.singleTopicCounts.add(topic, topicProb);
			
		}
		
		for(Map.Entry<String, Double> je : tempJointCounts.entrySet()){
			String combinedToken = je.getKey();
			double jointNum = je.getValue();
			double jointProb = 0.;
			if(probData > 0.){
				jointProb = jointNum / probData;
			}
			if(Double.isNaN(jointProb)){
				throw new RuntimeException("Prob of joint word and topic for data instance is NaN");
			}
			this.jointWordCounts.add(combinedToken, jointProb);
		}
		
		

	}

	@Override
	public void runMStep() {
		
		NoisyOr norMod = (NoisyOr)this.gm.getModuleWithName(Model3Controller.LANGMODNAME);
		Set<String> naturalWords = norMod.getNaturalWords();
		Set<String> topics = norMod.semanticTopics;
		WordParam wp = norMod.getWp();
		
		for(String topic : topics){
			double tCount = this.singleTopicCounts.v(topic);
			for(String nword : naturalWords){
				String jointToken = tokenCombine(nword, topic);
				double jCount = this.jointWordCounts.v(jointToken);
				double newParamVal = 0.;
				if(jCount > 0.){
					newParamVal = jCount / tCount;
				}
				
				if(Double.isNaN(newParamVal)){
					throw new RuntimeException("New word parameter is NaN");
				}
				
				wp.set(newParamVal, nword, topic);
			}
		}

		
		this.needsAggreatorReset = true;
		this.gm.emptyCache();

	}
	
	
	
	
	protected void resetAggregators(){
		this.jointWordCounts = new HashedAggregator<String>();
		this.singleTopicCounts = new HashedAggregator<String>();
		this.needsAggreatorReset = false;
	}
	
	
	
	protected Map<TopicSetRVal, Double> constructJointTopicAndTrajectoryEntry(int dataInstanceId, TrajectoryValue trajectoryVal){
		
		
		HashedAggregator<TopicSetRVal> topicAggregator = new HashedAggregator<TopicSetRVal>();
		
		StateRVValue sval = new StateRVValue(trajectoryVal.t.getState(0), this.hashingFactory, this.gm.getRVarWithName(TaskModule.STATENAME));
		
		List<RVariableValue> sconds = new ArrayList<RVariableValue>(1);
		sconds.add(sval);
		Iterator<GMQueryResult> lrIter = this.gm.getNonZeroIterator(this.gm.getRVarWithName(TaskModule.LIFTEDRFNAME), sconds, true);
		while(lrIter.hasNext()){
			GMQueryResult lrRes = lrIter.next();
			
			List<RVariableValue> lrConds = new ArrayList<RVariableValue>(2);
			lrConds.add(sval);
			lrConds.add(lrRes.getSingleQueryVar());
			Iterator<GMQueryResult> grIter = this.gm.getNonZeroIterator(this.gm.getRVarWithName(TaskModule.GROUNDEDRFNAME), lrConds, true);
			while(grIter.hasNext()){
				GMQueryResult grRes = grIter.next();
				double stackLRGR = lrRes.probability*grRes.probability;
				
				//compute probability of trajectory
				GMQuery trajQuery = new GMQuery();
				trajQuery.addQuery(trajectoryVal);
				trajQuery.addCondition(sval);
				trajQuery.addCondition(grRes.getSingleQueryVar());
				GMQueryResult trajRes = this.gm.getProb(trajQuery, true);
				
				double stackedTrajectoryProb = stackLRGR * trajRes.probability;
				
				List<RVariableValue> grConds = new ArrayList<RVariableValue>(3);
				grConds.add(sval);
				grConds.add(lrRes.getSingleQueryVar());
				grConds.add(grRes.getSingleQueryVar());
				
				Iterator<GMQueryResult> bIter = this.gm.getNonZeroIterator(this.gm.getRVarWithName(TaskModule.BINDINGNAME), grConds, true);
				while(bIter.hasNext()){
					GMQueryResult bRes = bIter.next();
					double stackedToBind = stackedTrajectoryProb * bRes.probability;
					
					List<RVariableValue> bConds = new ArrayList<RVariableValue>(2);
					bConds.add(lrRes.getSingleQueryVar());
					bConds.add(bRes.getSingleQueryVar());
					
					Iterator<GMQueryResult> topicIter = this.gm.getNonZeroIterator(this.gm.getRVarWithName(NoisyOr.TVECTOR), bConds, true);
					while(topicIter.hasNext()){
						GMQueryResult topicRes = topicIter.next();
						
						double topicStacked = stackedToBind * topicRes.probability;
						TopicSetRVal topicVal = (TopicSetRVal)topicRes.getSingleQueryVar();
						topicAggregator.add(topicVal, topicStacked);
					}
					
				}
				
		
				
			}
			
		}
		
		
		
		Map<TopicSetRVal, Double> mapResult = topicAggregator.getHashMap();
		
		/*
		for(Map.Entry<TopicVectorRVal, Double> e : mapResult.entrySet()){
			System.out.println(e.getValue() + ": " + e.getKey().toString());
		}
		System.out.println("");
		*/
		
		//if for some reason things are out of order, make room by adding nulls
		while(this.jointTopicAndTrajectory.size() < dataInstanceId+1){
			this.jointTopicAndTrajectory.add(null);
		}
		
		//add it to the list
		this.jointTopicAndTrajectory.set(dataInstanceId, mapResult);
		
		
		return mapResult;
		
		
	}
	
	
	protected static String tokenCombine(String prodWord, String genWord){
		return prodWord + "+++" + genWord;
	}

}
