package generativemodel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.RuntimeErrorException;



public class GenerativeModel {
	
	protected List<GMModule>					modules;
	protected Map <String, GMModule>			moduleMap;
	protected List <RVariable>					inputVariables;
	protected Map<RVariable, GMModule>			variableOwnerResolver;
	protected Map<GMQuery, GMQueryResult>		queryCache;
	protected Map<GMQuery, GMQueryResult>		logQueryCache;
	
	
	
	public GenerativeModel(){
		this.modules = new ArrayList<GMModule>();
		this.moduleMap = new HashMap<String, GMModule>();
		this.inputVariables = new ArrayList<RVariable>();
		this.variableOwnerResolver = new HashMap<RVariable, GMModule>();
		this.queryCache = new HashMap<GMQuery, GMQueryResult>();
		this.logQueryCache = new HashMap<GMQuery, GMQueryResult>();
	}
	
	public GenerativeModel(List<RVariable> inputVariables){
		this.inputVariables = inputVariables;
		this.modules = new ArrayList<GMModule>();
		this.moduleMap = new HashMap<String, GMModule>();
		this.variableOwnerResolver = new HashMap<RVariable, GMModule>();
		this.queryCache = new HashMap<GMQuery, GMQueryResult>();
		this.logQueryCache = new HashMap<GMQuery, GMQueryResult>();
	}
	
	
	/**
	 * Add a module that governs the probability distribution and definition of random variables.
	 * @param module the module to add
	 */
	public void addGMModule(GMModule module){
		if(this.moduleMap.containsKey(module.name)){
			return ; //already have it
		}
		
		modules.add(module);
		moduleMap.put(module.name, module);
		
		for(RVariable rv : module.rVariables){
			variableOwnerResolver.put(rv, module);
		}
		
		module.setOwner(this);
		
	}
	
	
	/**
	 * Empties the probability query cache.
	 */
	public void emptyCache(){
		this.queryCache.clear();
		this.logQueryCache.clear();
	}
	
	
	/**
	 * Will return the variable object with the given name.
	 * @param name the name of the variable
	 * @return the variable object of the given name; null if it does not exist.
	 */
	public RVariable getRVarWithName(String name){
		
		for(RVariable rv : inputVariables){
			if(rv.name.equals(name)){
				return rv;
			}
		}
		
		for(RVariable rv : variableOwnerResolver.keySet()){
			if(rv.name.equals(name)){
				return rv;
			}
		}
		
		return null;
	}
	
	
	/**
	 * Will return whatever the cached probability for a query is. If there is no cache entry for the provided query
	 * then it will return null.
	 * @param query the query for which to return the probability
	 * @return the cached query result if it exists, null otherwise.
	 */
	public GMQueryResult getCachedResultForQuery(GMQuery query){
		return this.queryCache.get(query);
	}
	
	
	/**
	 * Will return whatever the cached log probability for a query is. If there is no cache entry for the provided query
	 * then it will return null.
	 * @param query the query for which to return the probability
	 * @return the cached query result if it exists, null otherwise.
	 */
	public GMQueryResult getCachedLoggedResultForQuery(GMQuery query){
		return this.logQueryCache.get(query);
	}
	
	
	/**
	 * Will return the probability of a given query. Currently, this method only supports
	 * computation queries for single variables, but multi-var queries
	 * may be returned if they exist in the cache
	 * @param query the probability query to make
	 * @param cache whether to cache this result if it is queried again the future
	 * @return the probability of this query
	 */
	public GMQueryResult getProb(GMQuery query, boolean cache){
		
		if(query.getNumQueryVars() > 1){
			GMQueryResult cachedRes = this.queryCache.get(query);
			if(cachedRes != null){
				return cachedRes;
			}
			else{
				throw new RuntimeErrorException(new Error("probabilities for multiple query variables cannot be computed, only returned from cache"));
			}
		}
		
		//note that cached check for single vars is performed at the module level (provides consistency for caching iterators)
		RVariable queryVar = query.getSingleQueryVar().owner;
		GMModule module = variableOwnerResolver.get(queryVar);
		GMQueryResult computedResult = module.getProb(query);
		if(cache){
			this.queryCache.put(query, computedResult);
		}
		
		return computedResult;
	}
	
