package commands.tests;

import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.statehashing.NameDependentStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.GroundedProp;
import burlap.oomdp.core.State;
import commands.data.TrainingElement;
import commands.data.Trajectory;
import commands.model3.GPConjunction;
import commands.model3.Model3Controller;
import commands.model3.TaskModule;
import commands.model3.TaskModule.LiftedVarValue;
import commands.model3.TaskModule.RFConVariableValue;
import commands.model3.TrajectoryModule;
import commands.model3.bagofwords.BagOfWordsEMModule;
import commands.model3.bagofwords.BagOfWordsModule;
import commands.model3.mt.MTModule;
import commands.model3.mt.MTModule.WordParam;
import commands.model3.mt.TokenedString;
import commands.model3.mt.Tokenizer;
import commands.model3.mt.em.MTEMModule;
import commands.model3.mt.em.WeightedMTInstance;
import commands.model3.mt.em.WeightedMTInstance.WeightedSemanticCommandPair;
import commands.model3.noisyor.NoisyOr;
import commands.model3.noisyor.NoisyOrEMModule;
import domain.singleagent.sokoban2.Sokoban2Domain;
import domain.singleagent.sokoban2.Sokoban2Parser;
import domain.singleagent.sokoban2.SokobanOldToNewParser;
import em.Dataset;
import em.EMAlgorithm;
import generativemodel.GMQueryResult;
import generativemodel.GenerativeModel;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;






/**
 * @author James MacGlashan.
 */
public class Model3ControllerExpertData {


	public static String 						DATASETTESTPATH = "oomdpResearch/dataFiles/commands/jerryNormalNoNoise";
	public static String 						CACHEPATH = "oomdpResearch/dataFiles/commands/jerryTrajectoryCache";



	/**
	 * @param args
	 */
	public static void main(String[] args) {

		//getAverageNumberOfWordsInTrajDataset();
		//getLatexDatasetTable();

		//uniformTest();
		//getUniqueCommands(DATASETTESTPATH);
		//execptedUniformRandom(DATASETTESTPATH);
		trajectoryTrainingTest(DATASETTESTPATH, CACHEPATH);
		//verifyIRL(DATASETTESTPATH, CACHEPATH);
		//verifyIRLSpecific(DATASETTESTPATH);
		//verifyRFLabels(DATASETTESTPATH);
		//verifyTrajectories(DATASETTESTPATH);
		//trajectoryTrainingTest(args[0]);
		//testMTWords(DATASETTESTPATH);
		//parallelLOOOutput(args);
		//parallelBOWLOOOutput(args);
		//parallelNORLOOOutput(args);
		//trajectoryToWeightedMTDataset();
		//rfFromTrajectoryDistributionTest();
		//strictMTTest();
		//strictMTTrainingTest();
		//strictMTLOOTest();
		//parameterTest();
		//uniformBOWTest();
		//uniformNORTest();
		//trajectoryBOWTrainingTest(DATASETTESTPATH, CACHEPATH);
		//trajectoryNORTrainingTest();


		//writeIRLCache(DATASETTESTPATH, "oomdpResearch/dataFiles/commands/jerryTrajectoryCache");
		//checkCacheRead(DATASETTESTPATH, "oomdpResearch/dataFiles/commands/jerryTrajectoryCache");


		//checkForIdenticalCommands(DATASETTESTPATH);

	}



	public static Model3Controller constructController(){
		Sokoban2Domain dg = new Sokoban2Domain();
		Domain domain = dg.generateDomain();
		StateHashFactory hashingFactory = new NameDependentStateHashFactory();
		List<GPConjunction> liftedTaskDescriptions = new ArrayList<GPConjunction>(2);

		GPConjunction atr = new GPConjunction();
		atr.addGP(new GroundedProp(domain.getPropFunction(Sokoban2Domain.PFAGENTINROOM), new String[]{"a", "r"}));
		liftedTaskDescriptions.add(atr);

		GPConjunction btr = new GPConjunction();
		btr.addGP(new GroundedProp(domain.getPropFunction(Sokoban2Domain.PFBLOCKINROOM), new String[]{"b", "r"}));
		liftedTaskDescriptions.add(btr);

		//jerry extensions
		GPConjunction abtr = new GPConjunction();
		abtr.addGP(new GroundedProp(domain.getPropFunction(Sokoban2Domain.PFAGENTINROOM), new String[]{"a", "r1"}));
		abtr.addGP(new GroundedProp(domain.getPropFunction(Sokoban2Domain.PFBLOCKINROOM), new String[]{"b", "r2"}));
		liftedTaskDescriptions.add(abtr);


		/*
		GPConjunction b2tr = new GPConjunction();
		b2tr.addGP(new GroundedProp(domain.getPropFunction(Sokoban2Domain.PFBLOCKINROOM), new String[]{"b1", "r1"}));
		b2tr.addGP(new GroundedProp(domain.getPropFunction(Sokoban2Domain.PFBLOCKINROOM), new String[]{"b2", "r2"}));
		liftedTaskDescriptions.add(b2tr);
		*/


		Model3Controller controller = new Model3Controller(domain, liftedTaskDescriptions, hashingFactory, true);


		return controller;
	}

	public static void uniformTest(){


		Model3Controller controller = constructController();

		Domain domain = controller.getDomain();

		StateParser sp = new SokobanOldToNewParser(domain);
		List<TrainingElement> trainingDataset = Model3Controller.getCommandsDataset(domain, DATASETTESTPATH, sp);

		Tokenizer tokenizer = new Tokenizer(true);
		tokenizer.addDelimiter("-");

		controller.setToMTLanguageModel(trainingDataset, 17, tokenizer);

		outputConstraintAndRFDistro(trainingDataset, controller);

	}


