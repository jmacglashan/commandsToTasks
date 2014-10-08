package commands.tests;

import generativemodel.GMQueryResult;
import generativemodel.GenerativeModel;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.statehashing.NameDependentStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.GroundedProp;

import commands.data.TrainingElement;
import commands.model3.GPConjunction;
import commands.model3.Model3Controller;
import commands.model3.TaskModule;
import commands.model3.TaskModule.LiftedVarValue;
import commands.model3.TaskModule.RFConVariableValue;
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
import domain.singleagent.sokoban2.SokobanOldToNewParser;
import em.Dataset;
import em.EMAlgorithm;

public class Model3ControllerTest {

	public static String 						DATASETTESTPATH = "data/allTurkTrain";
	//public static String 						DATASETTESTPATH = "data/allTurkTrainLimitedCommand";
	//public static String 						DATASETTESTPATH = "data/mySimpleSokoData";
	public static String						MTDATASETPATH = "data/allTurkSemanticLabeled";
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		//getAverageNumberOfWordsInTrajDataset();
		//getLatexDatasetTable();
		
		//uniformTest();
		trajectoryTrainingTest(DATASETTESTPATH);
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
		//trajectoryBOWTrainingTest();
		//trajectoryNORTrainingTest();

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
	
	public static void trajectoryTrainingTest(String datasetpath){
		
		
		Model3Controller controller = constructController();
		Domain domain = controller.getDomain();
		GenerativeModel gm = controller.getGM();
		StateParser sp = new SokobanOldToNewParser(domain);
		
		Tokenizer tokenizer = new Tokenizer(true, true);
		tokenizer.addDelimiter("-");
		
		List<TrainingElement> trainingDataset = Model3Controller.getCommandsDataset(domain, datasetpath, sp);
		Map<String, String> trainingRFLabels = getOriginalDatasetRFLabels();
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
		Map<String, String> trainingRFLabels = getOriginalDatasetRFLabels();
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
	
	public static void trajectoryBOWTrainingTest(){
		
		Model3Controller controller = constructController();
		
		Domain domain = controller.getDomain();
		
		StateParser sp = new SokobanOldToNewParser(domain);
		List<TrainingElement> trainingDataset = Model3Controller.getCommandsDataset(domain, DATASETTESTPATH, sp);
		
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
		
		Map<String, String> trainingRFLabels = getOriginalDatasetRFLabels();
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
	
	public static void strictMTTest(){
		
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
		
		Tokenizer tokenizer = new Tokenizer(true);
		tokenizer.addDelimiter("-");
		
		List<WeightedMTInstance> mtDataset = Model3Controller.getStrictMTDataset(tokenizer, MTDATASETPATH, "txt");
		controller.setToMTLanugageModelUsingMTDataset(mtDataset, tokenizer, true);
		
		
		List<GMQueryResult> distro = controller.getSemanticSentenceDistributionUsingStrictMT(mtDataset.get(0).naturalCommand.toString());
		for(GMQueryResult d : distro){
			System.out.println(d.probability + ": " + d.getSingleQueryVar().toString());
		}
		System.out.println("-------------------------\nMax:");
		GMQueryResult mxQuery = GMQueryResult.maxProb(distro);
		System.out.println(mxQuery.probability + ": " + mxQuery.getSingleQueryVar().toString());
		
	}
	
	
	public static void strictMTTrainingTest(){
		
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
		
		Tokenizer tokenizer = new Tokenizer(true);
		tokenizer.addDelimiter("-");
		
		List<WeightedMTInstance> mtDataset = Model3Controller.getStrictMTDataset(tokenizer, MTDATASETPATH, "txt");
		controller.setToMTLanugageModelUsingMTDataset(mtDataset, tokenizer, true);
		
		
		//printWordParams((MTModule)controller.getGM().getModuleWithName(Model3Controller.LANGMODNAME));
		
		
		//now do learning
		MTEMModule mtem = new MTEMModule(mtDataset, gm);
		mtem.runEMManually(10);
		
		//System.out.println("==========================================================");
		//System.out.println("==========================================================");
		//System.out.println("==========================================================");
		
		//printWordParams((MTModule)controller.getGM().getModuleWithName(Model3Controller.LANGMODNAME));
		
		
		int c = 0;
		int pc = 0;
		for(WeightedMTInstance inst : mtDataset){
			GMQueryResult query = GMQueryResult.maxProb(controller.getSemanticSentenceDistributionUsingStrictMT(inst.naturalCommand.toString()));
			double predictedProb = query.probability;
			TokenedString predicted = tokenizer.tokenize(query.getSingleQueryVar().toString());
			TokenedString actualLabel = inst.weightedSemanticCommands.get(0).semanticCommand; //assuming only one label in our strict MT dataset
			
			if(predicted.equals(actualLabel)){
				System.out.println("Correct: " + inst.naturalCommand.toString() + " -> " + predicted.toString() + "::" + predictedProb + "\n");
				c++;
				pc++;
			}
			else{
				String incLabel = "Incorrect";
				if(predicted.t(1).equals(actualLabel.t(1)) && predicted.t(4).equals(actualLabel.t(4))){
					incLabel = "Pseudo";
					pc++;
				}
				
				System.out.println(incLabel + ": " + inst.naturalCommand.toString() + " -> " + predicted.toString() + "::" + predictedProb);
				System.out.println("Actual: " + actualLabel.toString() + "\n");
			}
			
		}
		
		System.out.println("correct: " + c + "/" + mtDataset.size() + "; " + ((double)c/(double)mtDataset.size()));
		System.out.println("pesudeo correct: " + pc + "/" + mtDataset.size() + "; " + ((double)pc/(double)mtDataset.size()));
		
		
		
	}
	
	public static void strictMTLOOTest(){
		
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
		
		Tokenizer tokenizer = new Tokenizer(true, true);
		tokenizer.addDelimiter("-");
		
		List<WeightedMTInstance> sourceDataset = Model3Controller.getStrictMTDataset(tokenizer, MTDATASETPATH, "txt");
		
		int c = 0;
		int pc = 0;
		for(int i = 0; i < sourceDataset.size(); i++){
			WeightedMTInstance inst = sourceDataset.get(i);
			
			//setup controller for learning
			List<WeightedMTInstance> looDataset = looMTDataset(sourceDataset, i);
			Model3Controller controller = new Model3Controller(domain, liftedTaskDescriptions, hashingFactory, true);
			GenerativeModel gm = controller.getGM();
			controller.setToMTLanugageModelUsingMTDataset(looDataset, tokenizer, true);
			
			//run learning
			MTEMModule mtem = new MTEMModule(looDataset, gm);
			mtem.runEMManually(10);
			
			
			//test it
			GMQueryResult query = GMQueryResult.maxProb(controller.getSemanticSentenceDistributionUsingStrictMT(inst.naturalCommand.toString()));
			double predictedProb = query.probability;
			TokenedString predicted = tokenizer.tokenize(query.getSingleQueryVar().toString());
			TokenedString actualLabel = inst.weightedSemanticCommands.get(0).semanticCommand; //assuming only one label in our strict MT dataset
			
			if(predicted.equals(actualLabel)){
				System.out.println("Correct: " + inst.naturalCommand.toString() + " -> " + predicted.toString() + "::" + predictedProb + "\n");
				c++;
				pc++;
			}
			else{
				String incLabel = "Incorrect";
				if(predicted.t(1).equals(actualLabel.t(1)) && predicted.t(4).equals(actualLabel.t(4))){
					incLabel = "Pseudo";
					pc++;
				}
				
				System.out.println(incLabel + ": " + inst.naturalCommand.toString() + " -> " + predicted.toString() + "::" + predictedProb);
				System.out.println("Actual: " + actualLabel.toString() + "\n");
			}
			
			
		}
		
		System.out.println("correct: " + c + "/" + sourceDataset.size() + "; " + ((double)c/(double)sourceDataset.size()));
		System.out.println("pesudeo correct: " + pc + "/" + sourceDataset.size() + "; " + ((double)pc/(double)sourceDataset.size()));
		
		
		
	}
	
	
	public static void parameterTest(){
		
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
		
		List<WeightedMTInstance> mtDataset = Model3Controller.getStrictMTDataset(tokenizer, MTDATASETPATH, "txt");
		controller.setToMTLanugageModelUsingMTDataset(mtDataset, tokenizer, true);
		
		
		//printWordParams((MTModule)controller.getGM().getModuleWithName(Model3Controller.LANGMODNAME));
		
		
		//now do learning
		MTEMModule mtem = new MTEMModule(mtDataset, gm);
		mtem.runEMManually(10);
		
		MTModule mtmod = (MTModule)gm.getModuleWithName(Model3Controller.LANGMODNAME);
		//mtmod.printLPParams();
		System.out.println("Starting");
		printWordParams((MTModule)controller.getGM().getModuleWithName(Model3Controller.LANGMODNAME));
		
	}
	
	
	
	public static List<WeightedMTInstance> looMTDataset(List<WeightedMTInstance> sourceDataset, int i){
		List<WeightedMTInstance> looDataset = new ArrayList<WeightedMTInstance>(sourceDataset);
		looDataset.remove(i);
		return looDataset;
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
	
	public static void setParamsForSimpleSoko(BagOfWordsModule bowMod){
		
		Set<String> naturalWords = bowMod.getNaturalWords();
		commands.model3.bagofwords.BagOfWordsModule.WordParam wp = bowMod.getWp();
		
		setParams(wp, "agentInRoom", naturalWords, "leave","enter","arrive","go");
		setParams(wp, "roomIsRed", naturalWords, "rose","cherry","red","crimson");
		setParams(wp, "blockInRoom", naturalWords, "send","block","push","tow","piece","star","take","box");
		setParams(wp, "roomIsBlue", naturalWords, "cobalt","sapphire","blue","navy");
		//setParams(wp, "######", naturalWords, "to","for","at","the");
		setParams(wp, "roomIsGreen", naturalWords, "olive","emerald","green","jade");
		
		
	}
	
	protected static void setParams(commands.model3.bagofwords.BagOfWordsModule.WordParam wp, String topic, Set<String> naturalWords, String...wordsToUse){
		Set<String> wordsToKeep = new HashSet<String>();
		for(String w : wordsToUse){
			wordsToKeep.add(w);
		}
		
		double uni = 1./(double)wordsToKeep.size();
		for(String w : naturalWords){
			if(!wordsToKeep.contains(w)){
				wp.set(0., w, topic);
			}else{
				wp.set(uni, w, topic);
			}
		}
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
	

}