	/**
	 * Will return the log probability of a given query. Currently, this method only supports
	 * computation queries for single variables, but multi-var queries
	 * may be returned if they exist in the cache
	 * @param query the probability query to make
	 * @param cache whether to cache this result if it is queried again the future
	 * @return the probability of this query
	 */
	public GMQueryResult getLogProb(GMQuery query, boolean cache){
		
		if(query.getNumQueryVars() > 1){
			GMQueryResult cachedRes = this.logQueryCache.get(query);
			if(cachedRes != null){
				return cachedRes;
			}
			else{
				throw new RuntimeErrorException(new Error("log probabilities for multiple query variables cannot be computed, only returned from cache"));
			}
		}
		
		//note that cached check for single vars is performed at the module level (provides consistency for caching iterators)
		RVariable queryVar = query.getSingleQueryVar().owner;
		GMModule module = variableOwnerResolver.get(queryVar);
		GMQueryResult computedResult = module.getLogProb(query);
		if(cache){
			this.logQueryCache.put(query, computedResult);
		}
		
		return computedResult;
	}
	
	
	
	
	public GMQueryResult getJointProbWithDiscreteMarginalization(GMQuery query, String [] varNameOrder, boolean cache){
		
		RVariable [] varOrder = this.convertVarNamesArrayToVarArray(varNameOrder);
		return this.getJointProbWithDiscreteMarginalization(query, varOrder, cache);
	}
	
	protected RVariable [] convertVarNamesArrayToVarArray(String [] varNameOrder){
		RVariable [] varOrder = new RVariable[varNameOrder.length];
		for(int i = 0; i < varOrder.length; i++){
			varOrder[i] = this.getRVarWithName(varNameOrder[i]);
		}
		return varOrder;
	}
	
	public GMQueryResult getJointProbWithDiscreteMarginalization(GMQuery query, RVariable [] varOrder, boolean cache){
		
		GMQueryResult cachedRes = this.queryCache.get(query);
		if(cachedRes != null){
			return cachedRes;
		}
		
		
		double p = this.getJointProbWithDiscreteMarginalizationHelper(query, varOrder, 0, cache);
		GMQueryResult result = new GMQueryResult(query, p);
		
		if(cache){
			this.queryCache.put(query, result);
		}
		
		return result;
	}
	
	
	
	public double getJointProbWithDiscreteMarginalizationHelper(GMQuery query, RVariable [] varOrder, int varIndex, boolean cache){
		
		RVariable curVar = varOrder[varIndex];
		
		double pVal = 0.;
		
		//do we need to marginalize over this variable?
		RVariableValue curVarValue = query.getQueryForVariable(curVar);
		if(curVarValue != null){
			//create a single query for this var
			GMQuery sQuery = this.getSingleQueryWithOnlyParentConditionals(curVarValue, query.getConditionValues());
			pVal = this.getProb(sQuery, cache).probability;
			
			if(varIndex < varOrder.length - 1){
				//add this query as a conditional for subsequent CPTs that will pull from it
				GMQuery nextQuery = new GMQuery(query);
				nextQuery.addCondition(curVarValue);
				//nextQuery.removeQuery(curVarValue);
				double nextProb = this.getJointProbWithDiscreteMarginalizationHelper(nextQuery, varOrder, varIndex+1, cache);
				
				pVal *= nextProb;
			}
			
			
			
		}
		else{ //otherwise marginalize
			
			Iterator <GMQueryResult> varIter = this.getNonZeroIterator(curVar, this.onlyParentConditionals(curVar, query.getConditionValues()), cache);
			while(varIter.hasNext()){
				GMQueryResult iterRes = varIter.next();
				double mTerm = iterRes.probability;
				
				if(varIndex < varOrder.length - 1){
					GMQuery nextQuery = new GMQuery(query);
					nextQuery.addCondition(iterRes.getSingleQueryVar());
					double nextProb = this.getJointProbWithDiscreteMarginalizationHelper(nextQuery, varOrder, varIndex+1, cache);
					
					mTerm *= nextProb;
				}
				pVal += mTerm;
			}			
			
		}
		
		
		return pVal;
	}
	
	
	
	
	
	
	
	
	