	public static void execptedUniformRandom(String datasetpath){

		Model3Controller controller = constructController();
		Domain domain = controller.getDomain();

		GenerativeModel gm = controller.getGM();

		StateParser sp = new Sokoban2Parser(domain);
		List<TrainingElement> trainingDataset = Model3Controller.getCommandsDataset(domain, datasetpath, sp);

		Tokenizer tokenizer = new Tokenizer(true, true);
		tokenizer.addDelimiter("-");

		//System.out.println("Setting...");
		//controller.setToMTLanugageModelUsingMTDataset(new ArrayList<WeightedMTInstance>(),tokenizer,false);

		System.out.println("Testing.");
		double sumExpected = 0.;
		int c = 0;
		for(TrainingElement te : trainingDataset){

			c++;


			//List<GMQueryResult> possibleAnswers = controller.getRFDistribution(te.trajectory.getState(0), te.command);
			List<GMQueryResult> possibleAnswers = controller.getRFDistributionFromState(te.trajectory.getState(0));
			int n = possibleAnswers.size();
			double expectation = 1./(double)n;

			System.out.println("Testing " + c + ": " + expectation);

			sumExpected += expectation;

		}

		double totalExpected = sumExpected/(double)trainingDataset.size();

		System.out.println("Uniform random expectation: " + totalExpected);


	}


	public static void uniformBOWTest(){

		Model3Controller controller = constructController();

		Domain domain = controller.getDomain();

		StateParser sp = new SokobanOldToNewParser(domain);
		List<TrainingElement> trainingDataset = Model3Controller.getCommandsDataset(domain, DATASETTESTPATH, sp);

		Tokenizer tokenizer = new Tokenizer(true);
		tokenizer.addDelimiter("-");

		controller.setToBOWLanugageModel(trainingDataset, tokenizer, true);

		outputConstraintAndRFDistro(trainingDataset, controller);

	}


	public static void uniformNORTest(){

		Model3Controller controller = constructController();

		Domain domain = controller.getDomain();

		StateParser sp = new SokobanOldToNewParser(domain);
		List<TrainingElement> trainingDataset = Model3Controller.getCommandsDataset(domain, DATASETTESTPATH, sp);

		Tokenizer tokenizer = new Tokenizer(false);
		tokenizer.addDelimiter("-");

		controller.setToNORLanugageModel(trainingDataset, tokenizer, false);

		outputConstraintAndRFDistro(trainingDataset, controller);

	}


	protected static void outputConstraintAndRFDistro(List<TrainingElement> trainingDataset, Model3Controller controller){

		GenerativeModel gm = controller.getGM();

		TrainingElement te = trainingDataset.get(200);
		List<GMQueryResult> ldistro = controller.getLiftedRFAndBindingDistribution(te.trajectory.getState(0), te.command);
		for(GMQueryResult r : ldistro){
			LiftedVarValue lr = (LiftedVarValue)r.getQueryForVariable(gm.getRVarWithName(TaskModule.LIFTEDRFNAME));
			LiftedVarValue b = (LiftedVarValue)r.getQueryForVariable(gm.getRVarWithName(TaskModule.BINDINGNAME));

			System.out.printf("%.5f\t%s %s\n", r.probability, lr.toString(), b.toString());
		}

		System.out.println("===========================================================");

		List<GMQueryResult> rdistro = controller.getRFDistribution(te.trajectory.getState(0), te.command);
		for(GMQueryResult r : rdistro){
			RFConVariableValue gr = (RFConVariableValue)r.getQueryForVariable(gm.getRVarWithName(TaskModule.GROUNDEDRFNAME));


			System.out.printf("%.5f\t%s\n", r.probability, gr.toString());
		}


		System.out.println("Finished");

	}


	public static void verifyTrajectories(String datasetpath){

		Model3Controller controller = constructController();
		Domain domain = controller.getDomain();
		StateParser sp = new Sokoban2Parser(domain);

		List<TrainingElement> trainingDataset = Model3Controller.getCommandsDataset(domain, datasetpath, sp);
		int numFail = 0;
		for(TrainingElement te : trainingDataset){
			Trajectory t = te.trajectory;
			if(t.actions.size() != t.states.size()-1){
				numFail++;
				System.out.println("Failed on: " + te.identifier);
			}
			else{
				//System.out.println("Succeded on: " + te.identifier);
			}
		}

		System.out.println("Num failed: " + numFail + " out of " + trainingDataset.size());

	}


	public static void verifyRFLabels(String datasetpath){

		Model3Controller controller = constructController();
		Domain domain = controller.getDomain();
		StateParser sp = new Sokoban2Parser(domain);

		List<TrainingElement> trainingDataset = Model3Controller.getCommandsDataset(domain, datasetpath, sp);
		Map<String, String> trainingRFLabels = getJerryNormalRFLabels();


		int numFails = 0;
		int numTries = 0;
		for(TrainingElement te : trainingDataset){

			String ident = te.identifier;
			String labelStr = trainingRFLabels.get(ident);
			List <GroundedProp> translatedGPs = parseGPsFromString(labelStr, domain);
			//System.out.println(labelStr + " -> " + gpsToString(translatedGPs));
			if(!labelStr.equals(gpsToString(translatedGPs))){System.out.println("Fail!");}
			if(gpsSatisfied(translatedGPs, te.trajectory.getState(te.trajectory.numStates()-1))){
				System.out.println("Satisfied.");
			}
			else{
				System.out.println("Not satisfied.");
				numFails++;
			}
			numTries++;


		}

		System.out.println("Total fails " + numFails);
		System.out.println("Total tried: " + numTries);

	}


