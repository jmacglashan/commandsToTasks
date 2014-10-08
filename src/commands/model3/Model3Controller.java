package commands.model3;

import generativemodel.GMQuery;
import generativemodel.GMQueryResult;
import generativemodel.GenerativeModel;
import generativemodel.RVariable;
import generativemodel.RVariableValue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import behavior.irl.DGDIRLFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.GroundedProp;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;

import commands.data.TrainingElement;
import commands.data.TrainingElementParser;
import commands.data.Trajectory;
import commands.model3.TaskModule.LiftedVarValue;
import commands.model3.TaskModule.StateRVValue;
import commands.model3.TrajectoryModule.TrajectoryValue;
import commands.model3.bagofwords.BagOfWordsModule;
import commands.model3.mt.MTModule;
import commands.model3.mt.SentenceReasoner;
import commands.model3.mt.TokenedString;
import commands.model3.mt.Tokenizer;
import commands.model3.mt.em.WeightedMTInstance;
import commands.model3.mt.em.WeightedMTInstance.WeightedSemanticCommandPair;
import commands.model3.noisyor.NoisyOr;

import burlap.datastructures.HashedAggregator;
import em.Dataset;

public class Model3Controller {

	public final static String			TASKMODNAME = "taskMod";
	public final static String			TRAJECMODNAME = "trajectoryMod";
	public final static String			LANGMODNAME = "langMod";
	
	
	protected GenerativeModel			gm;
	protected TaskModule				taskMod;
	protected TrajectoryModule			trajectMod;
	
	protected SentenceReasoner			sentenceReasoner;
	
	protected Domain					domain;
	protected StateHashFactory			hashingFactory;
	
	protected RVariable					naturalCommandVariable;
	
	protected List<TrainingElement>		trajectoryDataset;
	
	
	public Model3Controller(Domain domain, List<GPConjunction> taskGoals, StateHashFactory hashingFactory, boolean addTermainateActionForIRL){
		
		this.domain = domain;
		this.hashingFactory = hashingFactory;
		
		this.gm = new GenerativeModel();
		
		this.taskMod = new TaskModule(TASKMODNAME, this.domain);
		this.gm.addGMModule(this.taskMod);
		
		RVariable liftedVar = gm.getRVarWithName(TaskModule.LIFTEDRFNAME);
		for(GPConjunction conj : taskGoals){
			LiftedVarValue lrf = new LiftedVarValue(liftedVar);
			for(GroundedProp gp : conj){
				lrf.addProp(gp);
			}
			this.taskMod.addLiftedVarValue(lrf);
		}
		
		DGDIRLFactory plannerFactory = new DGDIRLFactory(this.domain, 0.99, this.hashingFactory);
		this.trajectMod = new TrajectoryModule(TRAJECMODNAME, this.gm.getRVarWithName(TaskModule.STATENAME), this.gm.getRVarWithName(TaskModule.GROUNDEDRFNAME), this.domain, plannerFactory, addTermainateActionForIRL, true);
		this.gm.addGMModule(trajectMod);
		
	}

	public TrajectoryModule getTrajectoryModule(){
		return this.trajectMod;
	}
	
	public GenerativeModel getGM(){
		return this.gm;
	}
	
	public List<TrainingElement> getTrajectoryDataset(){
		return this.trajectoryDataset;
	}
	
	public Domain getDomain(){
		return domain;
	}
	
	public StateHashFactory getHashingFactory(){
		return this.hashingFactory;
	}
	
