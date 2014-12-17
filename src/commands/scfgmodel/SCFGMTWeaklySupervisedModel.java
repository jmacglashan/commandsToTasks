package commands.scfgmodel;

import generativemodel.RVariable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import logicalexpressions.LogicalExpression;
import logicalexpressions.PFAtom;

import commands.model3.mt.Tokenizer;
import commands.model3.mt.em.WeightedMTInstance;
import commands.model3.mt.em.WeightedMTInstance.WeightedSemanticCommandPair;
import commands.model3.weaklysupervisedinterface.WeaklySupervisedController;
import commands.model3.weaklysupervisedinterface.WeaklySupervisedLanguageModel;
import commands.model3.weaklysupervisedinterface.WeaklySupervisedTrainingInstance;

/**
 * An SCFG Machine Translate language model that is trained through weak supervision.
 * @author Abhinav Bajaj
 */
public class SCFGMTWeaklySupervisedModel implements WeaklySupervisedLanguageModel{

	public static final String		LANGMODNAME 			= "langMod";
	public static final String		NEW_LINE 				= "\n";

	public static final String		MOSES_TRAIN_DIR			= "moses-training";
	public static final String		COMM_FILE_NAME 			= "dataset";
	public static final String		SEM_COMM_FILE_EXT 		= ".sem";
	public static final String		NAT_COMM_FILE_EXT 		= ".nat";
	public static final String		TEST_TRANLATION_FILE	= "test.test";

	public static final String		MOSES_SCRIPT 			= "./runMoses.sh";
	public static final String		MOSES_TRANS_SCRIPT 		= "./runMosesTranslation.sh";
	public static final String		MOSES_SCRIPT_OUTPUT 	= "moses.out";
	public static final String		MOSES_TRANS_OUPTUT_DIR		= "/export/projects/nlpeducation/nlpcommands/mosesmodel/datastore/moses.2012-02-06/training/model";
	public static final String		MOSES_TRANS_SCORE_FILE		= "scores.out";

	protected WeaklySupervisedController controller;
	protected Tokenizer tokenizer;

	protected RVariable naturalCommandVariable;

	protected int numEMIterations;


	/**
	 * Initializes. The {@link commands.model3.weaklysupervisedinterface.WeaklySupervisedController} is required
	 * because the MT model will add a module to the controller's {@link generativemodel.GenerativeModel} to perform
	 * inference.
	 * @param controller the controller with the {@link generativemodel.GenerativeModel} that will be extended
	 * @param tokenizer a {@link commands.model3.mt.Tokenizer} for tokenizing input strings.
	 * @param numEMIterations the number of training EM iterations that will be performed when the {@link #learnFromDataset(java.util.List)} method is called.
	 */
	public SCFGMTWeaklySupervisedModel(WeaklySupervisedController controller, Tokenizer tokenizer, int numEMIterations){

		this.controller = controller;
		this.tokenizer = tokenizer;
		this.numEMIterations = numEMIterations;

	}
	
	
	public static void main(String args[]){
		String line = "0 ||| agentInRoom agent room .  ||| d: 0 -0.336472 0 0 -0.336472 0 0 lm: -43.3132 -41.4966 w: -4 tm: -7.81035 -28.8375 -1.09861 -3.22836 0.999896 ||| -125.399";
		String parts[] = line.split("\\|\\|\\|");
		for(String part : parts)
		{
			System.out.println("==" +part +"+++");
		}
		System.out.println(parts[1].trim());
		System.out.println(parts[parts.length-1].trim());
		Double a = Math.exp(Double.parseDouble(parts[parts.length-1].trim()));
		System.out.println(a);
	}

	@Override
	public double probabilityOfCommand(LogicalExpression liftedTask, LogicalExpression bindingConstraints, String command) {
		System.out.println("Command -- > " + command);

		// Create the test.test file
		PrintWriter testWriter = null;
		try {
			File natCommFile = new File(MOSES_TRAIN_DIR, TEST_TRANLATION_FILE);
			if(natCommFile.exists())
				natCommFile.delete();

			testWriter = new PrintWriter(natCommFile);
			testWriter.write(command.replace(".","").trim() + NEW_LINE);
			testWriter.close();

			// call moses transation
			runMoses(MOSES_TRANS_SCRIPT);

			String machineLanguage = this.getMachineLanguageString(liftedTask, bindingConstraints);
			System.out.println("======================================");
			System.out.println("Machine -> " + machineLanguage);
			
			// Load Moses output && Calculate probability score
			return getScoreFromMosesOutput(machineLanguage);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out.println("Exitted due to exception -" + e.getMessage());
			System.exit(-1);
			
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Exitted due to exception -" + e.getMessage());
			System.exit(-1);
			
		}
		finally {
			if(testWriter!=null)
				testWriter.close();
		}
		return 0;
	}

