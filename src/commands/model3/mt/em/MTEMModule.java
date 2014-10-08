package commands.model3.mt.em;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import commands.model3.Model3Controller;
import commands.model3.mt.MTModule;
import commands.model3.mt.MTModule.DistortionParam;
import commands.model3.mt.MTModule.IntTupleHash;
import commands.model3.mt.MTModule.WordParam;
import commands.model3.mt.TokenedString;
import commands.model3.mt.Tokenizer;
import commands.model3.mt.em.WeightedMTInstance.WeightedSemanticCommandPair;

import burlap.datastructures.HashedAggregator;
import em.EMModule;
import generativemodel.GenerativeModel;
import generativemodel.RVariableValue;

public class MTEMModule extends EMModule {

	
	protected GenerativeModel gm;
	protected List<WeightedMTInstance> dataset;
	protected Tokenizer tokenizer;
	protected MTModule mtModule;
	
	protected double wordAdditiveConstant = 0.1;
	protected int numWithWordParamAlone = 5;
	protected int numEMIterations = 0;
	
	protected boolean factorInAlignment = true;
	
	protected HashedAggregator<String> jointWordCounts;
	protected HashedAggregator<String> singleWordCounts;
	protected HashedAggregator<IntTupleHash> jointDistortionCounts;
	protected HashedAggregator<IntTupleHash> singleDistortionCounts;
	
	
	public MTEMModule(List<WeightedMTInstance> dataset, GenerativeModel gm){
		this.gm = gm;
		this.dataset = dataset;
		this.mtModule = (MTModule)this.gm.getModuleWithName(Model3Controller.LANGMODNAME);
		
		this.primeMTModuleParameters(dataset);
	}
	
	public void runEMManually(int numIterations){
		for(int i = 0; i < numIterations; i++){
			for(int j = 0; j < this.dataset.size(); j++){
				this.runEStep(j, null);
			}
			this.runMStep();
		}
	}
	
	protected void primeMTModuleParameters(List<WeightedMTInstance> dataset){
		jointWordCounts = new HashedAggregator<String>(wordAdditiveConstant);
		singleWordCounts = new HashedAggregator<String>(wordAdditiveConstant*this.mtModule.getNaturalWords().size());
		
		jointDistortionCounts = new HashedAggregator<IntTupleHash>(0);
		singleDistortionCounts = new HashedAggregator<IntTupleHash>(0);
		
		
		//estimate the length parameters up front since they won't change otherwise
		this.mtModule.resetLengthParameters();
		HashedAggregator<IntTupleHash> lmCount = new HashedAggregator<MTModule.IntTupleHash>();
		HashedAggregator<Integer> lCount = new HashedAggregator<Integer>();
		int maxM = 0; //m is length of natural language
		int maxL = 0; //l is length of semantic language
		for(WeightedMTInstance wi : dataset){
			int m = wi.naturalCommand.size();
			maxM = Math.max(maxM, m);
			for(WeightedSemanticCommandPair wsc : wi){
				int l = wsc.semanticCommand.size();
				maxL = Math.max(maxL, l);
				lmCount.add(new IntTupleHash(l, m), wsc.prob);
				lCount.add(l, wsc.prob);
			}
		}
		for(int l : lCount.keySet()){
			double norm = lCount.v(l);
			for(int m = 1; m <= maxM; m++){
				double joint = lmCount.v(new IntTupleHash(l, m));
				double p = joint / norm;
				this.mtModule.setLengthParameterProb(l, m, p);
			}
		}
		
	}
	
	
	@Override
	public void runEStep(int dataInstanceId, List<RVariableValue> observables) {
		
		WeightedMTInstance instance = this.dataset.get(dataInstanceId);
		TokenedString naturalCommand = instance.naturalCommand;
		for(WeightedSemanticCommandPair ws : instance){
			TokenedString semanticCommand = ws.semanticCommand;
			double weight = ws.prob;
			
			int m = naturalCommand.size();
			int l = semanticCommand.size();
			
			for(int i = 1; i <= m; i++){
				
				for(int j = 0; j <= l; j++){
					
					double delta = this.expectedVal(naturalCommand, semanticCommand, i, j, m, l);
					delta *= weight;
					
					String nWord = naturalCommand.t(i);
					String sWord = semanticCommand.t(j);
					
					if(Double.isNaN(delta)){
						throw new RuntimeException("delta is NaN.");
					}
					
					this.jointWordCounts.add(tokenCombine(nWord, sWord), delta);
					this.singleWordCounts.add(sWord, delta);
					
					this.jointDistortionCounts.add(new IntTupleHash(j, i, l, m), delta);
					this.singleDistortionCounts.add(new IntTupleHash(i, l, m), delta);
					
				}
				
			}
			
			
		}
	

	}