	public GMQueryResult getJointLogProbWithDiscreteMarginalization(GMQuery query, String [] varNameOrder, boolean cache){
		
		RVariable [] varOrder = this.convertVarNamesArrayToVarArray(varNameOrder);
		return this.getJointLogProbWithDiscreteMarginalization(query, varOrder, cache);
	}
	
	public GMQueryResult getJointLogProbWithDiscreteMarginalization(GMQuery query, RVariable [] varOrder, boolean cache){
		
		GMQueryResult cachedRes = this.logQueryCache.get(query);
		if(cachedRes != null){
			return cachedRes;
		}
		
		
		double lp = this.getJointLogProbWithDiscreteMarginalizationHelper(query, varOrder, 0, cache);
		GMQueryResult result = new GMQueryResult(query, lp);
		
		if(cache){
			this.logQueryCache.put(query, result);
		}
		
		return result;
	}
	
	
	public double getJointLogProbWithDiscreteMarginalizationHelper(GMQuery query, RVariable [] varOrder, int varIndex, boolean cache){
		
		RVariable curVar = varOrder[varIndex];
		
		double logVal = 0.;
		
		//do we need to marginalize over this variable?
		RVariableValue curVarValue = query.getQueryForVariable(curVar);
		if(curVarValue != null){
			//create a single query for this var
			GMQuery sQuery = this.getSingleQueryWithOnlyParentConditionals(curVarValue, query.getConditionValues());
			logVal = this.getLogProb(sQuery, cache).probability;
			if(Double.isNaN(logVal)){
				System.out.println("Nan Error in joint prob");
			}
			if(varIndex < varOrder.length - 1){
				//add this query as a conditional for subsequent CPTs that will pull from it
				GMQuery nextQuery = new GMQuery(query);
				nextQuery.addCondition(curVarValue);
				//nextQuery.removeQuery(curVarValue);
				double nextLog = this.getJointLogProbWithDiscreteMarginalizationHelper(nextQuery, varOrder, varIndex+1, cache);
				if(Double.isNaN(nextLog)){
					System.out.println("Nan Error in joint prob");
				}
				logVal += nextLog;
			}
			
			if(Double.isNaN(logVal)){
				System.out.println("Nan Error in joint prob");
			}
			
		}
		else{ //otherwise marginalize
			
			List <Double> margTerms = new ArrayList<Double>();
			Iterator <GMQueryResult> varIter = this.getNonInfiniteLogProbIterator(curVar, this.onlyParentConditionals(curVar, query.getConditionValues()), cache);
			while(varIter.hasNext()){
				GMQueryResult iterRes = varIter.next();
				double mTerm = iterRes.probability;
				if(Double.isNaN(mTerm)){
					System.out.println("Nan Error in joint prob");
				}
				if(varIndex < varOrder.length - 1){
					GMQuery nextQuery = new GMQuery(query);
					nextQuery.addCondition(iterRes.getSingleQueryVar());
					double nextLog = this.getJointLogProbWithDiscreteMarginalizationHelper(nextQuery, varOrder, varIndex+1, cache);
					if(Double.isNaN(nextLog)){
						System.out.println("Nan Error in joint prob");
					}
					mTerm += nextLog;
				}
				margTerms.add(mTerm);
			}
			
			logVal = LogSumExp.logSumOfExponentials(margTerms);
			
			if(Double.isNaN(logVal)){
				System.out.println("Nan Error in joint prob");
			}
			
		}
		
		
		return logVal;
	}
	