	public static void verifyIRL(String datasetpath, String cachePath){

		Model3Controller controller = constructController();
		Domain domain = controller.getDomain();
		GenerativeModel gm = controller.getGM();
		StateParser sp = new Sokoban2Parser(domain);

		Tokenizer tokenizer = new Tokenizer(true, true);
		tokenizer.addDelimiter("-");

		System.out.println("Getting training dataset");
		List<TrainingElement> trainingDataset = Model3Controller.getCommandsDataset(domain, datasetpath, sp);

		System.out.println("Getting RF labels");
		Map<String, String> trainingRFLabels = getJerryNormalRFLabels();

		if(cachePath != null && cachePath.length() > 0){
			System.out.println("Reading cache");
			TrajectoryModule tm = controller.getTrajectoryModule();
			tm.readCacheFromDisk(cachePath, domain, sp, controller.getHashingFactory());
		}

		System.out.println("Starting verification.");

		int nCorrect = 0;
		int nSoFar = 0;
		for(TrainingElement te : trainingDataset){
			List<GMQueryResult> dist = controller.getRFDistributionFromTrajectory(te.trajectory);
			GMQueryResult predicted = GMQueryResult.maxProb(dist);
			RFConVariableValue gr = (RFConVariableValue)predicted.getQueryForVariable(gm.getRVarWithName(TaskModule.GROUNDEDRFNAME));
			String grs = gr.toString().trim();

			String label = trainingRFLabels.get(te.identifier);

			nSoFar++;

			if(label.equals(grs.toString().trim())){
				nCorrect++;
				double acc = (double)nCorrect/(double)nSoFar;
				System.out.println("Correct (" + acc + " -- " + nSoFar + "): " + te.identifier);
			}
			else{
				double acc = (double)nCorrect/(double)nSoFar;
				System.out.println("Incorrect (" + acc + " -- " + nSoFar + "): " + te.identifier);
			}

		}

	}


	public static void writeIRLCache(String datasetpath, String cachePath){

		Model3Controller controller = constructController();
		Domain domain = controller.getDomain();
		GenerativeModel gm = controller.getGM();
		StateParser sp = new Sokoban2Parser(domain);

		Tokenizer tokenizer = new Tokenizer(true, true);
		tokenizer.addDelimiter("-");

		System.out.println("Getting training dataset");
		List<TrainingElement> trainingDataset = Model3Controller.getCommandsDataset(domain, datasetpath, sp);
		//List<TrainingElement> shortData = new ArrayList<TrainingElement>(1);
		//shortData.add(trainingDataset.get(0));



		int nSoFar = 0;
		for(TrainingElement te : trainingDataset){
			nSoFar++;
			System.out.println("Computing trajectories for " + nSoFar);
			controller.getRFDistributionFromTrajectory(te.trajectory);
		}

		System.out.println("Writing cache");
		TrajectoryModule tm = controller.getTrajectoryModule();
		tm.writeCacheToDisk(cachePath, domain, sp);

	}

	public static void checkCacheRead(String datasetpath, String cachePath){

		Model3Controller controller = constructController();
		Domain domain = controller.getDomain();
		GenerativeModel gm = controller.getGM();
		StateParser sp = new Sokoban2Parser(domain);

		Tokenizer tokenizer = new Tokenizer(true, true);
		tokenizer.addDelimiter("-");

		System.out.println("Getting training dataset");
		List<TrainingElement> trainingDataset = Model3Controller.getCommandsDataset(domain, datasetpath, sp);
		List<TrainingElement> shortData = new ArrayList<TrainingElement>(1);
		shortData.add(trainingDataset.get(0));

		TrajectoryModule tm = controller.getTrajectoryModule();
		tm.readCacheFromDisk(cachePath, domain, sp, controller.getHashingFactory());

		System.out.println("Getting RF labels");
		Map<String, String> trainingRFLabels = getJerryNormalRFLabels();



		TrainingElement te = trainingDataset.get(0);
		System.out.println("Analyzing: " + te.identifier);
		List<GMQueryResult> dist = controller.getRFDistributionFromTrajectory(te.trajectory);
		GMQueryResult predicted = GMQueryResult.maxProb(dist);
		RFConVariableValue gr = (RFConVariableValue)predicted.getQueryForVariable(gm.getRVarWithName(TaskModule.GROUNDEDRFNAME));
		String grs = gr.toString().trim();

		String label = trainingRFLabels.get(te.identifier);


		if(label.equals(grs.toString().trim())){

			System.out.println("Correct");
		}
		else{

			System.out.println("Incorrect");

			System.out.println("Actual: " + label);

			System.out.println("Distro:\n--------------------");



		}

		for(GMQueryResult rf : dist){
			if(rf.probability == predicted.probability){
				System.out.print("*");
			}
			System.out.println(rf.probability + ": " + rf.getQueryForVariable(gm.getRVarWithName(TaskModule.GROUNDEDRFNAME)).toString());
		}


	}


	public static void verifyIRLSpecific(String datasetpath){

		Model3Controller controller = constructController();
		Domain domain = controller.getDomain();
		GenerativeModel gm = controller.getGM();
		StateParser sp = new Sokoban2Parser(domain);

		Tokenizer tokenizer = new Tokenizer(true, true);
		tokenizer.addDelimiter("-");

		System.out.println("Getting training dataset");
		List<TrainingElement> trainingDataset = Model3Controller.getCommandsDataset(domain, datasetpath, sp);

		System.out.println("Getting RF labels");
		Map<String, String> trainingRFLabels = getJerryNormalRFLabels();


		TrainingElement te = trainingDataset.get(0);
		System.out.println("Analyzing: " + te.identifier);
		List<GMQueryResult> dist = controller.getRFDistributionFromTrajectory(te.trajectory);
		GMQueryResult predicted = GMQueryResult.maxProb(dist);
		RFConVariableValue gr = (RFConVariableValue)predicted.getQueryForVariable(gm.getRVarWithName(TaskModule.GROUNDEDRFNAME));
		String grs = gr.toString().trim();

		String label = trainingRFLabels.get(te.identifier);


		if(label.equals(grs.toString().trim())){

			System.out.println("Correct");
		}
		else{

			System.out.println("Incorrect");

			System.out.println("Actual: " + label);

			System.out.println("Distro:\n--------------------");



		}

		for(GMQueryResult rf : dist){
			if(rf.probability == predicted.probability){
				System.out.print("*");
			}
			System.out.println(rf.probability + ": " + rf.getQueryForVariable(gm.getRVarWithName(TaskModule.GROUNDEDRFNAME)).toString());
		}

	}


