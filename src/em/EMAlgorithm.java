package em;

import generativemodel.GenerativeModel;
import generativemodel.RVariableValue;

import java.util.ArrayList;
import java.util.List;

import burlap.debugtools.DPrint;

public class EMAlgorithm {

	protected GenerativeModel			generativeModel;
	protected List<EMModule>			emmodules;
	protected List <EMAuxiliaryCode>	aux;
	protected Dataset					dataset;
	
	public 	int							debugCode = 8446;
	
	
	public EMAlgorithm(GenerativeModel gm) {
		this.generativeModel = gm;
		this.emmodules = new ArrayList<EMModule>();
		this.aux = new ArrayList<EMAuxiliaryCode>();
	}
	
	public EMAlgorithm(GenerativeModel gm, Dataset ds) {
		this.generativeModel = gm;
		this.emmodules = new ArrayList<EMModule>();
		this.dataset = ds;
		this.aux = new ArrayList<EMAuxiliaryCode>();
	}
	
	
	public void setDataset(Dataset ds){
		this.dataset = ds;
	}
	
	public void addEMModule(EMModule m){
		this.emmodules.add(m);
		m.setGenerativeModelSrc(generativeModel);
	}
	
	public void addAux(EMAuxiliaryCode ac){
		this.aux.add(ac);
	}
	
	
	public void runEM(int nIterations){
		
		for(int i = 0; i < nIterations; i++){
			DPrint.cl(debugCode, "Starting E Pass: " + i);
			this.runEPass();
			DPrint.cl(debugCode, "Starting M Step: " + i);
			this.runMStep();
		}
		DPrint.cl(debugCode, "Finished " + nIterations + " EM Iterations");
		
	}
	
	
	
	public void runEPass(){
		for(EMAuxiliaryCode ac : this.aux){
			ac.preEStep();
		}
		for(int i = 0; i < dataset.size(); i++){
			this.runEStep(i, dataset.getDataInstance(i));
		}
	}
	
	
	
	protected void runEStep(int dataInstanceId, List<RVariableValue> observables){
		for(EMModule mod : emmodules){
			mod.runEStep(dataInstanceId, observables);
		}
	}
	
	protected void runMStep(){
		for(EMModule mod : emmodules){
			mod.runMStep();
		}
		this.generativeModel.emptyCache(); //after an M-step everything must be cleared so that new probabilities are computed
	}
	

}