	private double getScoreFromMosesOutput(String machineLanguage) throws IOException {

		BufferedReader br = null;

		try {

			String line;

			br = new BufferedReader(new FileReader(new File(MOSES_TRANS_OUPTUT_DIR, MOSES_TRANS_SCORE_FILE)));
			while ((line = br.readLine()) != null) {
				//System.out.println(line);
				
				String parts[] = line.split("\\|\\|\\|");
				String semComm = parts[1].trim();
				//System.out.println(semComm);
				String score = parts[parts.length-1];
				//System.out.println(score);
				if(semComm.equals(machineLanguage)) {
					return Math.exp(Double.parseDouble(score));
				}
			}
			//TODO Value to be returned in machinelanguage does not match any moses output
			System.out.println("MachineLang not found, returning 0");
			return 0.0;
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		} finally {
			if (br != null)
				br.close();
		}
	}

	@Override
	public void learnFromDataset(List<WeaklySupervisedTrainingInstance> dataset) {

		List<WeightedMTInstance> mtDataset = this.generateMTDataset(dataset);

		// Create the files here
		PrintWriter natCommWriter = null;
		PrintWriter semCommWriter = null;
		try {
			File natCommFile = new File(MOSES_TRAIN_DIR, COMM_FILE_NAME+NAT_COMM_FILE_EXT);
			File semCommFile = new File(MOSES_TRAIN_DIR, COMM_FILE_NAME+SEM_COMM_FILE_EXT);
			if(natCommFile.exists())
				natCommFile.delete();
			if(semCommFile.exists())
				semCommFile.delete();

			natCommWriter = new PrintWriter(natCommFile);
			semCommWriter = new PrintWriter(semCommFile);

			// write the training data to files
			for(WeightedMTInstance wMTObj: mtDataset) {
				String natComm = wMTObj.naturalCommand.toString();
				for(WeightedSemanticCommandPair semComm : wMTObj) {
					semCommWriter.write(semComm.semanticCommand.toString() + NEW_LINE);
					natCommWriter.write(natComm + NEW_LINE);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		finally {
			if(natCommWriter!=null)
				natCommWriter.close();
			if(semCommWriter!=null)
				semCommWriter.close();
		}
		System.out.println("Created the nat and sem files");

		// RUN MOSES
		runMoses(MOSES_SCRIPT);
	}

	private boolean runMoses(String script) {
		ProcessBuilder processBuilder = new ProcessBuilder(script);
		File log = new File(MOSES_SCRIPT_OUTPUT);
		processBuilder.redirectErrorStream(true);
		processBuilder.redirectOutput(Redirect.to(log));
		Process p;
		try {
			System.out.println("Starting "+script+" Execution");
			p = processBuilder.start();
			p.waitFor();

			System.out.println("Execution Complete");
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return false;

	}

	/**
	 * Returns the {@link commands.model3.mt.em.WeightedMTInstance} dataset wrapper that our MT model prefers. This dataset
	 * requires natural language expressions and all possible machine language expressions that are paired with it and their weight.
	 * This means the logical expressions are converted deterministically to machine language expressions.
	 * @param wsDataset the input weakly supervised dataset consisting of logical expression, natural language expressions, and their weakly supervised weight.
	 * @return the {@link commands.model3.mt.em.WeightedMTInstance} dataset wrapper
	 */
	protected List<WeightedMTInstance> generateMTDataset(List<WeaklySupervisedTrainingInstance> wsDataset){

		List<WeightedMTInstance> mtDataset = new ArrayList<WeightedMTInstance>(wsDataset.size());
		Map<String, WeightedMTInstance> instances = new HashMap<String, WeightedMTInstance>(wsDataset.size());
		for(WeaklySupervisedTrainingInstance wsi : wsDataset){

			WeightedMTInstance instance = instances.get(wsi.command);
			if(instance == null){
				instance = new WeightedMTInstance(this.tokenizer.tokenize(wsi.command));
				instances.put(wsi.command, instance);
				mtDataset.add(instance);
			}

			String machineLanguage = this.getMachineLanguageString(wsi.liftedTask, wsi.bindingConstraints);
			instance.addWeightedSemanticCommand(this.tokenizer.tokenize(machineLanguage), wsi.weight);

		}

		return mtDataset;
	}


	/**
	 * Takes task and binding constrained logical expressions and turns it into a machine language expression.
	 * This method assumes that the children of the logical expressions are terminal {@link logicalexpressions.PFAtom}
	 * objects.
	 * @param liftedTask the lifted task
	 * @param bindingConstraints the object binding constraints
	 * @return the machine language expression.
	 */
	protected String getMachineLanguageString(LogicalExpression liftedTask, LogicalExpression bindingConstraints){

		//assume flat list of PF Atoms for now
		StringBuilder sb = new StringBuilder();

		for(int i = 0; i < liftedTask.getChildExpressions().size(); i++){
			if(i > 0){
				sb.append(" ");
			}
			PFAtom atom = (PFAtom)liftedTask.getChildExpressions().get(i);
			sb.append(atom.getGroundedProp().pf.getName());
			String [] pfParams = atom.getGroundedProp().pf.getParameterClasses();
			for(String p : pfParams){
				sb.append(" ").append(p);
			}
		}

		for(int i = 0; i < bindingConstraints.getChildExpressions().size(); i++){
			sb.append(" ");
			PFAtom atom = (PFAtom)bindingConstraints.getChildExpressions().get(i);
			sb.append(atom.getGroundedProp().pf.getName());
			String [] pfParams = atom.getGroundedProp().pf.getParameterClasses();
			for(String p : pfParams){
				sb.append(" ").append(p);
			}
		}
		return sb.toString();
	}


	/**
	 * Fills in up a provided set with the semantic words and returns the maximum semantic command length
	 * @param dataset the input MT dataset
	 * @param semWords the set to fill with the semantic words
	 * @return the maximum semantic command length
	 */
	/*protected static int getSemanticWordsFromMTDataset(List<WeightedMTInstance> dataset, Set<String> semWords){

		int maxLength = 0;
		for(WeightedMTInstance wi : dataset){
			for(WeightedMTInstance.WeightedSemanticCommandPair wsc : wi){
				maxLength = Math.max(maxLength, wsc.semanticCommand.size());
				for(int i = 0; i <= wsc.semanticCommand.size(); i++){
					semWords.add(wsc.semanticCommand.t(i));
				}
			}
		}

		return maxLength;

	}*/


	/**
	 * Fills in up a provided set with the natural words and returns the maximum semantic command length
	 * @param dataset the input MT dataset
	 * @param natWords the set to fill with the natural words
	 * @return the maximum natural command length
	 */
	/*protected static int getNaturalWordsFromMTDataset(List<WeightedMTInstance> dataset, Set<String> natWords){

		int maxLength = 0;
		for(WeightedMTInstance wi : dataset){
			maxLength = Math.max(maxLength, wi.naturalCommand.size());
			for(int i = 1; i <= wi.naturalCommand.size(); i++){
				natWords.add(wi.naturalCommand.t(i));
			}
		}

		return maxLength;

	}
*/

	/**
	 * Extracts the {@link burlap.oomdp.core.GroundedProp} objects from the logical expression by assuming that
	 * each child is a {@link logicalexpressions.PFAtom}.
	 * @param exp the input {@link logicalexpressions.LogicalExpression}
	 * @return all {@link burlap.oomdp.core.GroundedProp} objects.
	 */
	/*protected List<GroundedProp> extractGPs(LogicalExpression exp){
		List<GroundedProp> gps = new ArrayList<GroundedProp>(exp.getChildExpressions().size());
		for(LogicalExpression atom : exp.getChildExpressions()){
			GroundedProp gp = ((PFAtom)atom).getGroundedProp();
			gps.add(gp);
		}

		return gps;
	}*/
}