	public static void checkForIdenticalCommands(String datasetpath){

		Model3Controller controller = constructController();
		Domain domain = controller.getDomain();
		GenerativeModel gm = controller.getGM();
		StateParser sp = new Sokoban2Parser(domain);

		Tokenizer tokenizer = new Tokenizer(true, true);
		tokenizer.addDelimiter("-");

		System.out.println("Getting training dataset");
		List<TrainingElement> trainingDataset = Model3Controller.getCommandsDataset(domain, datasetpath, sp);

		Map<String, List<String>> commandsToFiles = new HashMap<String, List<String>>();
		for(TrainingElement te : trainingDataset){
			List<String> files = commandsToFiles.get(te.command);
			if(files == null){
				files = new ArrayList<String>();
				commandsToFiles.put(te.command, files);
			}
			files.add(te.identifier);
		}

		//now print by groups
		int numDups = 0;
		for(Map.Entry<String, List<String>> e : commandsToFiles.entrySet()){
			if(e.getValue().size() > 1) {
				System.out.println(e.getKey());
				for (String file : e.getValue()) {
					System.out.println(file);
				}
				System.out.println("-----------------------------");
				numDups += e.getValue().size()-1;
			}
		}

		System.out.println("Total command dups: " + numDups + " out of " + trainingDataset.size() + " examples.");

	}


	public static void getUniqueCommands(String datasetpath){

		Model3Controller controller = constructController();
		Domain domain = controller.getDomain();
		GenerativeModel gm = controller.getGM();
		StateParser sp = new Sokoban2Parser(domain);

		Tokenizer tokenizer = new Tokenizer(true, true);
		tokenizer.addDelimiter("-");

		System.out.println("Getting training dataset");
		List<TrainingElement> trainingDataset = Model3Controller.getCommandsDataset(domain, datasetpath, sp);

		Set<String> commands = new HashSet<String>();
		for(TrainingElement te : trainingDataset){
			if(!commands.contains(te.command)){
				System.out.println(te.command);
				commands.add(te.command);
			}

		}

	}


	public static void trajectoryTrainingTest(String datasetpath, String cachePath){


		Model3Controller controller = constructController();
		Domain domain = controller.getDomain();
		GenerativeModel gm = controller.getGM();
		StateParser sp = new Sokoban2Parser(domain);

		Tokenizer tokenizer = new Tokenizer(true, true);
		tokenizer.addDelimiter("-");

		System.out.println("Getting training dataset");
		List<TrainingElement> trainingDataset = Model3Controller.getCommandsDataset(domain, datasetpath, sp);
		//Map<String, String> trainingRFLabels =
		System.out.println("Getting RF labels");
		Map<String, String> trainingRFLabels = getJerryNormalRFLabels();

		if(cachePath != null && cachePath.length() > 0){
			System.out.println("Reading cache");
			TrajectoryModule tm = controller.getTrajectoryModule();
			tm.readCacheFromDisk(cachePath, domain, sp, controller.getHashingFactory());
		}

		System.out.println("Getting MT dataset");
		List<WeightedMTInstance> mtDataset = controller.getWeightedMTDatasetFromTrajectoryDataset(trainingDataset, tokenizer, 1.e-20);
		controller.setToMTLanugageModelUsingMTDataset(mtDataset, tokenizer, false);


		//now do learning
		System.out.println("Starting training.");
		MTEMModule mtem = new MTEMModule(mtDataset, gm);
		mtem.runEMManually(10);
		System.out.println("Finished training; beginning testing.");

		getAccuracyOnTrajectoryDataset(controller, trainingDataset, trainingRFLabels);
		//printWordParams((MTModule)controller.getGM().getModuleWithName(Model3Controller.LANGMODNAME));






	}

	public static void testMTWords(String datasetpath){

		Model3Controller controller = constructController();
		Domain domain = controller.getDomain();
		GenerativeModel gm = controller.getGM();
		StateParser sp = new SokobanOldToNewParser(domain);

		Tokenizer tokenizer = new Tokenizer(true, true);
		tokenizer.addDelimiter("-");

		List<TrainingElement> trainingDataset = Model3Controller.getCommandsDataset(domain, datasetpath, sp);
		//Map<String, String> trainingRFLabels = getOriginalDatasetRFLabels();
		List<WeightedMTInstance> mtDataset = controller.getWeightedMTDatasetFromTrajectoryDataset(trainingDataset, tokenizer, 1.e-20);

		controller.setToMTLanugageModelUsingMTDataset(mtDataset, tokenizer, false);
		MTModule module = (MTModule)controller.getGM().getModuleWithName(Model3Controller.LANGMODNAME);
		Set<String> naturalWords = module.getNaturalWords();
		for(String s : naturalWords){
			System.out.println(s);
		}

	}

	public static void getAccuracyOnTrajectoryDataset(Model3Controller controller, List<TrainingElement> trainingDataset, Map<String, String> rfLabels){

		GenerativeModel gm = controller.getGM();

		int c = 0;
		int n = 0;
		for(TrainingElement te : trainingDataset){

			String rfLabel = rfLabels.get(te.identifier);
			GMQueryResult predicted = GMQueryResult.maxProb(controller.getRFDistribution(te.trajectory.getState(0), te.command));
			RFConVariableValue gr = (RFConVariableValue)predicted.getQueryForVariable(gm.getRVarWithName(TaskModule.GROUNDEDRFNAME));
			String grs = gr.toString().trim();
			if(grs.equals(rfLabel)){
				c++;
				System.out.println("Correct: " + te.identifier);
			}
			else{
				System.out.println("Incorrect: " + te.identifier);
			}

			n++;
		}
		System.out.println(c + "/" + trainingDataset.size() + "; " + ((double)c/(double)trainingDataset.size()));


	}

