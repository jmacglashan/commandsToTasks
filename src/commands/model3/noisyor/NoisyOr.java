package commands.model3.noisyor;

import generativemodel.GMModule;
import generativemodel.GMQuery;
import generativemodel.GMQueryResult;
import generativemodel.ModelTrackedVarIterator;
import generativemodel.RVariable;
import generativemodel.RVariableValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import burlap.oomdp.core.GroundedProp;

import commands.model3.StringValue;
import commands.model3.TaskModule.LiftedVarValue;
import commands.model3.bagofwords.BagOfWordsModule.TopicVectorRVal;
import commands.model3.mt.TokenedString;
import commands.model3.mt.Tokenizer;

public class NoisyOr extends GMModule {

	public static final String						TVECTOR = "topicVector";
	public static final String						STOPICS = "semanticTopics";
	public static final String						NNAME = "naturalCommand";
	
	public static final String						NULLTOPIC = "######";
	
	protected RVariable								liftedRFVariable;
	protected RVariable								bindingConstraintVariable;
	
	/**
	 * This one is the conjunction of logical terms in a liftedRFVariable and bindingConstraintVariable
	 */
	protected RVariable								commandTopicSet;
	protected RVariable								topic;
	protected RVariable								naturalCommandVariable;
	
	
	
	protected Set<String> 							semanticTopics;
	protected Set<String> 							naturalWords;
	
	protected WordParam								wp;
	
	protected Tokenizer								tokenizer;
	
