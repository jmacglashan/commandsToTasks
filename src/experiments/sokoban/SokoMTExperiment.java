package experiments.sokoban;


import burlap.oomdp.core.states.State;
import commands.data.TrainingElement;
import commands.model3.TaskModule;
import commands.model3.mt.Tokenizer;
import commands.model3.weaklysupervisedinterface.MTWeaklySupervisedModel;
import commands.model3.weaklysupervisedinterface.WeaklySupervisedController;
import generativemodel.GMQueryResult;
import generativemodel.GenerativeModel;
import generativemodel.RVariableValue;

import java.util.*;

/**
 * @author James MacGlashan.
 */
public class SokoMTExperiment {

	public static void main(String [] args){

		/////////////////////////////////////////////////////////////
		// FOR AMT FULL DATASET TRAINING TEST USE THE BELOW
		////////////////////////////////////////////////////////////
		trainingTest(true, SokobanControllerConstructor.AMTFULLDATASET);
		//trainingTest(false, SokobanControllerConstructor.EXPERTDATASET);

		//savedModelTest(true, SokobanControllerConstructor.AMTFULLDATASET, "langModelParams.yaml");
		//savedModelTest(false, SokobanControllerConstructor.EXPERTDATASET, "expertLangModelParams.yaml");

		//savedModelTaskProbs(true, SokobanControllerConstructor.AMTFULLDATASET, "amtLangModelParams.yaml");

		//testWeaklySupervisedDataWrite(true, SokobanControllerConstructor.AMTFULLDATASET);


		/////////////////////////////////////////////////////////////
		// FOR CHACHING AMT FULL DATASET IRL USE THE BELOW (create directory on file system first)
		////////////////////////////////////////////////////////////
		//cacheIRLResultsFor(true, SokobanControllerConstructor.AMTFULLDATASET, "data/amtFullTrajectoryCache");


		/////////////////////////////////////////////////////////////
		// FOR AMT FULL DATASET TRAINING TEST USE THE BELOW
		////////////////////////////////////////////////////////////
		//trainingTest(true, SokobanControllerConstructor.AMTFULLDATASET, "data/amtFullTrajectoryCache");


		/////////////////////////////////////////////////////////////
		// FOR AMT FULL DATASET LEAVE ONE OUT TEST USE THE BELOW
		////////////////////////////////////////////////////////////
		//LOOTest(true, SokobanControllerConstructor.AMTFULLDATASET, "data/amtFullTrajectoryCache");


	}



	public static void trainingTest(boolean isAMT, String pathToDataset){

		//sokoban training task definition
		SokobanControllerConstructor constructor;
		if(isAMT){
			constructor = new SokobanControllerConstructor(false, true);
		}
		else{
			constructor = new SokobanControllerConstructor(true, false);
		}


		//get our controller
		WeaklySupervisedController controller = constructor.generateNewController();

		//instantiate our MT language model
		createAndAddLanguageModel(controller);

		//get training data
		List<TrainingElement> dataset = constructor.getTrainingDataset(pathToDataset);

		//instantiate the weakly supervised language model dataset using IRL
		controller.createWeaklySupervisedTrainingDatasetFromTrajectoryDataset(dataset);

		//perform learning
		controller.trainLanguageModel();

		//evaluate results
		if(isAMT) {
			evaluatePerformanceOnDataset(controller, dataset, constructor.getTurkDatasetRFLabels());
		}
		else{
			evaluatePerformanceOnDataset(controller, dataset, constructor.getExpertDatasetRFLabels());
		}

		//controller.dumpLanguageMode("expertLangModelParams.yaml");
	}

	public static void testWeaklySupervisedDataWrite(boolean isAMT, String pathToDataset){

		//sokoban training task definition
		SokobanControllerConstructor constructor;
		if(isAMT){
			constructor = new SokobanControllerConstructor(false, true);
		}
		else{
			constructor = new SokobanControllerConstructor(true, false);
		}


		//get our controller
		WeaklySupervisedController controller = constructor.generateNewController();

		//instantiate our MT language model
		createAndAddLanguageModel(controller);

		//get training data
		List<TrainingElement> dataset = constructor.getTrainingDataset(pathToDataset);
		dataset = dataset.subList(0,1);

		//instantiate the weakly supervised language model dataset using IRL
		controller.createWeaklySupervisedTrainingDatasetFromTrajectoryDataset(dataset);

		controller.writeWeaklySupervisedData("exampleWS.yaml");

	}