	protected GMQuery getSingleQueryWithOnlyParentConditionals(RVariableValue val, Collection <RVariableValue> conditions){
		GMQuery query = new GMQuery();
		query.addQuery(val);
		for(RVariableValue c : conditions){
			if(val.owner.isDependentOn(c.owner)){
				query.addCondition(c);
			}
		}
		
		return query;
	}
	
	protected List <RVariableValue> onlyParentConditionals(RVariable var, Collection <RVariableValue> srcConditions){
		List <RVariableValue> conditions = new ArrayList<RVariableValue>(srcConditions.size());
		for(RVariableValue c : srcConditions){
			if(var.isDependentOn(c.owner)){
				conditions.add(c);
			}
		}
		return conditions;
	}
	
	
	/**
	 * Gets an iterator of possible variable values (and their probability) that have a non-zero probability.
	 * @param queryVar The variable over which to iterate
	 * @param conditions the conditional variable values
	 * @param cache whether to save the computed probabilities for each iterated variable to the cache
	 * @return an iterator of possible variable values (and their probability) that have a non-zero probability.
	 */
	public Iterator<GMQueryResult> getNonZeroIterator(RVariable queryVar, List <RVariableValue> conditions, boolean cache){
		
		GMModule module = variableOwnerResolver.get(queryVar);
		ModelTrackedVarIterator iter = module.getNonZeroProbIterator(queryVar, conditions);
		iter.GMIniter(this, cache);
		
		return iter;
		
	}
	
	
	/**
	 * Gets an iterator of possible variable values (and their probability) that have a non-zero probability.
	 * @param queryVar The variable over which to iterate
	 * @param conditions the conditional variable values
	 * @param cache whether to save the computed probabilities for each iterated variable to the cache
	 * @return an iterator of possible variable values (and their probability) that have a non-zero probability.
	 */
	public Iterator<GMQueryResult> getNonInfiniteLogProbIterator(RVariable queryVar, List <RVariableValue> conditions, boolean cache){
		
		GMModule module = variableOwnerResolver.get(queryVar);
		ModelTrackedVarIterator iter = module.getNonInfiniteLogProbIterator(queryVar, conditions);
		iter.GMIniter(this, cache);
		
		return iter;
		
	}
	
	
	
	/**
	 * Will return an iterator over all possible variable values for a given random variable
	 * @param queryVar The variable over which to iterate
	 * @return
	 */
	public Iterator<RVariableValue> getRVariableValuesFor(RVariable queryVar){
		GMModule module = variableOwnerResolver.get(queryVar);
		return module.getRVariableValuesFor(queryVar);
	}
	
	
	
	public Set <RVariableValue> getNonInfiniteVariableValues(RVariable var, List <RVariableValue> conditions, String [] varNameOrder){
		RVariable [] varOrder = this.convertVarNamesArrayToVarArray(varNameOrder);
		return this.getNonInfiniteVariableValues(var, conditions, varOrder);
	}
	
	public Set <RVariableValue> getNonInfiniteVariableValues(RVariable var, List <RVariableValue> conditions, RVariable [] varOrder){
		Set <RVariableValue> result = new HashSet<RVariableValue>();
		List <RVariableValue> nConditions = new ArrayList<RVariableValue>(conditions);
		this.getNonInfiniteVariableValuesHelper(var, nConditions, varOrder, result, 0);
		
		return result;
	}
	
	public void getNonInfiniteVariableValuesHelper(RVariable var, List <RVariableValue> conditions, RVariable[] varOrder, Set <RVariableValue> unique, int varIndex){
		
	}
	
	
	
	
	
	/**
	 * Will store a computation result in the generative models cache for future queries
	 * @param result the computed probability to store in the cache
	 */
	public void manualCache(GMQueryResult result){
		this.queryCache.put(new GMQuery(result), result);
	}
	
	
	/**
	 * Will store a computation result in the generative models cache for future queries
	 * @param result the computed probability to store in the cache
	 */
	public void manualLogCache(GMQueryResult result){
		this.logQueryCache.put(new GMQuery(result), result);
	}
	
	
	public GMModule getModuleWithName(String name){
		return this.moduleMap.get(name);
	}
	
	
}
