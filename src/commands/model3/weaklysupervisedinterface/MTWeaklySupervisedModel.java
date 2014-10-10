package commands.model3.weaklysupervisedinterface;

import burlap.oomdp.core.GroundedProp;
import commands.model3.StringValue;
import commands.model3.TaskModule;
import commands.model3.mt.MTModule;
import commands.model3.mt.Tokenizer;
import commands.model3.mt.em.MTEMModule;
import commands.model3.mt.em.WeightedMTInstance;
import generativemodel.GMQuery;
import generativemodel.GMQueryResult;
import generativemodel.GenerativeModel;
import generativemodel.RVariable;
import logicalexpressions.LogicalExpression;
import logicalexpressions.PFAtom;

import java.util.*;

/**
 * @author James MacGlashan.
 */
public class MTWeaklySupervisedModel implements WeaklySupervisedLanguageModel{

	public static final String		LANGMODNAME = "langMod";


	protected WeaklySupervisedController controller;
	protected Tokenizer tokenizer;

	protected RVariable naturalCommandVariable;

	protected int numEMIterations;



	public MTWeaklySupervisedModel(WeaklySupervisedController controller, Tokenizer tokenizer, int numEMIterations){

		this.controller = controller;
		this.tokenizer = tokenizer;
		this.numEMIterations = numEMIterations;

	}

	@Override
	public double probabilityOfCommand(LogicalExpression liftedTask, LogicalExpression bindingConstraints, String command) {

		GenerativeModel gm = this.controller.getGM();
		TaskModule.LiftedVarValue liftedTaskVal = new TaskModule.LiftedVarValue(gm.getRVarWithName(TaskModule.LIFTEDRFNAME), this.extractGPs(liftedTask));
		TaskModule.LiftedVarValue bindingConstraintsVal = new TaskModule.LiftedVarValue(gm.getRVarWithName(TaskModule.BINDINGNAME), this.extractGPs(bindingConstraints));
		StringValue sVal = new StringValue(command, this.naturalCommandVariable);

		GMQuery nCommandQuery = new GMQuery();
		nCommandQuery.addQuery(sVal);
		nCommandQuery.addCondition(liftedTaskVal);
		nCommandQuery.addCondition(bindingConstraintsVal);

		GMQueryResult langQR = gm.getProb(nCommandQuery, true);
		double p = langQR.probability;

		return p;
	}

	@Override
	public void learnFromDataset(List<WeaklySupervisedTrainingInstance> dataset) {

		GenerativeModel gm = this.controller.getGM();

		List<WeightedMTInstance> mtDataset = this.generateMTDataset(dataset);

		Set<String> semanticWords = new HashSet<String>();
		Set<String> naturalWords = new HashSet<String>();

		int maxSemanticCommandLength = getSemanticWordsFromMTDataset(mtDataset, semanticWords);
		int maxNaturalCommandLength = getNaturalWordsFromMTDataset(mtDataset, naturalWords);

		MTModule langMod = new MTModule(LANGMODNAME, gm.getRVarWithName(TaskModule.LIFTEDRFNAME), gm.getRVarWithName(TaskModule.BINDINGNAME),
				semanticWords, naturalWords, maxSemanticCommandLength, maxNaturalCommandLength, tokenizer);

		gm.addGMModule(langMod);

		this.naturalCommandVariable = gm.getRVarWithName(MTModule.NNAME);

		MTEMModule mtem = new MTEMModule(mtDataset, gm);
		mtem.runEMManually(this.numEMIterations);

	}



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
	protected static int getSemanticWordsFromMTDataset(List<WeightedMTInstance> dataset, Set<String> semWords){

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

	}


	/**
	 * Fills in up a provided set with the natural words and returns the maximum semantic command length
	 * @param dataset the input MT dataset
	 * @param natWords the set to fill with the natural words
	 * @return the maximum natural command length
	 */
	protected static int getNaturalWordsFromMTDataset(List<WeightedMTInstance> dataset, Set<String> natWords){

		int maxLength = 0;
		for(WeightedMTInstance wi : dataset){
			maxLength = Math.max(maxLength, wi.naturalCommand.size());
			for(int i = 1; i <= wi.naturalCommand.size(); i++){
				natWords.add(wi.naturalCommand.t(i));
			}
		}

		return maxLength;

	}


	protected List<GroundedProp> extractGPs(LogicalExpression exp){
		List<GroundedProp> gps = new ArrayList<GroundedProp>(exp.getChildExpressions().size());
		for(LogicalExpression atom : exp.getChildExpressions()){
			GroundedProp gp = ((PFAtom)atom).getGroundedProp();
			gps.add(gp);
		}

		return gps;
	}

}
