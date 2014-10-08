package commands.model3.bagofwords;

import generativemodel.GMModule;
import generativemodel.GMQuery;
import generativemodel.GMQueryResult;
import generativemodel.ModelTrackedVarIterator;
import generativemodel.RVariable;
import generativemodel.RVariableValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import burlap.oomdp.core.GroundedProp;

import commands.model3.StringValue;
import commands.model3.TaskModule.LiftedVarValue;
import commands.model3.mt.TokenedString;
import commands.model3.mt.Tokenizer;

import burlap.datastructures.HashedAggregator;

public class BagOfWordsModule extends GMModule {

	
	public static final String						TVECTOR = "topicVector";
	public static final String						STOPICS = "semanticTopics";
	public static final String						NNAME = "naturalCommand";
	
	public static final String						NULLTOPIC = "######";
	
	protected RVariable								liftedRFVariable;
	protected RVariable								bindingConstraintVariable;
	
	/**
	 * This one is the conjunction of logical terms in a liftedRFVariable and bindingConstraintVariable
	 */
	protected RVariable								commandTopicVector;
	protected RVariable								topic;
	protected RVariable								naturalCommandVariable;
	
	
	
	protected Set<String> 							semanticTopics;
	protected Set<String> 							naturalWords;
	
	protected WordParam								wp;
	
	protected Tokenizer								tokenizer;
	
	
	protected boolean								includePFParamClasses = true;
	protected boolean								useFrequnecyOfTopicsInCommand = true;
	
	
	
	public BagOfWordsModule(String name, RVariable liftedRFVariable, RVariable bindingConstraintVariable, Set<String> semanticTopics, Set<String> naturalWords, Tokenizer tokenizer) {
		super(name);
		this.liftedRFVariable = liftedRFVariable;
		this.bindingConstraintVariable = bindingConstraintVariable;
		this.semanticTopics = semanticTopics;
		this.naturalWords = naturalWords;
		this.tokenizer = tokenizer;
		
		this.semanticTopics.add(NULLTOPIC); //add it if it wasn't already there
		
		this.commandTopicVector = new RVariable(TVECTOR, this);
		this.topic = new RVariable(STOPICS, this);
		this.naturalCommandVariable = new RVariable(NNAME, this);
		
		
		this.wp = new WordParam();
		
		//initialize the word parameters to be uniform
		double uniWordGen = 1. / this.naturalWords.size();
		for(String s : this.semanticTopics){
			for(String n : this.naturalWords){
				this.wp.set(uniWordGen, n, s);
			}
		}
	}
	



	public Set<String> getSemanticTopics() {
		return semanticTopics;
	}



	public void setSemanticTopics(Set<String> semanticTopics) {
		this.semanticTopics = semanticTopics;
	}



	public Set<String> getNaturalWords() {
		return naturalWords;
	}



	public void setNaturalWords(Set<String> naturalWords) {
		this.naturalWords = naturalWords;
	}



	public WordParam getWp() {
		return wp;
	}



	public void setWp(WordParam wp) {
		this.wp = wp;
	}



	public Tokenizer getTokenizer() {
		return tokenizer;
	}



	public void setTokenizer(Tokenizer tokenizer) {
		this.tokenizer = tokenizer;
	}

	

	public boolean isIncludePFParamClasses() {
		return includePFParamClasses;
	}


	public void setIncludePFParamClasses(boolean includePFParamClasses) {
		this.includePFParamClasses = includePFParamClasses;
	}




	public boolean isUseFrequnecyOfTopicsInCommand() {
		return useFrequnecyOfTopicsInCommand;
	}




	public void setUseFrequnecyOfTopicsInCommand(
			boolean useFrequnecyOfTopicsInCommand) {
		this.useFrequnecyOfTopicsInCommand = useFrequnecyOfTopicsInCommand;
	}