	@Override
	public void runMStep() {
		
		Set<String> semanticWords = this.mtModule.getSemanticWords();
		Set<String> naturalWords = this.mtModule.getNaturalWords();
		
		WordParam wp = this.mtModule.getWp();
		DistortionParam dp = this.mtModule.getDp();
		
		int maxNatural = this.mtModule.getMaxNaturalCommandLength();
		int maxSemantic = this.mtModule.getMaxSemanticCommandLength();
		
		//M-step for word params
		for(String gWord : semanticWords){
			double sc = singleWordCounts.v(gWord);
			for(String pWord : naturalWords){
				double jc = jointWordCounts.v(tokenCombine(pWord, gWord));
				
				double p = jc > 0. ? jc / sc : 0.;
				wp.set(p, pWord, gWord);
				
			}
		}
		
		//M step for distortion params
		for(int m = 1; m <= maxNatural; m++){
			for(int l = 1; l <= maxSemantic; l++){
				
				/* the first two loops are to iterate over all possible text length pairs
				 * the next to loops will be to iterate over all possible assignments for them
				 */
				
				for(int i = 1; i <= m; i++){

					double sc = singleDistortionCounts.v(new IntTupleHash(i, l, m));
					for(int j = 0; j <= l; j++){
						double jc = jointDistortionCounts.v(new IntTupleHash(j, i, l, m));
						
						double p = jc > 0. ? jc / sc : 0.;
						dp.set(p, j, i, l, m);
						
					}
					
				}
				
				
			}
		}
		
		
		
		
		//reset storage for next EM step
		jointWordCounts = new HashedAggregator<String>(wordAdditiveConstant);
		singleWordCounts = new HashedAggregator<String>(wordAdditiveConstant*this.mtModule.getNaturalWords().size());
		
		jointDistortionCounts = new HashedAggregator<IntTupleHash>(0);
		singleDistortionCounts = new HashedAggregator<IntTupleHash>(0);
		
		
		this.numEMIterations++;
		if(this.numEMIterations >= this.numWithWordParamAlone){
			this.factorInAlignment = true;
		}
		
		this.gm.emptyCache();

	}
	
	
	
	protected double expectedVal(TokenedString prodText, TokenedString genText, int i, int j, int m, int l){
		
		WordParam wp = this.mtModule.getWp();
		DistortionParam dp = this.mtModule.getDp();
		
		double sum = 0.;
		for(int u = 0; u <= l; u++){
			double d = 1.;
			if(this.factorInAlignment){
				d = dp.prob(u, i, l, m);
			}
			double w = wp.prob(prodText.t(i), genText.t(u));
			sum += d*w;
		}
		
		double d = 1.;
		if(this.factorInAlignment){
			d = dp.prob(j, i, l, m);
		}
		double w = wp.prob(prodText.t(i), genText.t(j));
		
		double num = d*w;
		double p = num / sum;
		
		if(num == 0. && sum == 0.){
			p = 0.;
		}
		
		if(Double.isNaN(p)){
			throw new RuntimeException("Expected Val is NaN.");
		}
		
		return p;
		
	}
	
	
	
	
	protected static String tokenCombine(String prodWord, String genWord){
		return prodWord + "+++" + genWord;
	}
	
	
	public static List<WeightedMTInstance> getVanillaMTDataset(Tokenizer tokenizer, String pathToDataDir, String dataFileExtension){
		
		List<WeightedMTInstance> dataset = new ArrayList<WeightedMTInstance>();
		
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
		for(int i = 0; i < children.length; i++){
			String path = pathToDataDir + "/" + children[i];
			dataset.add(getInstanceFromFile(tokenizer, path));
		}
		
		return dataset;
	}
	
	public static WeightedMTInstance getInstanceFromFile(Tokenizer tokenizer, String pathToFile){
		
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
		}
		
		return d;

	}

}