	public void setToMTLanguageModel(Set<String> semanticWords, Set<String> naturalWords, int maxSemanticCommandLength, int maxNaturalCommandLenth, Tokenizer tokenizer){
		
		MTModule langMod = new MTModule(LANGMODNAME, this.gm.getRVarWithName(TaskModule.LIFTEDRFNAME), this.gm.getRVarWithName(TaskModule.BINDINGNAME), 
				semanticWords, naturalWords, maxSemanticCommandLength, maxNaturalCommandLenth, tokenizer);
		
		this.gm.addGMModule(langMod);
		
		this.naturalCommandVariable = this.gm.getRVarWithName(MTModule.NNAME);
		
	}
	
	
	public void setToMTLanguageModel(List<TrainingElement> trainingData, int maxSemanticCommandLength, Tokenizer tokenizer){
		
		this.trajectoryDataset = trainingData;
		
		Set<String> semanticWords = this.getSemanticWords(true);
		Set<String> naturalWords = this.getNaturalWords(trainingData, tokenizer);
		
		int maxNaturalCommandLength = 0;
		for(TrainingElement te : trainingData){
			maxNaturalCommandLength = Math.max(maxNaturalCommandLength, tokenizer.tokenize(te.command).size());
		}
		
		MTModule langMod = new MTModule(LANGMODNAME, this.gm.getRVarWithName(TaskModule.LIFTEDRFNAME), this.gm.getRVarWithName(TaskModule.BINDINGNAME), 
				semanticWords, naturalWords, maxSemanticCommandLength, maxNaturalCommandLength, tokenizer);
		
		this.gm.addGMModule(langMod);
		
		this.naturalCommandVariable = this.gm.getRVarWithName(MTModule.NNAME);
		
	}
	
	public void setToMTLanugageModelUsingMTDataset(List<WeightedMTInstance> dataset, Tokenizer tokenizer, boolean isStrictMT){
		
		Set<String> semanticWords = new HashSet<String>();
		Set<String> naturalWords = new HashSet<String>();
		
		int maxSemanticCommandLength = getSemanticWordsFromMTDataset(dataset, semanticWords);
		int maxNaturalCommandLength = getNaturalWordsFromMTDataset(dataset, naturalWords);
		
		MTModule langMod = new MTModule(LANGMODNAME, this.gm.getRVarWithName(TaskModule.LIFTEDRFNAME), this.gm.getRVarWithName(TaskModule.BINDINGNAME), 
				semanticWords, naturalWords, maxSemanticCommandLength, maxNaturalCommandLength, tokenizer);
		
		this.gm.addGMModule(langMod);
		
		this.naturalCommandVariable = this.gm.getRVarWithName(MTModule.NNAME);
		
		if(isStrictMT){
			this.sentenceReasoner = new SentenceReasoner(this.gm, dataset);
		}
		
	}
	
	
	public void setToBOWLanugageModel(List<TrainingElement> trainingData, Tokenizer tokenizer, boolean includeInSemanticsParameterObjectClasses){
		
		this.trajectoryDataset = trainingData;
		
		Set<String> semanticWords = this.getSemanticWords(includeInSemanticsParameterObjectClasses);
		Set<String> naturalWords = this.getNaturalWords(trainingData, tokenizer);
		
		BagOfWordsModule bowModule = new BagOfWordsModule(LANGMODNAME, this.gm.getRVarWithName(TaskModule.LIFTEDRFNAME), this.gm.getRVarWithName(TaskModule.BINDINGNAME),
				semanticWords, naturalWords, tokenizer);
		
		bowModule.setIncludePFParamClasses(includeInSemanticsParameterObjectClasses);
		
		this.gm.addGMModule(bowModule);
		
		this.naturalCommandVariable = this.gm.getRVarWithName(BagOfWordsModule.NNAME);
	}
	
	
	
