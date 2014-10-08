package commands.model3.mt;

import generativemodel.GMModule;
import generativemodel.GMQuery;
import generativemodel.GMQueryResult;
import generativemodel.ModelTrackedVarIterator;
import generativemodel.RVariable;
import generativemodel.RVariableValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import burlap.debugtools.RandomFactory;
import burlap.oomdp.core.GroundedProp;

import commands.model3.StringValue;
import commands.model3.TaskModule.LiftedVarValue;

public class MTModule extends GMModule {

	public static final String						SNAME = "semanticCommand";
	public static final String						NNAME = "naturalCommand";
	
	
	protected RVariable								liftedRFVariable;
	protected RVariable								bindingConstraintVariable;
	
	protected RVariable								semanticCommandVariable;
	protected RVariable								naturalCommandVariable;
	
	
	protected Set<String> 							semanticWords;
	protected Set<String> 							naturalWords;
	
	protected int									maxSemanticCommandLength;
	protected int									maxNaturalCommandLength;
	
	protected DistortionParam 						dp;
	protected WordParam 							wp;
	protected LengthParam 							lp;
	
	protected Tokenizer								naturalTokenizer;
	protected Tokenizer								semanticTokenizer;
	
	protected Random								rand;
	
	public MTModule(String name, RVariable liftedRFVariable, RVariable bindingConstraintVariable, Set<String> semanticWords, Set<String> naturalWords, 
			int maxSemanticCommandLength, int maxNaturalCommandLenth, Tokenizer tokenizer) {
		super(name);
		
		this.liftedRFVariable = liftedRFVariable;
		this.bindingConstraintVariable = bindingConstraintVariable;
		
		this.semanticCommandVariable = new RVariable(SNAME, this);
		this.naturalCommandVariable = new RVariable(NNAME, this);
		
		this.semanticWords = semanticWords;
		this.naturalWords = naturalWords;
		
		this.maxSemanticCommandLength = maxSemanticCommandLength;
		this.maxNaturalCommandLength = maxNaturalCommandLenth;
		
		this.naturalTokenizer = tokenizer;
		this.semanticTokenizer = new Tokenizer(true);
		
		this.wp = new WordParam();
		this.dp = new DistortionParam();
		this.lp = new LengthParam();
		
		//initialize the word parameters to be uniform
		double uniWordGen = 1. / this.semanticWords.size();
		for(String s : this.semanticWords){
			for(String n : this.naturalWords){
				this.wp.set(uniWordGen, n, s);
			}
		}
		
		//initialize length params; this should probably be reset once a dataset is provided
		double uniLen = 1. / this.maxNaturalCommandLength;
		for(int l = 1; l <= this.maxSemanticCommandLength; l++){
			for(int m = 1; m <= this.maxNaturalCommandLength; m++){
				this.lp.set(uniLen, l, m);
				
				for(int i = 1; i <= m; i++){
					double uniDist = 1./(l+1); //old form, which seems like it was wrong: 1./(this.maxL+1);
					for(int j = 0; j <= l; j++){
						this.dp.set(uniDist, j, i, l, m);
					}
				}
				
			}
		}
		
		
		//this.rand = RandomFactory.getMapped(0);
		this.rand = new Random(1);
		
	}
	
	
	public void resetParametersToUniforForNewDictionary(Set<String> naturalWords, int maxNaturalCommandLenth){
		
		this.naturalWords = naturalWords;
		this.maxNaturalCommandLength = maxNaturalCommandLenth;
		
		this.wp = new WordParam();
		this.dp = new DistortionParam();
		this.lp = new LengthParam();
		
		//initialize the word parameters to be uniform
		double uniWordGen = 1. / this.semanticWords.size();
		for(String s : this.semanticWords){
			for(String n : this.naturalWords){
				this.wp.set(uniWordGen, n, s);
			}
		}
		
		//initialize length params; this should probably be reset once a dataset is provided
		double uniLen = 1. / this.maxNaturalCommandLength;
		for(int l = 1; l <= this.maxSemanticCommandLength; l++){
			for(int m = 1; m <= this.maxNaturalCommandLength; m++){
				this.lp.set(uniLen, l, m);
				
				for(int i = 1; i <= m; i++){
					double uniDist = 1./(l+1); //old form, which seems like it was wrong: 1./(this.maxL+1);
					for(int j = 0; j <= l; j++){
						this.dp.set(uniDist, j, i, l, m);
					}
				}
				
			}
		}
		
	}
	
	
	
	public int getMaxSemanticCommandLength() {
		return maxSemanticCommandLength;
	}



	public void setMaxSemanticCommandLength(int maxSemanticCommandLength) {
		this.maxSemanticCommandLength = maxSemanticCommandLength;
	}



	public int getMaxNaturalCommandLength() {
		return maxNaturalCommandLength;
	}



	public void setMaxNaturalCommandLength(int maxNaturalCommandLength) {
		this.maxNaturalCommandLength = maxNaturalCommandLength;
	}

	


	public Set<String> getNaturalWords() {
		return naturalWords;
	}