	public static void savedModelTest(boolean isAMT, String pathToDataset, String pathToLanguageModel){

		//sokoban training task definition
		SokobanControllerConstructor constructor;
		if(isAMT){
			constructor = new SokobanControllerConstructor(false, true);
		}
		else{
			constructor = new SokobanControllerConstructor(true, false);
		}


		//get our controller
		WeaklySupervisedController controller = constructor.generateNewController();

		//instantiate our MT language model
		createAndAddMTModel(controller, pathToLanguageModel);

		//get training data
		List<TrainingElement> dataset = constructor.getTrainingDataset(pathToDataset);


		//evaluate results
		if(isAMT) {
			evaluatePerformanceOnDataset(controller, dataset, constructor.getTurkDatasetRFLabels());
		}
		else{
			evaluatePerformanceOnDataset(controller, dataset, constructor.getExpertDatasetRFLabels());
		}

	}


	public static void savedModelTaskProbs(boolean isAMT, String pathToDataset, String pathToLanguageModel){

		//sokoban training task definition
		SokobanControllerConstructor constructor;
		if(isAMT){
			constructor = new SokobanControllerConstructor(false, true);
		}
		else{
			constructor = new SokobanControllerConstructor(true, false);
		}


		//get our controller
		WeaklySupervisedController controller = constructor.generateNewController();

		//instantiate our MT language model
		createAndAddMTModel(controller, pathToLanguageModel);

		//get training data
		List<TrainingElement> dataset = constructor.getTrainingDataset(pathToDataset);


		for(TrainingElement te : dataset) {
			String command = te.command;
			State state = te.trajectory.getState(0);
			System.out.println(command);
			List<GMQueryResult> distro = controller.getTaskDescriptionDistribution(state, command);
			Collections.sort(distro, new Comparator<GMQueryResult>() {
				@Override
				public int compare(GMQueryResult o1, GMQueryResult o2) {
					return -1*Double.compare(o1.probability, o2.probability);
				}
			});


			for(GMQueryResult result : distro) {
				System.out.println(result.probability + ": " + taskDescription(controller, result));
			}
			System.out.println("");
		}



	}

	protected static String taskDescription(WeaklySupervisedController controller, GMQueryResult result){

		RVariableValue lifted = result.getQueryForVariable(controller.getGM().getRVarWithName(TaskModule.LIFTEDRFNAME));
		RVariableValue bindings = result.getQueryForVariable(controller.getGM().getRVarWithName(TaskModule.BINDINGNAME));

		return lifted.toString() + " " + bindings.toString();

	}

	public static void trainingTest(boolean isAMT, String pathToDataset, String pathToIRLCache){

		//sokoban training task definition
		SokobanControllerConstructor constructor;
		if(isAMT){
			constructor = new SokobanControllerConstructor(false, true);
		}
		else{
			constructor = new SokobanControllerConstructor(true, false);
		}

		//get our controller
		WeaklySupervisedController controller = constructor.generateNewController();

		//instantiate our MT language model
		createAndAddLanguageModel(controller);

		//get training data
		List<TrainingElement> dataset = constructor.getTrainingDataset(pathToDataset);

		//load our IRL trajectory cache for fast IRL
		controller.loadIRLProbabiltiesFromDisk(pathToIRLCache, constructor.cacheStateParser);

		//instantiate the weakly supervised language model dataset using IRL
		controller.createWeaklySupervisedTrainingDatasetFromTrajectoryDataset(dataset);

		//perform learning
		controller.trainLanguageModel();

		//evaluate results
		if(isAMT) {
			evaluatePerformanceOnDataset(controller, dataset, constructor.getTurkDatasetRFLabels());
		}
		else{
			evaluatePerformanceOnDataset(controller, dataset, constructor.getExpertDatasetRFLabels());
		}
	}