	public static void trajectoryBOWTrainingTest(String datasetpath, String cachePath){

		Model3Controller controller = constructController();

		Domain domain = controller.getDomain();

		StateParser sp = new Sokoban2Parser(domain);
		List<TrainingElement> trainingDataset = Model3Controller.getCommandsDataset(domain, datasetpath, sp);

		if(cachePath != null && cachePath.length() > 0){
			System.out.println("Reading cache");
			TrajectoryModule tm = controller.getTrajectoryModule();
			tm.readCacheFromDisk(cachePath, domain, sp, controller.getHashingFactory());
		}


		Tokenizer tokenizer = new Tokenizer(true);
		tokenizer.addDelimiter("-");

		controller.setToBOWLanugageModel(trainingDataset, tokenizer, true);

		Dataset emDataset = controller.getEMDatasetFromTrajectoriesDataset(trainingDataset);
		EMAlgorithm em = new EMAlgorithm(controller.getGM(), emDataset);
		BagOfWordsEMModule bowEMMod = new BagOfWordsEMModule(controller.getHashingFactory());
		em.addEMModule(bowEMMod);

		BagOfWordsModule bowMod = (BagOfWordsModule)controller.getGM().getModuleWithName(Model3Controller.LANGMODNAME);
		//bowMod.printWordParams();

		//System.out.println("Beginning Training...");
		em.runEM(10);
		//setParamsForSimpleSoko(bowMod);
		//System.out.println("Finished Training; beginning testing");
		//bowMod.printWordParams();

		Map<String, String> trainingRFLabels = getJerryNormalRFLabels();
		//Map<String, String> trainingRFLabels = getOriginalDatasetRFLabels();
		//Map<String, String> trainingRFLabels = getSimpleDatasetRFLabels();
		getAccuracyOnTrajectoryDataset(controller, trainingDataset, trainingRFLabels);

	}

	public static void trajectoryNORTrainingTest(){

		Model3Controller controller = constructController();

		Domain domain = controller.getDomain();

		StateParser sp = new SokobanOldToNewParser(domain);
		List<TrainingElement> trainingDataset = Model3Controller.getCommandsDataset(domain, DATASETTESTPATH, sp);

		Tokenizer tokenizer = new Tokenizer(true);
		tokenizer.addDelimiter("-");

		controller.setToNORLanugageModel(trainingDataset, tokenizer, true);

		Dataset emDataset = controller.getEMDatasetFromTrajectoriesDataset(trainingDataset);
		EMAlgorithm em = new EMAlgorithm(controller.getGM(), emDataset);
		NoisyOrEMModule norEMMod = new NoisyOrEMModule(controller.getHashingFactory());
		em.addEMModule(norEMMod);

		NoisyOr norMod = (NoisyOr)controller.getGM().getModuleWithName(Model3Controller.LANGMODNAME);
		//norMod.printWordParams();

		System.out.println("Beginning Training...");
		em.runEM(10);
		//setParamsForSimpleSoko(bowMod);
		System.out.println("Finished Training; beginning testing");
		//norMod.printWordParams();

		Map<String, String> trainingRFLabels = getOriginalDatasetRFLabels();
		//Map<String, String> trainingRFLabels = getSimpleDatasetRFLabels();
		getAccuracyOnTrajectoryDataset(controller, trainingDataset, trainingRFLabels);

	}