	protected boolean								includePFParamClasses = true;
	
	
	public NoisyOr(String name, RVariable liftedRFVariable, RVariable bindingConstraintVariable, Set<String> semanticTopics, Set<String> naturalWords, Tokenizer tokenizer) {
		super(name);
		
		
		this.liftedRFVariable = liftedRFVariable;
		this.bindingConstraintVariable = bindingConstraintVariable;
		this.semanticTopics = semanticTopics;
		this.naturalWords = naturalWords;
		this.tokenizer = tokenizer;
		
		this.semanticTopics.add(NULLTOPIC); //add it if it wasn't already there
		
		this.commandTopicSet = new RVariable(TVECTOR, this);
		this.topic = new RVariable(STOPICS, this);
		this.naturalCommandVariable = new RVariable(NNAME, this);
		
		
		this.wp = new WordParam();
		
		//initialize the word suppression parameters to be 50-50
		for(String s : this.semanticTopics){
			for(String n : this.naturalWords){
				this.wp.set(0.9999, n, s);
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


	@Override
	public GMQueryResult computeProb(GMQuery query) {
		
		RVariableValue qval = query.getSingleQueryVar();
		if(qval.getOwner().equals(this.naturalCommandVariable)){
			TopicSetRVal topicSet = (TopicSetRVal)query.getConditionForVariable(this.commandTopicSet);
			if(topicSet == null){
				LiftedVarValue liftedRF = (LiftedVarValue)query.getConditionForVariable(this.liftedRFVariable);
				LiftedVarValue constraints = (LiftedVarValue)query.getConditionForVariable(this.bindingConstraintVariable);
				if(liftedRF == null || constraints == null){
					throw new RuntimeException("Cannot compute the probabiltiy for " + this.commandTopicSet.getName() + " because the requried conditions are not specified.");
				}
				topicSet = new TopicSetRVal(this.commandTopicSet, liftedRF, constraints);
			}
			
			//now compute
			double p = this.computeNaturalCommandProb((StringValue)qval, topicSet);
			GMQueryResult res = new GMQueryResult(query, p);
			return res;
		}
		
		throw new RuntimeException("Cannot compute the probability for variable: " + qval.getOwner().getName());
		
		
	}

	@Override
	public ModelTrackedVarIterator getNonZeroProbIterator(RVariable queryVar,
			List<RVariableValue> conditions) {
		
		if(queryVar.equals(this.commandTopicSet)){
			
			LiftedVarValue liftedRF = (LiftedVarValue)this.extractValueForVariableFromConditions(this.liftedRFVariable, conditions);
			LiftedVarValue constraints = (LiftedVarValue)this.extractValueForVariableFromConditions(this.bindingConstraintVariable, conditions);
			return new TopicSetValIterator(liftedRF, constraints);
			
		}
		
		throw new RuntimeException("Cannot get an iterator for variable: " + queryVar.getName());
	}

	@Override
	public Iterator<RVariableValue> getRVariableValuesFor(RVariable queryVar) {
		// TODO Auto-generated method stub
		return null;
	}

	
	
	
	public double computeNaturalCommandProb(StringValue ncommand, TopicSetRVal topicSet){
		Set<String> wordSet = this.getWordSet(ncommand);
		return computeNaturalCommandProb(wordSet, topicSet);
	}
	
	
	public double computeNaturalCommandProb(Set<String> wordSet, TopicSetRVal topicSet){
		
		double product = 1.;
		for(String w : this.naturalWords){
			double wP = this.probNotWordGivenTopicSet(w, topicSet);
			if(wordSet.contains(w)){
				wP = 1. - wP;
			}
			product *= wP;
		}
		
		return product;
	}
	
	public double probNotWordGivenTopicSet(String word, TopicSetRVal topicSet){
		double wP = 1.;
		for(String topic : topicSet.topics){
			wP *= this.wp.prob(word, topic);
		}
		return wP;
		
	}
	
	public Set<String> getWordSet(StringValue ncommand){
		Set<String> wordSet = new HashSet<String>();
		
		TokenedString tokened = this.tokenizer.tokenize(ncommand.s);
		for(int i = 1; i <= tokened.size(); i++){
			String w = tokened.t(i);
			if(this.naturalWords.contains(w)){
				wordSet.add(w);
			}
			
		}
		
		return wordSet;
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
	
	
	
	
	
	protected static String tokenCombine(String prodWord, String genWord){
		return prodWord + "+++" + genWord;
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
	
	
	
	
	
	
	
	
	
	
	
	public class TopicSetValIterator extends ModelTrackedVarIterator{

		TopicSetRVal res;
		boolean hasNext = true;
		LiftedVarValue liftedRF;
		LiftedVarValue constraints;
		
		public TopicSetValIterator(LiftedVarValue liftedRF, LiftedVarValue constraints){
			this.res = new TopicSetRVal(commandTopicSet, liftedRF, constraints);
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
	
	
	
	public class TopicSetRVal extends RVariableValue{

		protected Set<String> topics;
		
		protected String srep;
		
		
		public TopicSetRVal(RVariable owner, LiftedVarValue liftedRF, LiftedVarValue constraints){
			this.setOwner(owner);
			this.topics = new HashSet<String>();
			
			this.topics.add(NULLTOPIC);
			
			
			//do rf
			for(GroundedProp gp : liftedRF.conditions){
				this.topics.add(gp.pf.getName());
				if(includePFParamClasses){
					for(String obClassName : gp.pf.getParameterClasses()){
						
						topics.add(obClassName);
					}
				}
			}
			
			//do constraints
			for(GroundedProp gp : constraints.conditions){
				this.topics.add(gp.pf.getName());
				if(includePFParamClasses){
					for(String obClassName : gp.pf.getParameterClasses()){
						this.topics.add(obClassName);
					}
				}
			}
			
			
			List<String> listOfTopics = new ArrayList<String>(this.topics);
			Collections.sort(listOfTopics);
			StringBuffer bsrep = new StringBuffer();
			boolean first = true;
			for(String t : listOfTopics){
				if(!first){
					bsrep.append(" ");
				}
				bsrep.append(t);
				
				first = false;
			}
			
			this.srep = bsrep.toString();
			
		}
		
		public Set<String> getNonZeroTopics(){
			return this.topics;
		}
		
		
		@Override
		public boolean valueEquals(RVariableValue other) {
			
			if(!(other instanceof TopicVectorRVal)){
				return false;
			}
			
			TopicSetRVal to = (TopicSetRVal)other;
			
			if(this.topics.size() != to.topics.size()){
				return false;
			}
			
			for(String t : this.topics){
				if(!to.topics.contains(t)){
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