	public void setNaturalWords(Set<String> naturalWords) {
		this.naturalWords = naturalWords;
	}

	

	public Set<String> getSemanticWords() {
		return semanticWords;
	}

	

	public void setSemanticWords(Set<String> semanticWords) {
		this.semanticWords = semanticWords;
	}



	public DistortionParam getDp() {
		return dp;
	}



	public void setDp(DistortionParam dp) {
		this.dp = dp;
	}



	public WordParam getWp() {
		return wp;
	}



	public void setWp(WordParam wp) {
		this.wp = wp;
	}



	public LengthParam getLp() {
		return lp;
	}



	public void setLp(LengthParam lp) {
		this.lp = lp;
	}


	public void setLengthParameterProb(int l, int m, double p){
		this.lp.set(p, l, m);
	}
	
	public void resetLengthParameters(){
		this.lp = new LengthParam();
	}

	public TokenedString getTokenedSemanticString(LiftedVarValue liftedRF, LiftedVarValue bindingConstraints){

		StringBuffer buf = new StringBuffer();
		boolean first = true;
		for(GroundedProp gp : liftedRF.conditions){
			if(!first){
				buf.append(" ");
			}
			buf.append(gp.pf.getName());
			for(String p : gp.pf.getParameterClasses()){
				buf.append(" ").append(p);
			}
			first = false;
		}
		for(GroundedProp gp : bindingConstraints.conditions){
			buf.append(" ").append(gp.pf.getName());
			for(String p : gp.pf.getParameterClasses()){
				buf.append(" ").append(p);
			}
		}
		
		return this.semanticTokenizer.tokenize(buf.toString());
	}

	@Override
	public GMQueryResult computeProb(GMQuery query) {
		
		RVariableValue queryVal = query.getSingleQueryVar();
		if(queryVal.getOwner().equals(this.naturalCommandVariable)){
			
			StringValue naturalCommandVal = (StringValue)queryVal;
			TokenedString naturalTokened = this.naturalTokenizer.tokenize(naturalCommandVal.s);
			
			StringValue semanticCommandVal = (StringValue)query.getConditionForVariable(semanticCommandVariable);
			TokenedString semTokened;
			if(semanticCommandVal != null){
				semTokened = this.semanticTokenizer.tokenize(semanticCommandVal.s);
			}
			else{
				LiftedVarValue liftedRF = (LiftedVarValue)query.getConditionForVariable(this.liftedRFVariable);
				LiftedVarValue bindingConstraints = (LiftedVarValue)query.getConditionForVariable(this.bindingConstraintVariable);
				if(liftedRF == null || bindingConstraints == null){
					throw new RuntimeException("Not proper conditional variable values to compute the probabiltiy for a natural language command. Need either a semantic command or a lifted RF and binding constraint");
				}
				semTokened = this.getTokenedSemanticString(liftedRF, bindingConstraints);
			}
			
			
			double p = this.computeNaturalCommandProb(semTokened, naturalTokened);
			GMQueryResult res = new GMQueryResult(query, p);
			return res;
			
		}
		
		throw new RuntimeException("Cannot compute probability for variable: " + queryVal.getOwner().getName());
	}
	
	public double computeNaturalCommandProb(TokenedString semanticCommand, TokenedString naturalCommand){
		
		
		int l = semanticCommand.size();
		int m = naturalCommand.size();
		
		
		double n = this.lp.prob(l, m);
		if(n == 0.){
			//then is there any l for which this is not true?
			boolean allLengthParamsZero = true;
			for(int i = 1; i <= this.maxSemanticCommandLength; i++){
				if(this.lp.prob(i, m) > 0.){
					allLengthParamsZero = false;
					break;
				}
			}
			if(allLengthParamsZero){
				n = 1.;
			}
		}
		
		double alignMarg = 0.;
		if(n > 0){
			alignMarg = this.sampleMargAlign(semanticCommand, naturalCommand, 1000);
		}
		else{
			alignMarg = this.m1MaximumAlignment(semanticCommand, naturalCommand);
		}
		
		double p = alignMarg * n;
		
		return p;
	}
	
	protected double sampleMargAlign(TokenedString semanticCommand, TokenedString naturalCommand, int nSamples){
		
		
		int l = semanticCommand.size();
		int m = naturalCommand.size();
		
		double alignMarg = 0.;
		for(int i = 0; i < nSamples; i++){
			List <Integer> alignment = this.sampleAlignment(l, m);
			double prod = 1.;
			for(int k = 1; k <= alignment.size(); k++){
				int ak = alignment.get(k-1); //note that alignment array is in 0-base index
				String pWord = naturalCommand.t(k);
				String gWord = semanticCommand.t(ak);
				
				if(!this.naturalWords.contains(pWord)){
					continue;
				}
				
				double word = this.wp.prob(pWord, gWord);
				
				prod *= word;
			}
			alignMarg += prod;

		}
		
		
		alignMarg /= (double)nSamples;
		
		return alignMarg;
		
	}
	
