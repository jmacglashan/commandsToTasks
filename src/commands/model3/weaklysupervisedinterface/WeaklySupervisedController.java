package commands.model3.weaklysupervisedinterface;

import behavior.irl.DGDIRLFactory;
import burlap.datastructures.HashedAggregator;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.GroundedProp;
import burlap.oomdp.core.states.State;
import burlap.oomdp.legacy.StateParser;
import burlap.oomdp.statehashing.HashableStateFactory;
import commands.data.TrainingElement;
import commands.data.Trajectory;
import commands.model3.GPConjunction;
import commands.model3.StringValue;
import commands.model3.TaskModule;
import commands.model3.TrajectoryModule;
import commands.model3.mt.MTModule;
import generativemodel.*;
import logicalexpressions.Conjunction;
import logicalexpressions.LogicalExpression;
import logicalexpressions.PFAtom;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * A controller for managing inference steps in a grounding language to reward function problem where the language
 * model is specified by a {@link commands.model3.weaklysupervisedinterface.WeaklySupervisedLanguageModel}.
 * This controller also provides the means to cache IRL results and load cached IRL results for increased reasoning speed
 * using the method {@link #cacheIRLProbabilitiesToDisk(String, burlap.oomdp.legacy.StateParser)} and
 * {@link #loadIRLProbabiltiesFromDisk(String, burlap.oomdp.legacy.StateParser)}, respectively.
 *
 *
 * @author James MacGlashan.
 */
public class WeaklySupervisedController {

	public final static String			TASKMODNAME = "taskMod";
	public final static String			TRAJECMODNAME = "trajectoryMod";


	protected GenerativeModel gm;
	protected TaskModule taskMod;
	protected TrajectoryModule trajectMod;

	protected WeaklySupervisedLanguageModel languageModel;

	protected List<WeaklySupervisedTrainingInstance> weaklySupervisedTrainingDataset;

	protected Domain domain;
	protected HashableStateFactory hashingFactory;

	protected boolean hasPerformedIRL = false;



	public WeaklySupervisedController(Domain domain, List<GPConjunction> taskGoals, HashableStateFactory hashingFactory, boolean addTermainateActionForIRL){

		this.domain = domain;
		this.hashingFactory = hashingFactory;

		this.gm = new GenerativeModel();

		this.taskMod = new TaskModule(TASKMODNAME, this.domain);
		this.gm.addGMModule(this.taskMod);

		RVariable liftedVar = gm.getRVarWithName(TaskModule.LIFTEDRFNAME);
		for(GPConjunction conj : taskGoals){
			TaskModule.LiftedVarValue lrf = new TaskModule.LiftedVarValue(liftedVar);
			for(GroundedProp gp : conj){
				lrf.addProp(gp);
			}
			this.taskMod.addLiftedVarValue(lrf);
		}

		DGDIRLFactory plannerFactory = new DGDIRLFactory(this.domain, 0.99, this.hashingFactory);
		this.trajectMod = new TrajectoryModule(TRAJECMODNAME, this.gm.getRVarWithName(TaskModule.STATENAME), this.gm.getRVarWithName(TaskModule.GROUNDEDRFNAME), this.domain, plannerFactory, addTermainateActionForIRL, true);
		this.gm.addGMModule(trajectMod);

	}

	public void setLanguageModel(WeaklySupervisedLanguageModel languageModel){
		this.languageModel = languageModel;
	}

	public void dumpLanguageMode(String path){
		((MTModule)(this.gm.getModuleWithName(MTWeaklySupervisedModel.LANGMODNAME))).dumpToFile(path);
	}


	public TrajectoryModule getTrajectoryModule(){
		return this.trajectMod;
	}

	public GenerativeModel getGM(){
		return this.gm;
	}


	public Domain getDomain(){
		return domain;
	}

	public HashableStateFactory getHashingFactory(){
		return this.hashingFactory;
	}

	public WeaklySupervisedLanguageModel getLanguageModel(){ return this.languageModel; }

	public void writeWeaklySupervisedData(String path){
		File file = new File(path);
		if(file.getParentFile() != null && !file.getParentFile().exists()){
			file.mkdirs();
		}

		Yaml yaml = new Yaml(new PFAtomRepresenter());

		FileWriter writer = null;
		try {
			writer = new FileWriter(path);
		} catch(IOException e) {
			e.printStackTrace();
		}

		yaml.dumpAll(this.weaklySupervisedTrainingDataset.iterator(), writer);

	}

	public class PFAtomConstructor extends Constructor{



	}

	public class PFAtomRepresenter extends Representer{
		public PFAtomRepresenter(){
			super();
			this.representers.put(PFAtom.class, new PFAtomRepresent());
			addClassTag(PFAtomShell.class, "!pfatom");
		}

		private class PFAtomRepresent implements Represent{
			@Override
			public Node representData(Object o) {
				PFAtom atom = (PFAtom)o;
				StringBuilder builder = new StringBuilder();
				builder.append(atom.name);
				for(int i = 0; i < atom.pfParams.length; i++){
					builder.append(" ").append(atom.pfParams[i]);
				}
				//return represent(new PFAtomShell(atom.parentExpression, atom.name, atom.pfParams));
				//return representScalar(new Tag("!pfatom"), builder.toString());
				try {
					return representJavaBean(getProperties(PFAtomShell.class), new PFAtomShell(atom.parentExpression, atom.name, atom.pfParams));
				}catch(Exception e){
					throw new RuntimeException("Not working..");
				}
			}
		}
	}

	private class PFAtomShell{
		public LogicalExpression parent;
		public String name;
		public String [] params;
		public PFAtomShell(){}

		public PFAtomShell(LogicalExpression parent, String name, String[] params) {
			this.parent = parent;
			this.name = name;
			this.params = params;
		}
	}


	public List<GMQueryResult> getRFDistribution(State initialState, String naturalCommand){

		//System.out.println(naturalCommand);

		TaskModule.StateRVValue sval = new TaskModule.StateRVValue(initialState, this.hashingFactory, this.gm.getRVarWithName(TaskModule.STATENAME));

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


					//convert to logical expressions
					LogicalExpression liftedTaskLE = this.convertLiftedIntoLogicalExpression(
							(TaskModule.LiftedVarValue)lrRes.getSingleQueryVar());

					LogicalExpression bindingConstraintLE = this.convertLiftedIntoLogicalExpression(
							(TaskModule.LiftedVarValue)bRes.getSingleQueryVar());

					double lp = this.languageModel.probabilityOfCommand(liftedTaskLE, bindingConstraintLE, naturalCommand);
					double p = lp * stackLRGRB;

					////System.out.println(p + ": " + grRes.getSingleQueryVar().toString() + " " + bRes.getSingleQueryVar().toString());

					GMQuery distroWrapper = new GMQuery();
					distroWrapper.addQuery(grRes.getSingleQueryVar());
					distroWrapper.addCondition(sval);


					jointP.add(distroWrapper, p);
					totalProb += p;


				}

			}

		}

		List<GMQueryResult> distro = new ArrayList<GMQueryResult>(jointP.size());
		for(Map.Entry<GMQuery, Double> e : jointP.entrySet()){
			double prob = e.getValue() / totalProb;
			GMQueryResult qr = new GMQueryResult(e.getKey(), prob);
			distro.add(qr);
		}


		return distro;
	}


	public List<GMQueryResult> getTaskDescriptionDistribution(State initialState, String naturalCommand){

		TaskModule.StateRVValue sval = new TaskModule.StateRVValue(initialState, this.hashingFactory, this.gm.getRVarWithName(TaskModule.STATENAME));

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


					//convert to logical expressions
					LogicalExpression liftedTaskLE = this.convertLiftedIntoLogicalExpression(
							(TaskModule.LiftedVarValue)lrRes.getSingleQueryVar());

					LogicalExpression bindingConstraintLE = this.convertLiftedIntoLogicalExpression(
							(TaskModule.LiftedVarValue)bRes.getSingleQueryVar());

					double lp = this.languageModel.probabilityOfCommand(liftedTaskLE, bindingConstraintLE, naturalCommand);
					double p = lp * stackLRGRB;

					////System.out.println(p + ": " + grRes.getSingleQueryVar().toString() + " " + bRes.getSingleQueryVar().toString());

					GMQuery distroWrapper = new GMQuery();
					distroWrapper.addQuery(lrRes.getSingleQueryVar());
					distroWrapper.addQuery(bRes.getSingleQueryVar());
					distroWrapper.addCondition(sval);
					distroWrapper.addCondition(new StringValue(naturalCommand, this.gm.getRVarWithName(MTModule.NNAME)));


					jointP.add(distroWrapper, p);
					totalProb += p;


				}

			}

		}

		List<GMQueryResult> distro = new ArrayList<GMQueryResult>(jointP.size());
		for(Map.Entry<GMQuery, Double> e : jointP.entrySet()){
			double prob = e.getValue() / totalProb;
			GMQueryResult qr = new GMQueryResult(e.getKey(), prob);
			distro.add(qr);
		}


		return distro;

	}



	public void createWeaklySupervisedTrainingDatasetFromTrajectoryDataset(List<TrainingElement> trajCommandDataset){
		List<WeaklySupervisedTrainingInstance> wsd = new LinkedList<WeaklySupervisedTrainingInstance>();

		for(TrainingElement te : trajCommandDataset){
			List<WeaklySupervisedTrainingInstance> instancesForTrjactory = this.getWeaklySupervisedTrainingInstancesForTrajectory(te.trajectory, te.command);
			wsd.addAll(instancesForTrjactory);
		}

		//convert to array list for fast access
		this.weaklySupervisedTrainingDataset = new ArrayList<WeaklySupervisedTrainingInstance>(wsd);


	}

	public void createOrAddWeaklySupervisedTrainingDatasetFromTrajectoryDataset(List<TrainingElement> trajCommandDataset){
		List<WeaklySupervisedTrainingInstance> wsd = new LinkedList<WeaklySupervisedTrainingInstance>();

		for(TrainingElement te : trajCommandDataset){
			List<WeaklySupervisedTrainingInstance> instancesForTrjactory = this.getWeaklySupervisedTrainingInstancesForTrajectory(te.trajectory, te.command);
			wsd.addAll(instancesForTrjactory);
		}

		//convert to array list for fast access
		if(this.weaklySupervisedTrainingDataset == null) {
			this.weaklySupervisedTrainingDataset = new ArrayList<WeaklySupervisedTrainingInstance>(wsd);
		}
		else{
			this.weaklySupervisedTrainingDataset.addAll(wsd);
		}


	}

	public void setWeaklySupervisedTrainingDataset(List<WeaklySupervisedTrainingInstance> trainingDataset){
		this.weaklySupervisedTrainingDataset = new ArrayList<WeaklySupervisedTrainingInstance>(trainingDataset);
	}

	public List<WeaklySupervisedTrainingInstance> getWeaklySupervisedTrainingDataset() {
		return weaklySupervisedTrainingDataset;
	}

	public void trainLanguageModel(){
		if(this.languageModel == null){
			throw new RuntimeException("Cannot train language model, because the weakly supervised language model has not be set!\n" +
					"Please use the setLanguageModel(WeaklySupervisedLanguageModel) method on the WeaklySupervisedController object.");
		}

		if(this.weaklySupervisedTrainingDataset == null){
			throw new RuntimeException("Cannot train the language model because the weakly supervised training dataset was not created!\n" +
					"Please use the createWeaklySupervisedTrainingDatasetFromTrajectoryDataset(List<TrainingElement>)  or " +
					"the setWeaklySupervisedTrainingDataset(List<WeaklySupervisedTrainingInstance>) method on the WeaklySupervisedController object.");
		}

		this.languageModel.learnFromDataset(this.weaklySupervisedTrainingDataset);

	}


	public List<WeaklySupervisedTrainingInstance> getWeaklySupervisedTrainingInstancesForTrajectory(Trajectory trajectory, String command){

		List<WeaklySupervisedTrainingInstance> instances = new ArrayList<WeaklySupervisedTrainingInstance>();

		TrajectoryModule.TrajectoryValue trajectoryVal = new TrajectoryModule.TrajectoryValue(trajectory, this.gm.getRVarWithName(TrajectoryModule.TNAME));
		double sumTrajectoryProb = 0.;

		TaskModule.StateRVValue sval = new TaskModule.StateRVValue(trajectoryVal.t.getState(0), this.hashingFactory, this.gm.getRVarWithName(TaskModule.STATENAME));

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
				trajQuery.addQuery(trajectoryVal);
				trajQuery.addCondition(sval);
				trajQuery.addCondition(grRes.getSingleQueryVar());
				GMQueryResult trajRes = this.gm.getProb(trajQuery, true);

				double stackedTrajectoryProb = stackLRGR * trajRes.probability;
				sumTrajectoryProb += stackedTrajectoryProb;

				Iterator<GMQueryResult> bIter = this.gm.getNonZeroIterator(this.gm.getRVarWithName(TaskModule.BINDINGNAME), grConds, true);
				while(bIter.hasNext()){
					GMQueryResult bRes = bIter.next();

					//convert to logical expressions
					LogicalExpression liftedTaskLE = this.convertLiftedIntoLogicalExpression(
							(TaskModule.LiftedVarValue)lrRes.getSingleQueryVar());

					LogicalExpression bindingConstraintLE = this.convertLiftedIntoLogicalExpression(
							(TaskModule.LiftedVarValue)bRes.getSingleQueryVar());


					double jointP = stackedTrajectoryProb * bRes.probability;
					WeaklySupervisedTrainingInstance ti = new WeaklySupervisedTrainingInstance(liftedTaskLE, bindingConstraintLE, command, stackedTrajectoryProb);
					instances.add(ti);


				}


			}


		}


		//normalize before returning
		for(WeaklySupervisedTrainingInstance ti : instances){
			ti.weight /= sumTrajectoryProb;
		}

		this.hasPerformedIRL = true;

		return instances;
	}


	public void cacheIRLProbabilitiesToDisk(String pathToCacheDirectory, StateParser sp){
		if(!this.hasPerformedIRL){
			throw new RuntimeException("Cannot cache IRL probabilities to disk, because there has been no task inference performed. You should" +
					"create a weakly supervised training dataset from a trajectory dataset first using the" +
					"createWeaklySupervisedTrainingDatasetFromTrajectoryDataset(List<TrainingElement>) method of this WeaklySupervisedController object.");
		}

		this.trajectMod.writeCacheToDisk(pathToCacheDirectory, this.domain, sp);
	}

	public void loadIRLProbabiltiesFromDisk(String pathToCacheDirectory, StateParser sp){
		this.trajectMod.readCacheFromDisk(pathToCacheDirectory, this.domain, sp, this.getHashingFactory());
	}


	protected LogicalExpression convertLiftedIntoLogicalExpression(TaskModule.LiftedVarValue varValue){
		Conjunction con = new Conjunction();
		for(GroundedProp gp : varValue.conditions){
			PFAtom atom = new PFAtom(new GroundedProp(gp.pf, gp.params.clone()));
			con.addChild(atom);
		}

		return con;
	}



}
