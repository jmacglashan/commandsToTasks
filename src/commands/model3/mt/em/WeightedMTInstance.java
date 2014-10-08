package commands.model3.mt.em;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import commands.model3.mt.TokenedString;
import commands.model3.mt.em.WeightedMTInstance.WeightedSemanticCommandPair;

public class WeightedMTInstance implements Iterable<WeightedSemanticCommandPair>{

	
	public List<WeightedSemanticCommandPair>		weightedSemanticCommands;
	public TokenedString							naturalCommand;
	
	public WeightedMTInstance(TokenedString naturalCommand){
		this.naturalCommand = naturalCommand;
		this.weightedSemanticCommands = new ArrayList<WeightedMTInstance.WeightedSemanticCommandPair>();
	}
	
	public void addWeightedSemanticCommand(TokenedString semanticCommand, double prob){
		WeightedSemanticCommandPair pair = new WeightedSemanticCommandPair(semanticCommand, prob);
		this.weightedSemanticCommands.add(pair);
	}
	
	public WeightedSemanticCommandPair getWeightedSemanticCommandPair(int i){
		return this.weightedSemanticCommands.get(i);
	}
	
	public int numWeightedSemanticCommands(){
		return this.weightedSemanticCommands.size();
	}
	
	@Override
	public Iterator<WeightedSemanticCommandPair> iterator() {
		return this.weightedSemanticCommands.iterator();
	}
	
	
	
	
	public static class WeightedSemanticCommandPair{
		
		public TokenedString	semanticCommand;
		public double			prob;
		
		public WeightedSemanticCommandPair(TokenedString semanticCommand, double prob){
			this.semanticCommand = semanticCommand;
			this.prob = prob;
		}
		
		
	}



	
	
}
