package commands.model3.mt;

import generativemodel.GMQuery;
import generativemodel.GMQueryResult;
import generativemodel.GenerativeModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import commands.model3.StringValue;
import commands.model3.mt.em.WeightedMTInstance;
import commands.model3.mt.em.WeightedMTInstance.WeightedSemanticCommandPair;

import burlap.datastructures.HashedAggregator;

public class SentenceReasoner {

	protected Map <TokenedString, Double> 	probGenSentences;
	
	protected GenerativeModel				gm;
	
	
	
	public SentenceReasoner(GenerativeModel gm, List<WeightedMTInstance> dataset){
		this.gm = gm;
		HashedAggregator<TokenedString>  ag = new HashedAggregator<TokenedString>();
		double sum = 0.;
		for(WeightedMTInstance d : dataset){
			for(WeightedSemanticCommandPair ws : d){
				ag.add(ws.semanticCommand, ws.prob);
				sum += ws.prob;
			}
		}
		
		this.probGenSentences = new HashMap<TokenedString, Double>(ag.size());
		for(TokenedString sem : ag.keySet()){
			double sumW = ag.v(sem);
			double p = sumW / sum;
			this.probGenSentences.put(sem, p); //sample data prior
			//this.probGenSentences.put(sem, 1./ag.keySet().size()); //uniform pripr
		}
		
		
	}
	
	
	public List<GMQueryResult> distribution(String naturalLanguageCommand){
		
		StringValue naturalRV = new StringValue(naturalLanguageCommand, this.gm.getRVarWithName(MTModule.NNAME));
		double sum = 0.;
		List<GMQueryResult> distribution = new ArrayList<GMQueryResult>();
		for(Map.Entry<TokenedString, Double> e : this.probGenSentences.entrySet()){
			
			StringValue semRV = new StringValue(e.getKey().toString(), this.gm.getRVarWithName(MTModule.SNAME));
			GMQuery query = new GMQuery();
			query.addQuery(naturalRV);
			query.addCondition(semRV);
			
			GMQueryResult conditionalResult = this.gm.getProb(query, true);
			
			double pNum = e.getValue() * conditionalResult.probability;
			sum += pNum;
			if(pNum > 0.){
				GMQueryResult dEntry = new GMQueryResult(pNum);
				dEntry.addQuery(semRV);
				dEntry.addCondition(naturalRV);
				distribution.add(dEntry);
			}
			
		}
		
		for(GMQueryResult dEntry : distribution){
			dEntry.probability = dEntry.probability / sum;
		}
		
		//everything was zero so default to priors
		if(distribution.size() == 0){
			System.out.println("zero prob for: " + naturalLanguageCommand);
			System.exit(0);
			for(Map.Entry<TokenedString, Double> e : this.probGenSentences.entrySet()){
				StringValue semRV = new StringValue(e.getKey().toString(), this.gm.getRVarWithName(MTModule.SNAME));
				GMQueryResult dEntry = new GMQueryResult(e.getValue());
				dEntry.addQuery(semRV);
				dEntry.addCondition(naturalRV);
				distribution.add(dEntry);
			}
		}
		
		return distribution;
	}
	
	
	
	
	
}