	@Override
	public GMQueryResult computeProb(GMQuery query) {
		
		RVariableValue qval = query.getSingleQueryVar();
		if(qval.getOwner().equals(this.naturalCommandVariable)){
			TopicVectorRVal topicVec = (TopicVectorRVal)query.getConditionForVariable(this.commandTopicVector);
			if(topicVec == null){
				LiftedVarValue liftedRF = (LiftedVarValue)query.getConditionForVariable(this.liftedRFVariable);
				LiftedVarValue constraints = (LiftedVarValue)query.getConditionForVariable(this.bindingConstraintVariable);
				if(liftedRF == null || constraints == null){
					throw new RuntimeException("Cannot compute the probabiltiy for " + this.commandTopicVector.getName() + " because the requried conditions are not specified.");
				}
				topicVec = new TopicVectorRVal(this.commandTopicVector, liftedRF, constraints);
			}
			
			//now compute
			double p = this.computeNaturalCommandProb((StringValue)qval, topicVec);
			GMQueryResult res = new GMQueryResult(query, p);
			return res;
		}
		
		throw new RuntimeException("Cannot compute the probability for variable: " + qval.getOwner().getName());
	}

	@Override
	public ModelTrackedVarIterator getNonZeroProbIterator(RVariable queryVar,
			List<RVariableValue> conditions) {
		
		if(queryVar.equals(this.commandTopicVector)){
			
			LiftedVarValue liftedRF = (LiftedVarValue)this.extractValueForVariableFromConditions(this.liftedRFVariable, conditions);
			LiftedVarValue constraints = (LiftedVarValue)this.extractValueForVariableFromConditions(this.bindingConstraintVariable, conditions);
			return new TopicVectorValIterator(liftedRF, constraints);
			
		}
		
		throw new RuntimeException("Cannot get an iterator for variable: " + queryVar.getName());
	}

	@Override
	public Iterator<RVariableValue> getRVariableValuesFor(RVariable queryVar) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	protected static String tokenCombine(String prodWord, String genWord){
		return prodWord + "+++" + genWord;
	}
	
	
	
	
	public double computeNaturalCommandProb(StringValue ncommand, TopicVectorRVal topicVector){
		Map<String, Double> wordCounts = this.getWordCounts(ncommand);
		return computeNaturalCommandProb(wordCounts, topicVector);
	}
	
	public double computeNaturalCommandProb(Map<String, Double> wordCounts, TopicVectorRVal topicVector){
		
		double prod = 1.;
		for(Map.Entry<String, Double> wce : wordCounts.entrySet()){
			double wc = wce.getValue();
			String w = wce.getKey();
			double inner = this.computeNaturalWordProb(w, topicVector);
			double outer = Math.pow(inner, wc);
			prod *= outer;
		}
		
		return prod;
	}
	
