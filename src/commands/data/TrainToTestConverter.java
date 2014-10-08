package commands.data;

import java.util.ArrayList;
import java.util.List;

import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Domain;

public class TrainToTestConverter {

	public static TestElement convertToTest(TrainingElement te, String testId){
		
		TestElement tste = new TestElement(testId, te.trajectory.getState(0), te.command);
		return tste;
		
	}
	
	public static List<TestElement> convertTrainDatasetToTest(List <TrainingElement> dataset){
		
		List <String> testIds = new ArrayList<String>(dataset.size());
		for(int i = 0; i < dataset.size(); i++){
			testIds.add("Test" + i + ".txt");
		}
		return convertTrainDatasetToTest(dataset, testIds);
		
	}
	
	public static List<TestElement> convertTrainDatasetToTest(List <TrainingElement> dataset, List <String> testIds){
		List <TestElement> tes = new ArrayList<TestElement>(dataset.size());
		for(int i = 0; i < dataset.size(); i++){
			tes.add(convertToTest(dataset.get(i), testIds.get(i)));
		}
		return tes;
	}
	
	
	
	public static void createTestDatasetFilesFromTrainingDatasetFiles(Domain d, StateParser sp, String pathToTrain, String pathToTest){
		
		TrainingElementParser tep = new TrainingElementParser(d, sp);
		List <TrainingElement> dataset = tep.getTrainingElementDataset(pathToTrain, ".txt");
		List <TestElement> testData = convertTrainDatasetToTest(dataset);
		
		if(!pathToTest.endsWith("/")){
			pathToTest = pathToTest + "/";
		}
		
		TestElementParser tsp = new TestElementParser(sp);
		
		for(TestElement te : testData){
			tsp.writeTestElementToFile(te, pathToTest + te.testIdenitifier);
		}
		
	}
	
	
	
}