	public static void parallelLOOOutput(String [] args){

		if(args.length != 3){
			System.out.println("Format:\n\tpathToDatasetDir pathToOutputdir instanceToTest");
			System.exit(0);
		}

		String pathToDatasetDir = args[0];
		String pathToOutputDir = args[1];
		int instanceToTest = Integer.parseInt(args[2]);

		if(!pathToOutputDir.endsWith("/")){
			pathToOutputDir = pathToOutputDir + "/";
		}

		System.out.println("Setting up...");

		Model3Controller controller = constructController();
		Domain domain = controller.getDomain();
		GenerativeModel gm = controller.getGM();
		StateParser sp = new Sokoban2Parser(domain);

		Tokenizer tokenizer = new Tokenizer(true, true);
		tokenizer.addDelimiter("-");

		List<TrainingElement> fullTrajectoryDataset = Model3Controller.getCommandsDataset(domain, pathToDatasetDir, sp);
		List<TrainingElement> looDataset = looTrajectoryDataset(fullTrajectoryDataset, instanceToTest);
		TrainingElement testInstance = fullTrajectoryDataset.get(instanceToTest);
		String outputPathName = pathToOutputDir + testInstance.identifier;


		BufferedWriter out = null;

		try {
			out = new BufferedWriter(new FileWriter(outputPathName));
		} catch (IOException e) {
			e.printStackTrace();
		}

		if(out == null){
			throw new RuntimeException("Could not open output file");
		}

		List<WeightedMTInstance> mtDataset = controller.getWeightedMTDatasetFromTrajectoryDataset(looDataset, tokenizer, 1.e-20);
		controller.setToMTLanugageModelUsingMTDataset(mtDataset, tokenizer, false);

		System.out.println("Training...");

		MTEMModule mtem = new MTEMModule(mtDataset, gm);
		mtem.runEMManually(10);

		System.out.println("Testing " + testInstance.identifier);

		GMQueryResult predicted = GMQueryResult.maxProb(controller.getRFDistribution(testInstance.trajectory.getState(0), testInstance.command));
		RFConVariableValue gr = (RFConVariableValue)predicted.getQueryForVariable(gm.getRVarWithName(TaskModule.GROUNDEDRFNAME));
		String grs = gr.toString().trim();

		try {
			out.write(grs + "\n");
		} catch (IOException e1) {
			e1.printStackTrace();
		}


		try {
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("Finished");
	}


	public static void parallelBOWLOOOutput(String [] args){

		if(args.length != 3 && args.length != 4){
			System.out.println("Format:\n\tpathToDatasetDir pathToOutputdir instanceToTest [cacheFilePath]");
			System.exit(0);
		}

		String pathToDatasetDir = args[0];
		String pathToOutputDir = args[1];
		int instanceToTest = Integer.parseInt(args[2]);

		if(!pathToOutputDir.endsWith("/")){
			pathToOutputDir = pathToOutputDir + "/";
		}

		String cachePath = null;
		if(args.length == 4){
			cachePath = args[3];
		}

		System.out.println("Setting up...");

		Model3Controller controller = constructController();
		Domain domain = controller.getDomain();
		GenerativeModel gm = controller.getGM();
		StateParser sp = new Sokoban2Parser(domain);

		Tokenizer tokenizer = new Tokenizer(true, true);
		tokenizer.addDelimiter("-");

		List<TrainingElement> fullTrajectoryDataset = Model3Controller.getCommandsDataset(domain, pathToDatasetDir, sp);
		List<TrainingElement> looDataset = looTrajectoryDataset(fullTrajectoryDataset, instanceToTest);
		TrainingElement testInstance = fullTrajectoryDataset.get(instanceToTest);
		String outputPathName = pathToOutputDir + testInstance.identifier;



		if(cachePath != null && cachePath.length() > 0){
			System.out.println("Reading cache");
			TrajectoryModule tm = controller.getTrajectoryModule();
			tm.readCacheFromDisk(cachePath, domain, sp, controller.getHashingFactory());
		}



		BufferedWriter out = null;

		try {
			out = new BufferedWriter(new FileWriter(outputPathName));
		} catch (IOException e) {
			e.printStackTrace();
		}

		if(out == null){
			throw new RuntimeException("Could not open output file");
		}

		controller.setToBOWLanugageModel(looDataset, tokenizer, true);

		Dataset emDataset = controller.getEMDatasetFromTrajectoriesDataset(looDataset);
		EMAlgorithm em = new EMAlgorithm(controller.getGM(), emDataset);
		BagOfWordsEMModule bowEMMod = new BagOfWordsEMModule(controller.getHashingFactory());
		em.addEMModule(bowEMMod);

		em.runEM(10);

		System.out.println("Testing " + testInstance.identifier);

		GMQueryResult predicted = GMQueryResult.maxProb(controller.getRFDistribution(testInstance.trajectory.getState(0), testInstance.command));
		RFConVariableValue gr = (RFConVariableValue)predicted.getQueryForVariable(gm.getRVarWithName(TaskModule.GROUNDEDRFNAME));
		String grs = gr.toString().trim();

		try {
			out.write(grs + "\n");
		} catch (IOException e1) {
			e1.printStackTrace();
		}


		try {
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("Finished");

	}


	public static void parallelNORLOOOutput(String [] args){

		if(args.length != 3){
			System.out.println("Format:\n\tpathToDatasetDir pathToOutputdir instanceToTest");
			System.exit(0);
		}

		String pathToDatasetDir = args[0];
		String pathToOutputDir = args[1];
		int instanceToTest = Integer.parseInt(args[2]);

		if(!pathToOutputDir.endsWith("/")){
			pathToOutputDir = pathToOutputDir + "/";
		}

		System.out.println("Setting up...");

		Sokoban2Domain dg = new Sokoban2Domain();
		Domain domain = dg.generateDomain();
		StateHashFactory hashingFactory = new NameDependentStateHashFactory();
		StateParser sp = new SokobanOldToNewParser(domain);
		List<GPConjunction> liftedTaskDescriptions = new ArrayList<GPConjunction>(2);

		GPConjunction atr = new GPConjunction();
		atr.addGP(new GroundedProp(domain.getPropFunction(Sokoban2Domain.PFAGENTINROOM), new String[]{"a", "r"}));
		liftedTaskDescriptions.add(atr);

		GPConjunction btr = new GPConjunction();
		btr.addGP(new GroundedProp(domain.getPropFunction(Sokoban2Domain.PFBLOCKINROOM), new String[]{"b", "r"}));
		liftedTaskDescriptions.add(btr);

		Model3Controller controller = new Model3Controller(domain, liftedTaskDescriptions, hashingFactory, true);
		GenerativeModel gm = controller.getGM();

		Tokenizer tokenizer = new Tokenizer(true, true);
		tokenizer.addDelimiter("-");

		List<TrainingElement> fullTrajectoryDataset = Model3Controller.getCommandsDataset(domain, pathToDatasetDir, sp);
		Map<String, String> trainingRFLabels = getOriginalDatasetRFLabels();
		List<TrainingElement> looDataset = looTrajectoryDataset(fullTrajectoryDataset, instanceToTest);
		TrainingElement testInstance = fullTrajectoryDataset.get(instanceToTest);
		String outputPathName = pathToOutputDir + testInstance.identifier;


		BufferedWriter out = null;

		try {
			out = new BufferedWriter(new FileWriter(outputPathName));
		} catch (IOException e) {
			e.printStackTrace();
		}

		if(out == null){
			throw new RuntimeException("Could not open output file");
		}

		controller.setToNORLanugageModel(looDataset, tokenizer, true);

		Dataset emDataset = controller.getEMDatasetFromTrajectoriesDataset(looDataset);
		EMAlgorithm em = new EMAlgorithm(controller.getGM(), emDataset);
		NoisyOrEMModule norEMMod = new NoisyOrEMModule(controller.getHashingFactory());
		em.addEMModule(norEMMod);

		em.runEM(10);

		System.out.println("Testing " + testInstance.identifier);

		GMQueryResult predicted = GMQueryResult.maxProb(controller.getRFDistribution(testInstance.trajectory.getState(0), testInstance.command));
		RFConVariableValue gr = (RFConVariableValue)predicted.getQueryForVariable(gm.getRVarWithName(TaskModule.GROUNDEDRFNAME));
		String grs = gr.toString().trim();

		try {
			out.write(grs + "\n");
		} catch (IOException e1) {
			e1.printStackTrace();
		}


		try {
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("Finished");

	}


	public static void trajectoryToWeightedMTDataset(){

		Sokoban2Domain dg = new Sokoban2Domain();
		Domain domain = dg.generateDomain();
		StateHashFactory hashingFactory = new NameDependentStateHashFactory();
		StateParser sp = new SokobanOldToNewParser(domain);
		List<GPConjunction> liftedTaskDescriptions = new ArrayList<GPConjunction>(2);

		GPConjunction atr = new GPConjunction();
		atr.addGP(new GroundedProp(domain.getPropFunction(Sokoban2Domain.PFAGENTINROOM), new String[]{"a", "r"}));
		liftedTaskDescriptions.add(atr);

		GPConjunction btr = new GPConjunction();
		btr.addGP(new GroundedProp(domain.getPropFunction(Sokoban2Domain.PFBLOCKINROOM), new String[]{"b", "r"}));
		liftedTaskDescriptions.add(btr);

		Model3Controller controller = new Model3Controller(domain, liftedTaskDescriptions, hashingFactory, true);
		GenerativeModel gm = controller.getGM();

		Tokenizer tokenizer = new Tokenizer(true, true);
		tokenizer.addDelimiter("-");

		List<TrainingElement> trainingDataset = Model3Controller.getCommandsDataset(domain, DATASETTESTPATH, sp);

		List<WeightedMTInstance> mtDataset = controller.getWeightedMTDatasetFromTrajectoryDataset(trainingDataset, tokenizer, 1.e-20);


		for(int i = 230; i < 240; i++){
			WeightedMTInstance inst = mtDataset.get(i);
			System.out.println(inst.naturalCommand.toString() + "\n----------------------");
			for(WeightedSemanticCommandPair wsc : inst){
				System.out.println(wsc.prob + ": " + wsc.semanticCommand);
			}
			System.out.println("");
		}


	}

	public static void rfFromTrajectoryDistributionTest(){

		Sokoban2Domain dg = new Sokoban2Domain();
		Domain domain = dg.generateDomain();
		StateHashFactory hashingFactory = new DiscreteStateHashFactory();
		StateParser sp = new SokobanOldToNewParser(domain);
		List<GPConjunction> liftedTaskDescriptions = new ArrayList<GPConjunction>(2);

		GPConjunction atr = new GPConjunction();
		atr.addGP(new GroundedProp(domain.getPropFunction(Sokoban2Domain.PFAGENTINROOM), new String[]{"a", "r"}));
		liftedTaskDescriptions.add(atr);

		GPConjunction btr = new GPConjunction();
		btr.addGP(new GroundedProp(domain.getPropFunction(Sokoban2Domain.PFBLOCKINROOM), new String[]{"b", "r"}));
		liftedTaskDescriptions.add(btr);

		Model3Controller controller = new Model3Controller(domain, liftedTaskDescriptions, hashingFactory, true);
		GenerativeModel gm = controller.getGM();

		Tokenizer tokenizer = new Tokenizer(true, true);
		tokenizer.addDelimiter("-");

		List<TrainingElement> trainingDataset = Model3Controller.getCommandsDataset(domain, DATASETTESTPATH, sp);

		/*
		for(TrainingElement te : trainingDataset){
			List<GMQueryResult> rdistro = controller.getRFDistributionFromTrajectory(te.trajectory);
			System.out.println(te.command + "\n-------------------------");
			for(GMQueryResult r : rdistro){
				RFConVariableValue gr = (RFConVariableValue)r.getQueryForVariable(gm.getRVarWithName(TaskModule.GROUNDEDRFNAME));
				System.out.printf("%f\t%s\n", r.probability, gr.toString());
			}
			System.out.println("");
		}
		*/

		for(int i = 0; i < trainingDataset.size(); i+=40){
			TrainingElement te = trainingDataset.get(i);
			List<GMQueryResult> rdistro = controller.getRFDistributionFromTrajectory(te.trajectory);
			System.out.println(te.command + "\n-------------------------");
			for(GMQueryResult r : rdistro){
				RFConVariableValue gr = (RFConVariableValue)r.getQueryForVariable(gm.getRVarWithName(TaskModule.GROUNDEDRFNAME));
				System.out.printf("%f\t%s\n", r.probability, gr.toString());
			}
			System.out.println("");
		}
		/*
		TrainingElement te = trainingDataset.get(0);
		List<GMQueryResult> rdistro = controller.getRFDistributionFromTrajectory(te.trajectory);
		System.out.println(te.command + "\n-------------------------");
		for(GMQueryResult r : rdistro){
			RFConVariableValue gr = (RFConVariableValue)r.getQueryForVariable(gm.getRVarWithName(TaskModule.GROUNDEDRFNAME));
			System.out.printf("%f\t%s\n", r.probability, gr.toString());
		}
		System.out.println("");
		*/

	}















	public static List<TrainingElement> looTrajectoryDataset(List<TrainingElement> sourceDataset, int i){
		List<TrainingElement> looDataset = new ArrayList<TrainingElement>(sourceDataset);
		looDataset.remove(i);
		return looDataset;
	}

	public static void printWordParams(MTModule mtmod){

		Set<String> semWords = mtmod.getSemanticWords();
		Set<String> natWords = mtmod.getNaturalWords();
		WordParam wp = mtmod.getWp();

		for(String s : semWords){
			System.out.println(s+"\n--------------------------------------");
			for(String n : natWords){
				double param = wp.prob(n, s);
				if(param > -1.){
					System.out.println(param + " " + n);
				}
			}
		}

	}


	public static Map<String, String> getOriginalDatasetRFLabels(){

		Map<String, String> labels = new HashMap<String, String>();

		addLabelMappingForRange(labels, "agent2orange_", "txt", 0, 40, "agentInRoom(agent0, room1)");
		addLabelMappingForRange(labels, "agent2tan_", "txt", 0, 40, "agentInRoom(agent0, room0)");
		addLabelMappingForRange(labels, "agent2teal_", "txt", 0, 40, "agentInRoom(agent0, room2)");
		addLabelMappingForRange(labels, "star2orange_", "txt", 0, 40, "blockInRoom(block0, room1)");
		addLabelMappingForRange(labels, "star2tan_", "txt", 0, 40, "blockInRoom(block0, room0)");
		addLabelMappingForRange(labels, "star2teal_", "txt", 0, 40, "blockInRoom(block0, room2)");


		return labels;
	}

	public static Map<String, String> getSimpleDatasetRFLabels(){

		Map<String, String> labels = new HashMap<String, String>();

		addLabelMappingForRange(labels, "agentToBlue", "txt", 0, 40, "agentInRoom(agent0, room1)");
		addLabelMappingForRange(labels, "agentToGreen", "txt", 0, 40, "agentInRoom(agent0, room0)");
		addLabelMappingForRange(labels, "agentToRed", "txt", 0, 40, "agentInRoom(agent0, room2)");
		addLabelMappingForRange(labels, "blockToBlue", "txt", 0, 40, "blockInRoom(block0, room1)");
		addLabelMappingForRange(labels, "blockToGreen", "txt", 0, 40, "blockInRoom(block0, room0)");
		addLabelMappingForRange(labels, "blockToRed", "txt", 0, 40, "blockInRoom(block0, room2)");


		return labels;
	}

	public static Map<String, String> getJerryNormalRFLabels(){

		Map<String, String> labels = new HashMap<String, String>();

		addLabelMappingForRange(labels, "block1ToRight_", "txt", 1, 21, "blockInRoom(block1, room2)");
		addLabelMappingForRange(labels, "block1ToBottom_", "txt", 1, 21, "blockInRoom(block1, room0)");
		addLabelMappingForRange(labels, "agentToLeft_", "txt", 1, 21, "agentInRoom(agent0, room1)");
		addLabelMappingForRange(labels, "agentToBottom_", "txt", 1, 21, "agentInRoom(agent0, room0)");
		addLabelMappingForRange(labels, "blockToLeft_", "txt", 1, 21, "blockInRoom(block0, room1)");
		addLabelMappingForRange(labels, "blockToRight_", "txt", 1, 21, "blockInRoom(block0, room2)");
		addLabelMappingForRange(labels, "blockToBottom_", "txt", 1, 21, "blockInRoom(block0, room0)");
		addLabelMappingForRange(labels, "agent2LNblock2R_", "txt", 1, 21, "agentInRoom(agent0, room1) blockInRoom(block0, room2)");
		addLabelMappingForRange(labels, "agent2RNblock2L_", "txt", 1, 21, "agentInRoom(agent0, room2) blockInRoom(block0, room1)");


		return labels;

	}

	protected static void addLabelMappingForRange(Map<String, String> labels, String baseIdentifier, String extension, int rangeStart, int rangeMax, String label){

		for(int i = rangeStart; i < rangeMax; i++){
			String name = baseIdentifier + i + "." + extension;
			labels.put(name, label);
		}

	}


	public static void getAverageNumberOfWordsInTrajDataset(){

		Sokoban2Domain dg = new Sokoban2Domain();
		Domain domain = dg.generateDomain();
		StateParser sp = new SokobanOldToNewParser(domain);

		Tokenizer tokenizer = new Tokenizer(true, true);
		tokenizer.addDelimiter("-");

		List<TrainingElement> dataset = Model3Controller.getCommandsDataset(domain, DATASETTESTPATH, sp);

		int sum = 0;
		for(TrainingElement te : dataset){
			TokenedString ts = tokenizer.tokenize(te.command);
			sum += ts.size();
		}

		double avgNum = (double)sum / (double)dataset.size();

		System.out.println("Average number of words: " + avgNum);


	}


	public static void getLatexDatasetTable(){

		Sokoban2Domain dg = new Sokoban2Domain();
		Domain domain = dg.generateDomain();
		StateParser sp = new SokobanOldToNewParser(domain);

		Tokenizer tokenizer = new Tokenizer(true, true);
		tokenizer.addDelimiter("-");

		List<TrainingElement> dataset = Model3Controller.getCommandsDataset(domain, DATASETTESTPATH, sp);

		for(TrainingElement te : dataset){
			String command = te.command.replaceAll(" \\.", ".");
			command = command.replaceAll(" ,", ",");
			command = command.replaceAll("&", "\\&");
			System.out.println(command + " & " + latexTaskMap(te.identifier) + " \\\\ \\hline");
		}

	}

	protected static String latexTaskMap(String input){
		if(input.startsWith("agent2orange")){
			return "${\\tt agentInRoom}(?a, ?r) \\land {\\tt roomIsOrange}(?r)$";
		}
		else if(input.startsWith("agent2tan")){
			return "${\\tt agentInRoom}(?a, ?r) \\land {\\tt roomIsTan}(?r)$";
		}
		else if(input.startsWith("agent2teal")){
			return "${\\tt agentInRoom}(?a, ?r) \\land {\\tt roomIsTeal(?r)}$";
		}
		else if(input.startsWith("star2orange")){
			return "${\\tt blockInRoom}(?b, ?r) \\land {\\tt itemIsStar}(?b) \\land {\\tt roomIsOrange}(?r)$";
		}
		else if(input.startsWith("star2tan")){
			return "${\\tt blockInRoom}(?b, ?r) \\land {\\tt itemIsStar}(?b) \\land {\\tt roomIsTan}(?r)$";
		}
		else if(input.startsWith("star2teal")){
			return "${\\tt blockInRoom}(?b, ?r) \\land {\\tt itemIsStar}(?b) \\land {\\tt roomIsTeal}(?r)$";
		}

		throw new RuntimeException("Could not map task");
	}




	public static List<GroundedProp> parseGPsFromString(String str, Domain domain){

		String [] gpComps = str.split("\\) ");
		List<GroundedProp> gps = new ArrayList<GroundedProp>();
		for(String gp : gpComps){
			gps.add(parseGPFromString(gp, domain));
		}

		return gps;

	}

	public static GroundedProp parseGPFromString(String str, Domain domain){
		if(!str.endsWith(")")){
			str = str + ")";
		}

		int lParenInd = str.indexOf("(");
		String pName = str.substring(0, lParenInd);

		String paramString = str.substring(lParenInd+1, str.length()-1);

		String [] params = paramString.split(", ");

		GroundedProp gp = new GroundedProp(domain.getPropFunction(pName), params);


		return gp;


	}


	public static String gpsToString(List<GroundedProp> gps){

		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < gps.size(); i++){
			if(i > 0){sb.append(" ");}
			sb.append(gps.get(i).toString());
		}

		return sb.toString();

	}


	public static boolean gpsSatisfied(List<GroundedProp> gps, State s){
		for(GroundedProp gp :gps){
			if(!gp.isTrue(s)){
				return false;
			}
		}

		return true;
	}

}