	public double computeNaturalWordProb(String word, TopicVectorRVal topicVector){
		double sum = 0.;
		for(String topic : topicVector.getNonZeroTopics()){
			double inner = topicVector.getSelectionProb(topic) * this.wp.prob(word, topic);
			sum += inner;
		}
		
		return sum;
	}
	
	
	public Map<String, Double> getWordCounts(StringValue ncommand){
		HashedAggregator<String> counts = new HashedAggregator<String>();
		TokenedString tokened = this.tokenizer.tokenize(ncommand.s);
		for(int i = 1; i <= tokened.size(); i++){
			String w = tokened.t(i);
			if(this.naturalWords.contains(w)){
				counts.add(w, 1.);
			}
		}
		
		return counts.getHashMap();
	}
	
	
	public void printWordParams(){
		for(String topic : this.semanticTopics){
			System.out.println(topic + "\n-------------------");
			for(String word : this.naturalWords){
				double param = this.wp.prob(word, topic);
				System.out.println(param + " " + word);
			}
			System.out.println("\n");
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
	
	
	
	public class TopicVectorValIterator extends ModelTrackedVarIterator{

		TopicVectorRVal res;
		boolean hasNext = true;
		LiftedVarValue liftedRF;
		LiftedVarValue constraints;
		
		public TopicVectorValIterator(LiftedVarValue liftedRF, LiftedVarValue constraints){
			this.res = new TopicVectorRVal(commandTopicVector, liftedRF, constraints);
			this.liftedRF = liftedRF;
			this.constraints = constraints;
		}
		
		@Override
		public boolean hasNext() {
			return hasNext;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public GMQueryResult varSpecificNext() {
			hasNext = false;
			GMQueryResult qres = new GMQueryResult(1.);
			qres.addQuery(res);
			qres.addCondition(liftedRF);
			qres.addCondition(constraints);
			
			return qres;
		}
		
		
		
	}
	
	
	public class TopicVectorRVal extends RVariableValue{

		protected Map<String, Integer> counts;
		protected int totalCount = 0;
		
		protected String srep;
		
		
		public TopicVectorRVal(RVariable owner, LiftedVarValue liftedRF, LiftedVarValue constraints){
			this.setOwner(owner);
			HashedAggregator<String> hcounts = new HashedAggregator<String>();
			this.counts = new HashMap<String, Integer>();
			int sum = 0;
			
			this.counts.put(NULLTOPIC, 1);
			sum++;
			
			
			//do rf
			for(GroundedProp gp : liftedRF.conditions){
				hcounts.add(gp.pf.getName(), 1.);
				this.counts.put(gp.pf.getName(), 1);
				sum++;
				if(includePFParamClasses){
					for(String obClassName : gp.pf.getParameterClasses()){
						hcounts.add(obClassName, 1.);
						this.counts.put(obClassName, 1);
						sum++;
					}
				}
			}
			
			//do constraints
			for(GroundedProp gp : constraints.conditions){
				hcounts.add(gp.pf.getName(), 1.);
				this.counts.put(gp.pf.getName(), 1);
				sum++;
				if(includePFParamClasses){
					for(String obClassName : gp.pf.getParameterClasses()){
						hcounts.add(obClassName, 1.);
						this.counts.put(obClassName, 1);
						sum++;
					}
				}
			}
			
			
			
			if(useFrequnecyOfTopicsInCommand){
				this.totalCount = sum;
				for(Map.Entry<String, Double> e : hcounts.entrySet()){
					int ii = (int)(double)e.getValue();
					counts.put(e.getKey(), ii);
				}
			}
			else{
				this.totalCount = this.counts.size();
			}
			
			List<String> listOfTopics = new ArrayList<String>(this.counts.keySet());
			Collections.sort(listOfTopics);
			StringBuffer bsrep = new StringBuffer();
			boolean first = true;
			for(String t : listOfTopics){
				if(!first){
					bsrep.append(" ");
				}
				int c = this.counts.get(t);
				bsrep.append(t).append("::").append(c);
				
				first = false;
			}
			
			this.srep = bsrep.toString();
			
		}
		
		public Set<String> getNonZeroTopics(){
			return counts.keySet();
		}
		
		public int getTotalCount(){
			return this.totalCount;
		}
		
		public int getCount(String topic){
			Integer C = this.counts.get(topic);
			if(C == null){
				return 0;
			}
			return C;
		}
		
		public double getSelectionProb(String topic){
			int c = this.getCount(topic);
			double p = (double)c / (double)totalCount;
			return p;
		}
		
		@Override
		public boolean valueEquals(RVariableValue other) {
			
			if(!(other instanceof TopicVectorRVal)){
				return false;
			}
			
			TopicVectorRVal to = (TopicVectorRVal)other;
			if(this.totalCount != to.totalCount){
				return false;
			}
			
			for(Map.Entry<String, Integer> e : this.counts.entrySet()){
				int c = e.getValue();
				int oc = to.getCount(e.getKey());
				if(c != oc){
					return false;
				}
			}
			
			return true;
		}

		@Override
		public String stringRep() {
			return this.srep;
		}
		
		
		
	}

}