	protected double m1MaximumAlignment(TokenedString semanticCommand, TokenedString naturalCommand){
		
		
		int l = semanticCommand.size();
		int m = naturalCommand.size();
		
		double prod = 1.;
		for(int k = 1; k <= m; k++){
			
			String pWord = naturalCommand.t(k);
			if(!this.naturalWords.contains(pWord)){
				continue;
			}
			
			//find best match
			double bestMatch = 0.;
			for(int j = 0; j <= l; j++){
				double word = this.wp.prob(pWord, semanticCommand.t(j));
				if(word > bestMatch){
					bestMatch = word;
				}
			}
			
			prod *= bestMatch;
			
		}
		
		//normalize by how many possible alignments there are
		double prob = prod / Math.pow(l+1, m);
		
		return prob;
		
	}
	
	protected List <Integer> sampleAlignment(int l, int m){
		List <Integer> alignment = new ArrayList<Integer>(m);
		
		for(int i = 1; i <= m; i++){
			double r = this.rand.nextDouble();
			double sumP = 0.;
			for(int j = 0; j <= l; j++){
				double p = this.dp.prob(j, i, l, m);
				sumP += p;
				if(r < sumP){
					alignment.add(j);
					break;
				}
			}

		}
		
		
		return alignment;
	}
	

	@Override
	public ModelTrackedVarIterator getNonZeroProbIterator(RVariable queryVar,
			List<RVariableValue> conditions) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<RVariableValue> getRVariableValuesFor(RVariable queryVar) {
		// TODO Auto-generated method stub
		return null;
	}

	protected static String tokenCombine(String prodWord, String genWord){
		return prodWord + "+++" + genWord;
	}
	
	
	public void printLPParams(){
		for(int l = 1; l <= this.maxSemanticCommandLength; l++){
			for(int m = 1; m <= this.maxNaturalCommandLength; m++){
				System.out.println("p("+m+"|"+l+") = " + this.lp.prob(l, m));
			}
		}
	}
	
	public void printDistortionParams(){
		for(int l = 1; l <= this.maxSemanticCommandLength; l++){
			for(int m = 1; m <= this.maxNaturalCommandLength; m++){
				double lprob = this.lp.prob(l, m);
				if(lprob > 0.){
					System.out.println("l = " + l + "; m = " + m + "\n----------------------------");
					for(int i = 1; i <= m; i++){
						for(int j = 0; j <= l; j++){
							double p = this.dp.prob(j, i, l, m);
							System.out.println(p + ": (" + j + " | " + i + ")");
						}
					}
				}
			}
		}
	}
	
	
	
	public class DistortionParam{
		
		Map<IntTupleHash, Double> paramValues;
		
		public DistortionParam(){
			this.paramValues = new HashMap<IntTupleHash, Double>();
		}
		
		public double prob(int j, int i, int l, int m){
			Double P = this.paramValues.get(new IntTupleHash(j, i, l, m));
			double p = P != null ? P : 0.; 
			return p;
		}
		
		public void set(double p, int j, int i, int l, int m){
			this.paramValues.put(new IntTupleHash(j, i, l, m), p);
		}
		
		
	}
	

	
	public class WordParam{
		
		Map <String, Double> paramValues;
		
		public WordParam(){
			this.paramValues = new HashMap<String, Double>();
		}
		
		public double prob(String prodWord, String genWord){
			Double P = this.paramValues.get(tokenCombine(prodWord, genWord));
			double p = P != null ? P : 0.;
			return p;
		}
		
		public void set(double p, String prodWord, String genWord){
			this.paramValues.put(tokenCombine(prodWord, genWord), p);
		}
		
	}
	
	public class LengthParam{
		
		Map <IntTupleHash, Double> paramValues;
		
		public LengthParam(){
			this.paramValues = new HashMap<IntTupleHash, Double>();
		}
		
		public double prob(int l, int m){
			Double P = this.paramValues.get(new IntTupleHash(l, m));
			double p = P != null ? P : 0.; 
			return p;
		}
		
		public void set(double p, int l, int m){
			this.paramValues.put(new IntTupleHash(l, m), p);
		}
		
	}
	
	
	public static class IntTupleHash{
		
		int [] tuple;
		
		public IntTupleHash(int j, int i, int l, int m){
			this.tuple = new int[]{j,i,l,m};
		}
		
		public IntTupleHash(int i, int l, int m){
			this.tuple = new int[]{i,l,m};
		}
		
		public IntTupleHash(int l, int m){
			this.tuple = new int[]{l,m};
		}
		
		@Override
		public int hashCode(){
			
			int shift = 23;
			int prod = 1;
			int sum = 0;
			for(int i : tuple){
				sum += i*prod;
				prod *= shift;
			}
			
			return sum;
			
		}
		
		@Override 
		public boolean equals(Object o){
			
			if(!(o instanceof IntTupleHash)){
				return false;
			}
			
			IntTupleHash oo = (IntTupleHash)o;
			
			if(this.tuple.length != oo.tuple.length){
				return false;
			}
			
			for(int i = 0; i < this.tuple.length; i++){
				if(this.tuple[i] != oo.tuple[i]){
					return false;
				}
			}
			
			return true;
			
		}
		
	}
	

}