	public void setToNORLanugageModel(List<TrainingElement> trainingData, Tokenizer tokenizer, boolean includeInSemanticsParameterObjectClasses){
		
		this.trajectoryDataset = trainingData;
		
		Set<String> semanticWords = this.getSemanticWords(includeInSemanticsParameterObjectClasses);
		Set<String> naturalWords = this.getNaturalWords(trainingData, tokenizer);
		
		NoisyOr norModule = new NoisyOr(LANGMODNAME, this.gm.getRVarWithName(TaskModule.LIFTEDRFNAME), this.gm.getRVarWithName(TaskModule.BINDINGNAME),
				semanticWords, naturalWords, tokenizer);
		
		norModule.setIncludePFParamClasses(includeInSemanticsParameterObjectClasses);
		
		this.gm.addGMModule(norModule);
		
		this.naturalCommandVariable = this.gm.getRVarWithName(NoisyOr.NNAME);
	}
	
	
	
	
	public List<GMQueryResult> getLiftedRFAndBindingDistribution(State initialState, String naturalCommand){
		
		StateRVValue sval = new StateRVValue(initialState, this.hashingFactory, this.gm.getRVarWithName(TaskModule.STATENAME));
		StringValue ncommandVal = new StringValue(naturalCommand, naturalCommandVariable);
		
		HashedAggregator<GMQuery> jointP = new HashedAggregator<GMQuery>();
		double totalProb = 0.;
		
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
				
				List<RVariableValue> grConds = new ArrayList<RVariableValue>(3);
				grConds.add(sval);
				grConds.add(lrRes.getSingleQueryVar());
				grConds.add(grRes.getSingleQueryVar());
				Iterator<GMQueryResult> bIter = this.gm.getNonZeroIterator(this.gm.getRVarWithName(TaskModule.BINDINGNAME), grConds, true);
				while(bIter.hasNext()){
					GMQueryResult bRes = bIter.next();
					double stackLRGRB = stackLRGR * bRes.probability;
					
					GMQuery nCommandQuery = new GMQuery();
					nCommandQuery.addQuery(ncommandVal);
					nCommandQuery.addCondition(lrRes.getSingleQueryVar());
					nCommandQuery.addCondition(bRes.getSingleQueryVar());
					
					GMQueryResult langQR = this.gm.getProb(nCommandQuery, true);
					double p = langQR.probability * stackLRGRB;
					
					GMQuery distroWrapper = new GMQuery();
					distroWrapper.addQuery(lrRes.getSingleQueryVar());
					distroWrapper.addQuery(bRes.getSingleQueryVar());
					distroWrapper.addCondition(sval);
					distroWrapper.addCondition(ncommandVal);
					
					jointP.add(distroWrapper, p);
					totalProb += p;
					
				}
				
			}
			
		}
		
		List<GMQueryResult> distro = new ArrayList<GMQueryResult>(jointP.size());
		for(Entry<GMQuery, Double> e : jointP.entrySet()){
			double prob = e.getValue() / totalProb;
			GMQueryResult qr = new GMQueryResult(e.getKey(), prob);
			distro.add(qr);
		}
		
		
		return distro;
	}
	
	public List<GMQueryResult> getSemanticSentenceDistributionUsingStrictMT(String naturalCommand){
		if(this.sentenceReasoner == null){
			throw new RuntimeException("This controller is not set to work in a strict MT setting; did you mean to use another inference method?");
		}
		return this.sentenceReasoner.distribution(naturalCommand);
	}
	
	public List<GMQueryResult> getRFDistribution(State initialState, String naturalCommand){
		
		//System.out.println(naturalCommand);
		
		StateRVValue sval = new StateRVValue(initialState, this.hashingFactory, this.gm.getRVarWithName(TaskModule.STATENAME));
		StringValue ncommandVal = new StringValue(naturalCommand, naturalCommandVariable);
		
		HashedAggregator<GMQuery> jointP = new HashedAggregator<GMQuery>();
		double totalProb = 0.;
		
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
				
				List<RVariableValue> grConds = new ArrayList<RVariableValue>(3);
				grConds.add(sval);
				grConds.add(lrRes.getSingleQueryVar());
				grConds.add(grRes.getSingleQueryVar());
				Iterator<GMQueryResult> bIter = this.gm.getNonZeroIterator(this.gm.getRVarWithName(TaskModule.BINDINGNAME), grConds, true);
				while(bIter.hasNext()){
					GMQueryResult bRes = bIter.next();
					double stackLRGRB = stackLRGR * bRes.probability;
					
					GMQuery nCommandQuery = new GMQuery();
					nCommandQuery.addQuery(ncommandVal);
					nCommandQuery.addCondition(lrRes.getSingleQueryVar());
					nCommandQuery.addCondition(bRes.getSingleQueryVar());
					
					GMQueryResult langQR = this.gm.getProb(nCommandQuery, true);
					double p = langQR.probability * stackLRGRB;
					
					//System.out.println(p + ": " + grRes.getSingleQueryVar().toString() + " " + bRes.getSingleQueryVar().toString());
					
					GMQuery distroWrapper = new GMQuery();
					distroWrapper.addQuery(grRes.getSingleQueryVar());
					distroWrapper.addCondition(sval);
					distroWrapper.addCondition(ncommandVal);
					
					jointP.add(distroWrapper, p);
					totalProb += p;
					
				}
				
			}
			
		}
		
		List<GMQueryResult> distro = new ArrayList<GMQueryResult>(jointP.size());
		for(Entry<GMQuery, Double> e : jointP.entrySet()){
			double prob = e.getValue() / totalProb;
			GMQueryResult qr = new GMQueryResult(e.getKey(), prob);
			distro.add(qr);
		}
		
		
		return distro;
	}


	public List<GMQueryResult> getRFDistributionFromState(State s){

		StateRVValue sval = new StateRVValue(s, this.hashingFactory, this.gm.getRVarWithName(TaskModule.STATENAME));

		HashedAggregator<GMQuery> jointP = new HashedAggregator<GMQuery>();
		double totalProb = 0.;

		List<RVariableValue> sconds = new ArrayList<RVariableValue>(1);
		sconds.add(sval);
		Iterator<GMQueryResult> lrIter = this.gm.getNonZeroIterator(this.gm.getRVarWithName(TaskModule.LIFTEDRFNAME), sconds, true);
		while(lrIter.hasNext()) {
			GMQueryResult lrRes = lrIter.next();

			List<RVariableValue> lrConds = new ArrayList<RVariableValue>(2);
			lrConds.add(sval);
			lrConds.add(lrRes.getSingleQueryVar());
			Iterator<GMQueryResult> grIter = this.gm.getNonZeroIterator(this.gm.getRVarWithName(TaskModule.GROUNDEDRFNAME), lrConds, true);
			while (grIter.hasNext()) {
				GMQueryResult grRes = grIter.next();
				double stackLRGR = lrRes.probability * grRes.probability;
				totalProb += stackLRGR;
				jointP.add(grRes, stackLRGR);

			}


		}

		List<GMQueryResult> distro = new ArrayList<GMQueryResult>(jointP.size());
		for(Entry<GMQuery, Double> e : jointP.entrySet()){
			double prob = e.getValue() / totalProb;
			GMQueryResult qr = new GMQueryResult(e.getKey(), prob);
			distro.add(qr);
		}


		return distro;
	}
	
	
	public List<GMQueryResult> getRFDistributionFromTrajectory(Trajectory trajectory){
		
		HashedAggregator<GMQuery> jointP = new HashedAggregator<GMQuery>();
		
		StateRVValue sval = new StateRVValue(trajectory.getState(0), this.hashingFactory, this.gm.getRVarWithName(TaskModule.STATENAME));
		TrajectoryValue trajectoryVal = new TrajectoryValue(trajectory, this.gm.getRVarWithName(TrajectoryModule.TNAME));
		double sumTrajectoryProb = 0.;
		
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


				//System.out.println("Iterating: " + grRes.getSingleQueryVar().toString());
				
				List<RVariableValue> grConds = new ArrayList<RVariableValue>(3);
				grConds.add(sval);
				grConds.add(lrRes.getSingleQueryVar());
				grConds.add(grRes.getSingleQueryVar());
				
				//compute probability of trajectory
				GMQuery trajQuery = new GMQuery();
				trajQuery.addQuery(trajectoryVal);
				trajQuery.addCondition(sval);
				trajQuery.addCondition(grRes.getSingleQueryVar());
				GMQueryResult trajRes = this.gm.getProb(trajQuery, true);
				
				double stackedTrajectoryProb = stackLRGR * trajRes.probability;
				sumTrajectoryProb += stackedTrajectoryProb;
				
				GMQuery distroWrapper = new GMQuery();
				distroWrapper.addQuery(grRes.getSingleQueryVar());
				distroWrapper.addCondition(trajectoryVal);
				
				jointP.add(distroWrapper, stackedTrajectoryProb);
		
				
			}
			
		}
		
		List<GMQueryResult> distro = new ArrayList<GMQueryResult>(jointP.size());
		for(Entry<GMQuery, Double> e : jointP.entrySet()){
			double prob = e.getValue() / sumTrajectoryProb;
			GMQueryResult qr = new GMQueryResult(e.getKey(), prob);
			distro.add(qr);
		}
		
		
		return distro;
		
		
	}
	
	
	
	public Set<String> getSemanticWords(boolean includeParameterObjectClass){
		Set<String> semanticWords = new HashSet<String>();
		semanticWords.add(TokenedString.NULLTOKEN);
		for(PropositionalFunction pf : this.domain.getPropFunctions()){
			semanticWords.add(pf.getName());
			if(includeParameterObjectClass){
				for(String p : pf.getParameterClasses()){
					semanticWords.add(p);
				}
			}
		}
		return semanticWords;
	}
	
	public Set<String> getNaturalWords(List<TrainingElement> dataset, Tokenizer tokenizer){
		Set<String> naturalWords = new HashSet<String>();
		for(TrainingElement te : dataset){
			TokenedString t = tokenizer.tokenize(te.command);
			for(int i = 1; i <= t.size(); i++){
				String nw = t.t(i);
				naturalWords.add(nw);
			}
		}
		return naturalWords;
	}
	
	public List<WeightedMTInstance> getWeightedMTDatasetFromTrajectoryDataset(List<TrainingElement> trajectoryDataset, Tokenizer tokenizer, double threshold){
		
		List<WeightedMTInstance> mtDataset = new ArrayList<WeightedMTInstance>(trajectoryDataset.size());
		int ind = 0;
		for(TrainingElement te : trajectoryDataset){

			System.out.println("Performing IRL on " + te.identifier + "(" + ind + "/" + trajectoryDataset.size() + "): " + te.command);
			TrajectoryValue trajectoryVal = new TrajectoryValue(te.trajectory, this.gm.getRVarWithName(TrajectoryModule.TNAME));
			Map<String, Double> semanticProbs = this.getSemanticSentenceDistribution(trajectoryVal);
			WeightedMTInstance instance = new WeightedMTInstance(tokenizer.tokenize(te.command));
			System.out.println(te.command);
			for(Map.Entry<String, Double> e : semanticProbs.entrySet()){
				double p = e.getValue();
				if(p > threshold){
					TokenedString tokenedSem = tokenizer.tokenize(e.getKey());
					instance.addWeightedSemanticCommand(tokenedSem, p);
				}
			}
			mtDataset.add(instance);
			ind++;
			
		}
		
		
		
		return mtDataset;
		
	}
	
	public Dataset getEMDatasetFromTrajectoriesDataset(List<TrainingElement> trajDataset){
		
		Dataset dataset = new Dataset();
		
		for(TrainingElement te : trajDataset){
			StringValue naturalCommand = new StringValue(te.command, naturalCommandVariable);
			TrajectoryValue trajectoryVal = new TrajectoryValue(te.trajectory, this.gm.getRVarWithName(TrajectoryModule.TNAME));
			List<RVariableValue> observables = new ArrayList<RVariableValue>(2);
			observables.add(naturalCommand);
			observables.add(trajectoryVal);
			dataset.addDataInstance(observables);
		}
		
		return dataset;
		
	}
	
	protected Map<String, Double> getSemanticSentenceDistribution(TrajectoryValue trajectory){
		
		StateRVValue sval = new StateRVValue(trajectory.t.getState(0), this.hashingFactory, this.gm.getRVarWithName(TaskModule.STATENAME));
		HashedAggregator<String> semSentenceProbs = new HashedAggregator<String>();
		double sumTrajectoryProb = 0.;
		
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
				
				List<RVariableValue> grConds = new ArrayList<RVariableValue>(3);
				grConds.add(sval);
				grConds.add(lrRes.getSingleQueryVar());
				grConds.add(grRes.getSingleQueryVar());
				System.out.println("IRL on " + grRes.getSingleQueryVar().toString());
				
				//compute probability of trajectory
				GMQuery trajQuery = new GMQuery();
				trajQuery.addQuery(trajectory);
				trajQuery.addCondition(sval);
				trajQuery.addCondition(grRes.getSingleQueryVar());
				GMQueryResult trajRes = this.gm.getProb(trajQuery, true);
				
				double stackedTrajectoryProb = stackLRGR * trajRes.probability;
				sumTrajectoryProb += stackedTrajectoryProb;
				
				Iterator<GMQueryResult> bIter = this.gm.getNonZeroIterator(this.gm.getRVarWithName(TaskModule.BINDINGNAME), grConds, true);
				while(bIter.hasNext()){
					GMQueryResult bRes = bIter.next();
					
					String sementicString = getSemanticString((LiftedVarValue)lrRes.getSingleQueryVar(), (LiftedVarValue)bRes.getSingleQueryVar());
					double jointP = stackedTrajectoryProb * bRes.probability;
					semSentenceProbs.add(sementicString, jointP);
					
				}
				
				
			}
			
			
		}
		
		
		Map<String, Double> semanticStringProbs = new HashMap<String, Double>(semSentenceProbs.size());
		for(Map.Entry<String, Double> e : semSentenceProbs.entrySet()){
			double normP = e.getValue() / sumTrajectoryProb;
			semanticStringProbs.put(e.getKey(), normP);
		}
		
		return semanticStringProbs;
	}
	
	
	
	public static String getSemanticString(LiftedVarValue liftedRF, LiftedVarValue bindingConstraints){

		StringBuffer buf = new StringBuffer();
		boolean first = true;
		for(GroundedProp gp : liftedRF.conditions){
			if(!first){
				buf.append(" ");
			}
			buf.append(gp.pf.getName());
			for(String p : gp.pf.getParameterClasses()){
				buf.append(" ").append(p);
			}
			first = false;
		}
		for(GroundedProp gp : bindingConstraints.conditions){
			buf.append(" ").append(gp.pf.getName());
			for(String p : gp.pf.getParameterClasses()){
				buf.append(" ").append(p);
			}
		}
		
		return buf.toString();
	}
	
	
	public static List <TrainingElement> getCommandsDataset(Domain domain, String path, StateParser sp){
		
		TrainingElementParser teparser = new TrainingElementParser(domain, sp);
		
		//get dataset
		List<TrainingElement> dataset = teparser.getTrainingElementDataset(path, ".txt");
		
		return dataset;
	}
	
	public static List<WeightedMTInstance> getStrictMTDataset(Tokenizer tokenizer, String pathToDataDir, String dataFileExtension){
		
		//get rid of trailing /
		if(pathToDataDir.charAt(pathToDataDir.length()-1) == '/'){
			pathToDataDir = pathToDataDir.substring(0, pathToDataDir.length());
		}
		
		File dir = new File(pathToDataDir);
		final String ext = new String(dataFileExtension);
		
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				if(name.endsWith(ext)){
					return true;
				}
				return false;
			}
		};
		String[] children = dir.list(filter);
		
		List<WeightedMTInstance> dataset = new ArrayList<WeightedMTInstance>(children.length);
		for(String child : children){
			String path = pathToDataDir + "/" + child;
			WeightedMTInstance inst = getStrictMTDataInstanceFromFile(tokenizer, path);
			dataset.add(inst);
		}
		
		
		return dataset;
		
	}
	
	public static WeightedMTInstance getStrictMTDataInstanceFromFile(Tokenizer tokenizer, String pathToFile){
		
		WeightedMTInstance d = null;
		
		try {
			BufferedReader in = new BufferedReader(new FileReader(pathToFile));
			String l1 = in.readLine().trim();
			String l2 = in.readLine().trim();
			
			d = new WeightedMTInstance(tokenizer.tokenize(l2));
			d.addWeightedSemanticCommand(tokenizer.tokenize(l1), 1.);
			
			in.close();
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Could now parse datasetfile: " + pathToFile);
		}
		
		return d;
	}
	
	
	/**
	 * Fills in up a provided set with the semantic words and returns the maximum semantic command length
	 * @param dataset the input MT dataset
	 * @param semWords the set to fill with the semantic words
	 * @return the maximum semantic command length
	 */
	public static int getSemanticWordsFromMTDataset(List<WeightedMTInstance> dataset, Set<String> semWords){
		
		int maxLength = 0;
		for(WeightedMTInstance wi : dataset){
			for(WeightedSemanticCommandPair wsc : wi){
				maxLength = Math.max(maxLength, wsc.semanticCommand.size());
				for(int i = 0; i <= wsc.semanticCommand.size(); i++){
					semWords.add(wsc.semanticCommand.t(i));
				}
			}
		}
		
		return maxLength;
		
	}
	
	
	/**
	 * Fills in up a provided set with the natural words and returns the maximum semantic command length
	 * @param dataset the input MT dataset
	 * @param natWords the set to fill with the natural words
	 * @return the maximum natural command length
	 */
	public static int getNaturalWordsFromMTDataset(List<WeightedMTInstance> dataset, Set<String> natWords){
		
		int maxLength = 0;
		for(WeightedMTInstance wi : dataset){
			maxLength = Math.max(maxLength, wi.naturalCommand.size());
			for(int i = 1; i <= wi.naturalCommand.size(); i++){
				natWords.add(wi.naturalCommand.t(i));
			}
		}
		
		return maxLength;
		
	}
	
	
}
