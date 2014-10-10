package experiments.sokoban;

import burlap.behavior.statehashing.NameDependentStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.GroundedProp;
import commands.data.TrainingElement;
import commands.data.TrainingElementParser;
import commands.model3.GPConjunction;
import commands.model3.weaklysupervisedinterface.WeaklySupervisedController;
import domain.singleagent.sokoban2.Sokoban2Domain;
import domain.singleagent.sokoban2.Sokoban2Parser;
import domain.singleagent.sokoban2.SokobanOldToNewParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author James MacGlashan.
 */
public class SokobanControllerConstructor {

	public static final String EXPERDATASETNODUPS = "data/jerryNormalNoNoiseNoDups";
	public static final String EXPERTDATASET = "data/jerryNormalNoNoise";

	public static final String AMTFULLDATASET = "data/allTurkTrain";
	public static final String AMTLIMITEDDATASET = "data/allTurkTrainLimitedCommand";


	public Sokoban2Domain domainGenerator;
	public Domain domain;
	public StateHashFactory hashingFactory;
	public List<GPConjunction> liftedTaskDescriptions;
	public StateParser sp;
	public StateParser cacheStateParser;


	public SokobanControllerConstructor(boolean includeBlockAndAgentGoal, boolean useOldToNewSokobanStateParser){

		this.domainGenerator = new Sokoban2Domain();
		this.domain = this.domainGenerator.generateDomain();
		this.hashingFactory = new NameDependentStateHashFactory();
		this.liftedTaskDescriptions = new ArrayList<GPConjunction>(2);

		GPConjunction atr = new GPConjunction();
		atr.addGP(new GroundedProp(domain.getPropFunction(Sokoban2Domain.PFAGENTINROOM), new String[]{"a", "r"}));
		this.liftedTaskDescriptions.add(atr);

		GPConjunction btr = new GPConjunction();
		btr.addGP(new GroundedProp(domain.getPropFunction(Sokoban2Domain.PFBLOCKINROOM), new String[]{"b", "r"}));
		this.liftedTaskDescriptions.add(btr);

		if(includeBlockAndAgentGoal) {
			//jerry extensions
			GPConjunction abtr = new GPConjunction();
			abtr.addGP(new GroundedProp(domain.getPropFunction(Sokoban2Domain.PFAGENTINROOM), new String[]{"a", "r1"}));
			abtr.addGP(new GroundedProp(domain.getPropFunction(Sokoban2Domain.PFBLOCKINROOM), new String[]{"b", "r2"}));
			this.liftedTaskDescriptions.add(abtr);
		}

		if(useOldToNewSokobanStateParser){
			this.sp = new SokobanOldToNewParser(this.domain);
			this.cacheStateParser = new Sokoban2Parser(this.domain);
		}
		else{
			this.sp = new Sokoban2Parser(this.domain);
			this.cacheStateParser = sp;
		}


	}


	public WeaklySupervisedController generateNewController(){
		WeaklySupervisedController controller = new WeaklySupervisedController(this.domain, liftedTaskDescriptions, hashingFactory, true);
		return controller;
	}


	public List<TrainingElement> getTrainingDataset(String pathToDatasetDir){
		TrainingElementParser teparser = new TrainingElementParser(this.domain, this.sp);
		List<TrainingElement> dataset = teparser.getTrainingElementDataset(pathToDatasetDir, ".txt");
		return dataset;
	}


	public Map<String, String> getExpertDatasetRFLabels(){
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

	public Map<String, String> getTurkDatasetRFLabels(){
		Map<String, String> labels = new HashMap<String, String>();

		addLabelMappingForRange(labels, "agent2orange_", "txt", 0, 40, "agentInRoom(agent0, room1)");
		addLabelMappingForRange(labels, "agent2tan_", "txt", 0, 40, "agentInRoom(agent0, room0)");
		addLabelMappingForRange(labels, "agent2teal_", "txt", 0, 40, "agentInRoom(agent0, room2)");
		addLabelMappingForRange(labels, "star2orange_", "txt", 0, 40, "blockInRoom(block0, room1)");
		addLabelMappingForRange(labels, "star2tan_", "txt", 0, 40, "blockInRoom(block0, room0)");
		addLabelMappingForRange(labels, "star2teal_", "txt", 0, 40, "blockInRoom(block0, room2)");


		return labels;
	}


	protected static void addLabelMappingForRange(Map<String, String> labels, String baseIdentifier, String extension, int rangeStart, int rangeMax, String label){

		for(int i = rangeStart; i < rangeMax; i++){
			String name = baseIdentifier + i + "." + extension;
			labels.put(name, label);
		}

	}

}