	public static void LOOTest(boolean isAMT, String pathToDataset, String pathToIRLCache){

		//sokoban training task definition
		SokobanControllerConstructor constructor;
		Map<String, String> rfLabels;
		if(isAMT){
			constructor = new SokobanControllerConstructor(false, true);
			rfLabels = constructor.getTurkDatasetRFLabels();
		}
		else{
			constructor = new SokobanControllerConstructor(true, false);
			rfLabels = constructor.getExpertDatasetRFLabels();
		}

		//get source training data
		List<TrainingElement> dataset = constructor.getTrainingDataset(pathToDataset);



		//start LOO loop
		int nc = 0;
		for(int i = 0; i < dataset.size(); i++){

			List<TrainingElement> trainingDataset = new ArrayList<TrainingElement>(dataset);
			trainingDataset.remove(i);

			//get our controller
			WeaklySupervisedController controller = constructor.generateNewController();

			//instantiate our MT language model
			createAndAddLanguageModel(controller);

			//load our IRL trajectory cache for fast IRL
			controller.loadIRLProbabiltiesFromDisk(pathToIRLCache, constructor.cacheStateParser);

			//instantiate the weakly supervised language model dataset using IRL
			controller.createWeaklySupervisedTrainingDatasetFromTrajectoryDataset(trainingDataset);

			//perform learning
			controller.trainLanguageModel();

			//test it
			GenerativeModel gm = controller.getGM();
			TrainingElement queryElement = dataset.get(i);
			String rfLabel = rfLabels.get(queryElement.identifier);
			List<GMQueryResult> rfDist = controller.getRFDistribution(queryElement.trajectory.getState(0), queryElement.command);
			GMQueryResult predicted = GMQueryResult.maxProb(rfDist);

			TaskModule.RFConVariableValue gr = (TaskModule.RFConVariableValue)predicted.getQueryForVariable(gm.getRVarWithName(TaskModule.GROUNDEDRFNAME));
			String grs = gr.toString().trim();
			if(grs.equals(rfLabel)){
				nc++;
				System.out.println("Correct: " + queryElement.identifier);
			}
			else{
				System.out.println("Incorrect: " + queryElement.identifier);
			}

		}

		double accuracy = (double)nc/(double)dataset.size();
		System.out.println(nc + "/" + dataset.size() + "; " + accuracy);

	}



	public static void cacheIRLResultsFor(boolean isAMT, String pathToDataset, String pathToCacheDirectory){

		//sokoban training task definition
		SokobanControllerConstructor constructor;
		if(isAMT){
			constructor = new SokobanControllerConstructor(false, true);
		}
		else{
			constructor = new SokobanControllerConstructor(true, false);
		}

		//get our controller
		WeaklySupervisedController controller = constructor.generateNewController();

		//instantiate our MT language model
		createAndAddLanguageModel(controller);

		//get training data
		List<TrainingElement> dataset = constructor.getTrainingDataset(pathToDataset);

		//instantiate the weakly supervised language model dataset using IRL
		controller.createWeaklySupervisedTrainingDatasetFromTrajectoryDataset(dataset);

		controller.cacheIRLProbabilitiesToDisk(pathToCacheDirectory, constructor.sp);

	}



	public static void createAndAddLanguageModel(WeaklySupervisedController controller){
		createAndAddMTModel(controller);
	}

	public static void createAndAddMTModel(WeaklySupervisedController controller){
		//setup language model
		Tokenizer tokenizer = new Tokenizer(true, true);
		tokenizer.addDelimiter("-");
		MTWeaklySupervisedModel model = new MTWeaklySupervisedModel(controller, tokenizer, 10);

		//set our controller to use the MT model we created
		controller.setLanguageModel(model);
	}

	public static void createAndAddMTModel(WeaklySupervisedController controller, String path){
		//setup language model
		Tokenizer tokenizer = new Tokenizer(true, true);
		tokenizer.addDelimiter("-");
		MTWeaklySupervisedModel model = new MTWeaklySupervisedModel(controller, tokenizer, 10);
		System.out.println("Loading model");
		model.loadModel(path);
		System.out.println("Finished loading");

		//set our controller to use the MT model we created
		controller.setLanguageModel(model);
	}


	public static void evaluatePerformanceOnDataset(WeaklySupervisedController controller,
													List<TrainingElement> dataset, Map<String, String> rfLabels){

		GenerativeModel gm = controller.getGM();

		int c = 0;
		int n = 0;
		for(TrainingElement te : dataset) {

			String rfLabel = rfLabels.get(te.identifier);
			List<GMQueryResult> rfDist = controller.getRFDistribution(te.trajectory.getState(0), te.command);
			GMQueryResult predicted = GMQueryResult.maxProb(rfDist);

			TaskModule.RFConVariableValue gr = (TaskModule.RFConVariableValue)predicted.getQueryForVariable(gm.getRVarWithName(TaskModule.GROUNDEDRFNAME));
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
		System.out.println(c + "/" + dataset.size() + "; " + ((double)c/(double)n));

	}


}
