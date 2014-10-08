package commands.model3;

import generativemodel.GMModule;
import generativemodel.GMQuery;
import generativemodel.GMQueryResult;
import generativemodel.ModelTrackedVarIterator;
import generativemodel.RVariable;
import generativemodel.RVariableValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.GroundedProp;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

public class TaskModule extends GMModule {

	public static final String		STATENAME = "state";
	public static final String		LIFTEDRFNAME = "liftedRF";
	public static final String		GROUNDEDRFNAME = "groundedRF";
	public static final String		BINDINGNAME = "bindingConstraint";
	
	protected RVariable				stateVariable;
	protected RVariable				liftedRFVariable;
	protected RVariable				groundedRFVariable;
	protected RVariable				bindingConstraintVariable;
	
	protected List<LiftedVarValue>	liftedRFValues;
	
	protected Domain				domain;
	
	protected boolean				permitInitiallySatisfiedRFs = false;
	protected int					maxBindingConstraintsComponentSize = 12;
	
	
	public TaskModule(String name, Domain domain) {
		super(name);
		
		this.stateVariable = new RVariable(STATENAME, this);
		
		this.liftedRFVariable = new RVariable(LIFTEDRFNAME, this);
		this.liftedRFVariable.addDependency(this.stateVariable);
		
		this.groundedRFVariable = new RVariable(GROUNDEDRFNAME, this);
		this.groundedRFVariable.addDependency(this.stateVariable);
		this.groundedRFVariable.addDependency(this.liftedRFVariable);
		
		this.bindingConstraintVariable = new RVariable(BINDINGNAME, this);
		this.bindingConstraintVariable.addDependency(this.stateVariable);
		this.bindingConstraintVariable.addDependency(this.liftedRFVariable);
		this.bindingConstraintVariable.addDependency(this.groundedRFVariable);
		
		this.liftedRFValues = new ArrayList<TaskModule.LiftedVarValue>();
		this.domain = domain;
		
	}
	
	public void addLiftedVarValue(LiftedVarValue val){
		this.liftedRFValues.add(val);
	}

	@Override
	public GMQueryResult computeProb(GMQuery query) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ModelTrackedVarIterator getNonZeroProbIterator(RVariable queryVar,
			List<RVariableValue> conditions) {
		
		if(queryVar.equals(this.liftedRFVariable)){
			return new LiftedRFIterator(conditions); 
		}
		else if(queryVar.equals(this.groundedRFVariable)){
			return new GroundedRFIterator(conditions);
		}
		else if(queryVar.equals(this.bindingConstraintVariable)){
			return new ConstraintIterator(conditions);
		}
		
		throw new RuntimeException("Unknown query var: " + queryVar.getName());
	}

	@Override
	public Iterator<RVariableValue> getRVariableValuesFor(RVariable queryVar) {
		throw new UnsupportedOperationException();
	}

	
	
	
	
	
	
	public static class StateRVValue extends RVariableValue{
		
		public State				s;
		public StateHashTuple		sh;

		public StateRVValue(State s, StateHashFactory factory, RVariable owner){
			this.s = s;
			this.sh = factory.hashState(s);
			
			this.setOwner(owner);
		}
		
		public StateRVValue(StateHashTuple sh, RVariable owner){
			this.s = sh.s;
			this.sh = sh;
			
			this.setOwner(owner);
		}
		
		
		@Override
		public boolean valueEquals(RVariableValue other) {
			
			if(this == other){
				return true;
			}
			
			if(!(other instanceof StateRVValue)){
				return false;
			}
			
			
			return ((StateRVValue)other).sh.equals(this.sh);
			
		}

		@Override
		public String stringRep() {
			return s.getCompleteStateDescription();
		}
		
		@Override
		public int hashCode(){
			return this.sh.hashCode();
		}
			
	}
	
	
	public static class LiftedVarValue extends RVariableValue{

		public Map<String, String>			freeVariableToClassMapping;
		public List<GroundedProp>			conditions;
		
		public LiftedVarValue(RVariable owner){
			this.freeVariableToClassMapping = new HashMap<String, String>();
			this.conditions = new ArrayList<GroundedProp>();
			this.setOwner(owner);
		}
		
		public LiftedVarValue(RVariable owner, List<GroundedProp> gps){
			this.freeVariableToClassMapping = new HashMap<String, String>();
			this.conditions = new ArrayList<GroundedProp>();
			for(GroundedProp gp : gps){
				this.addProp(gp);
			}
			this.setOwner(owner);
		}
		
		public void addProp(GroundedProp gp){
			this.conditions.add(gp);
			String [] types = gp.pf.getParameterClasses();
			for(int i = 0; i < gp.params.length; i++){
				String freeVar = gp.params[i];
				String type = types[i];
				this.freeVariableToClassMapping.put(freeVar, type);
			}
		}
		
		public int numComps(){
			int num = 0;
			for(GroundedProp gp : this.conditions){
				num += 1 + gp.params.length;
			}
			return num;
		}
		
		
		@Override
		public boolean valueEquals(RVariableValue other) {
			
			if(this == other){
				return true;
			}
			
			if(!(other instanceof LiftedVarValue)){
				return false;
			}
			
			LiftedVarValue olr = (LiftedVarValue)other;
			if(olr.conditions.size() != this.conditions.size()){
				return false;
			}
			
			for(int i = 0; i < this.conditions.size(); i++){
				GroundedProp tgp = this.conditions.get(i);
				GroundedProp ogp = olr.conditions.get(i);
				if(!tgp.equals(ogp)){
					return false;
				}
			}
			
			return true;
		}

		@Override
		public String stringRep() {
			
			StringBuffer buf = new StringBuffer(256);
			for(int i = 0; i < this.conditions.size(); i++){
				if(i > 0){
					buf.append(" ");
				}
				buf.append(this.conditions.get(i).toString());
			}
			
			return buf.toString();
		}
		
		
		
	}
	
	
	public static class ConjunctiveGroundedPropRF implements RewardFunction{

		public List <GroundedProp>			gps;
		public double						goalR = 1.;
		public double						nonGoalR = 0.;
		
		public ConjunctiveGroundedPropRF(){
			this.gps = new ArrayList<GroundedProp>();
		}
		
		public ConjunctiveGroundedPropRF(List <GroundedProp> gps){
			this.gps = gps;
		}
		
		public void addGP(GroundedProp gp){
			this.gps.add(gp);
		}
		
		
		@Override
		public double reward(State s, GroundedAction ga, State sprime) {
		
			if(this.satisfied(sprime)){
				return goalR;
			}
			
			return nonGoalR;
		}
		
		
		public boolean satisfied(State s){
			for(GroundedProp gp : gps){
				if(!gp.isTrue(s)){
					return false;
				}
			}
			return true;
		}
		
		
		@Override
		public boolean equals(Object other){
			
			if(this == other){
				return true;
			}
			
			if(!(other instanceof ConjunctiveGroundedPropRF)){
				return false;
			}
			
			ConjunctiveGroundedPropRF orf = (ConjunctiveGroundedPropRF)other;
			
			if(this.goalR != orf.goalR){
				return false;
			}
			
			if(this.nonGoalR != orf.nonGoalR){
				return false;
			}
			
			if(this.gps.size() != orf.gps.size()){
				return false;
			}
			
			for(GroundedProp gp : this.gps){
				if(!orf.gps.contains(gp)){
					return false;
				}
			}
			
			return true;
			
		}
		
		
	}
	
	
	
	public static class RFConVariableValue extends RVariableValue{

		public ConjunctiveGroundedPropRF			rf;
		
		
		public RFConVariableValue(RVariable owner){
			rf = new ConjunctiveGroundedPropRF();
			this.setOwner(owner);
		}
		
		public RFConVariableValue(RVariable owner, ConjunctiveGroundedPropRF rf){
			this.rf = rf;
			this.setOwner(owner);
		}
		
		public void addGoalGP(GroundedProp gp){
			this.rf.addGP(gp);
		}
		
		
		@Override
		public boolean valueEquals(RVariableValue other) {
			
			if(this == other){
				return true;
			}
			
			if(!(other instanceof RFConVariableValue)){
				return false;
			}
			
			RFConVariableValue that = (RFConVariableValue)other;
			
			for(GroundedProp gp : this.rf.gps){
				boolean foundMatch = false;
				for(GroundedProp tgp : that.rf.gps){
					if(gp.equals(tgp)){
						foundMatch = true;
						break;
					}
				}
				if(!foundMatch){
					return false;
				}
			}
			
			return true;
		}

		@Override
		public String stringRep() {
			
			StringBuffer buf = new StringBuffer();
			
			for(GroundedProp gp : this.rf.gps){
				buf.append(gp.toString() + " ");
			}
			
			return buf.toString();
		}
			
		
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	public class LiftedRFIterator extends ModelTrackedVarIterator{

		Iterator<GMQueryResult>			sourceIterator;
		
		public LiftedRFIterator(List<RVariableValue> conditions){
			
			StateRVValue stateVal = null;
			
			for(RVariableValue val : conditions){
				if(val.getOwner().equals(stateVariable)){
					stateVal = (StateRVValue)val;
				}
			}
			
			List<GMQueryResult> res = new ArrayList<GMQueryResult>(TaskModule.this.liftedRFValues.size());
			for(LiftedVarValue val : TaskModule.this.liftedRFValues){
				Map<String, Integer> numOfEachObject = new HashMap<String, Integer>();
				Set<String> seenVars = new HashSet<String>();
				for(GroundedProp gp : val.conditions){
					String [] paramClasses = gp.pf.getParameterClasses();
					for(int i = 0; i < gp.params.length; i++){
						if(!seenVars.contains(gp.params[i])){
							//then it's a new variable
							String cname = paramClasses[i];
							Integer storedCount = numOfEachObject.get(cname);
							int sc = storedCount != null ? storedCount : 0;
							numOfEachObject.put(cname, sc++);
						}
					}
				}
				boolean possibleInState = true;
				for(Entry<String, Integer> e : numOfEachObject.entrySet()){
					if(e.getValue() > stateVal.s.getObjectsOfTrueClass(e.getKey()).size()){
						possibleInState = false;
						break;
					}
				}
				
				if(possibleInState){
					GMQueryResult r = new GMQueryResult();
					r.addQuery(val);
					r.addCondition(stateVal);
					res.add(r);
				}
				
			}
			
			double uni = 1. / res.size();
			for(GMQueryResult r : res){
				r.probability = uni;
			}
			
			this.sourceIterator = res.iterator();
			
		}
		
		@Override
		public boolean hasNext() {
			return this.sourceIterator.hasNext();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public GMQueryResult varSpecificNext() {
			return this.sourceIterator.next();
		}
		
		
		
	}
	
	
	
	
	public class GroundedRFIterator extends ModelTrackedVarIterator{

		protected StateRVValue				stateVal;
		protected LiftedVarValue			liftedRFVal;
		
		protected List<RFConVariableValue>	rfVals;
		protected int 						next = 0;
		protected double					uniProb;
		
		public GroundedRFIterator(List<RVariableValue> conditions){
			
			for(RVariableValue val : conditions){
				if(val.getOwner().equals(stateVariable)){
					this.stateVal = (StateRVValue)val;
				}
				else if(val.getOwner().equals(liftedRFVariable)){
					this.liftedRFVal = (LiftedVarValue)val;
				}
			}
			
			
			String [] paramClasses = new String[liftedRFVal.freeVariableToClassMapping.size()];
			String [] paramOrderGroups = new String[liftedRFVal.freeVariableToClassMapping.size()];
			int i = 0;
			for(Map.Entry<String, String> e : liftedRFVal.freeVariableToClassMapping.entrySet()){
				paramClasses[i] = e.getValue();
				paramOrderGroups[i] = e.getKey();
				i++;
			}
			
			List<List<String>> linearMappings = stateVal.s.getPossibleBindingsGivenParamOrderGroups(paramClasses, paramOrderGroups);
			
			List<Map<String, String>> mappings = new ArrayList<Map<String,String>>(linearMappings.size());
			for(List<String> lmap : linearMappings){
				Map<String, String> mapping = new HashMap<String, String>(lmap.size());
				for(i = 0; i < lmap.size(); i++){
					mapping.put(paramOrderGroups[i], lmap.get(i));
				}
				mappings.add(mapping);
			}
			
			this.rfVals = new ArrayList<TaskModule.RFConVariableValue>(mappings.size());
			for(Map<String, String> nextMap : mappings){
				RFConVariableValue rfVal = new RFConVariableValue(groundedRFVariable);
				for(GroundedProp lgp : this.liftedRFVal.conditions){
					String [] gparams = new String[lgp.params.length];
					for(i = 0; i < lgp.params.length; i++){
						gparams[i] = nextMap.get(lgp.params[i]);
					}
					GroundedProp ggp = new GroundedProp(lgp.pf, gparams);
					rfVal.addGoalGP(ggp);
				}
				
				
				//this enforces that only rfs not satisfied in the initial state added unless otherwise specified
				if(!rfVal.rf.satisfied(stateVal.s) || permitInitiallySatisfiedRFs){
					rfVals.add(rfVal);
				}
			}
			
			this.uniProb  = 1. / this.rfVals.size();
			
		}
		
		@Override
		public boolean hasNext() {
			return this.next < this.rfVals.size();
		}

		@Override
		public GMQueryResult varSpecificNext() {
			
			RFConVariableValue rfVal = this.rfVals.get(this.next);
			next++;
			
			GMQueryResult res = new GMQueryResult(uniProb);
			res.addQuery(rfVal);
			res.addCondition(stateVal);
			res.addCondition(liftedRFVal);
			
			return res;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
			
	}
	
	
	
	public class ConstraintIterator extends ModelTrackedVarIterator{

		protected StateRVValue				stateVal;
		protected LiftedVarValue			liftedRFVal;
		protected RFConVariableValue		groundedRFVal;
		
		protected List<LiftedVarValue>		bindingConstraints;
		protected int						next = 0;
		protected double					uni = 0.;
		
		public ConstraintIterator(List<RVariableValue> conditions){
			
			for(RVariableValue val : conditions){
				if(val.getOwner().equals(stateVariable)){
					this.stateVal = (StateRVValue)val;
				}
				else if(val.getOwner().equals(liftedRFVariable)){
					this.liftedRFVal = (LiftedVarValue)val;
				}
				else if(val.getOwner().equals(groundedRFVariable)){
					this.groundedRFVal = (RFConVariableValue)val;
				}
			}
			
			this.bindingConstraints = new ArrayList<TaskModule.LiftedVarValue>();
			
			List<GroundedProp> allTruePFs = new ArrayList<GroundedProp>();
			List<PropositionalFunction> pfs = domain.getPropFunctions();
			for(PropositionalFunction pf : pfs){
				List <GroundedProp> gps = stateVal.s.getAllGroundedPropsFor(pf);
				for(GroundedProp gp : gps){
					if(gp.isTrue(stateVal.s)){
						allTruePFs.add(gp);
					}
				}
			}
			
			Set<String> groundedVars = new HashSet<String>();
			for(GroundedProp gp : groundedRFVal.rf.gps){
				for(String v : gp.params){
					groundedVars.add(v);
				}
			}
			
			List<GroundedProp> pureProps = new ArrayList<GroundedProp>(allTruePFs.size());
			List<GroundedProp> connectiveProps = new ArrayList<GroundedProp>(allTruePFs.size());
			List<GroundedProp> pureUnused = new ArrayList<GroundedProp>(allTruePFs.size());
			
			for(GroundedProp gp : allTruePFs){
				int code = this.varMembership(gp, groundedVars);
				if(code == 2){
					pureProps.add(gp);
				}
				else if(code == 1){
					connectiveProps.add(gp);
				}
				else{
					pureUnused.add(gp);
				}
			}
			
			
			
			Map<String, String> groundedToFree = this.getGroundedToFree();
			List<List<GroundedProp>> pureSetGPs = this.combinations(pureProps);
			List<LiftedVarValue> pureSet = new ArrayList<TaskModule.LiftedVarValue>(pureSetGPs.size());
			pureSet.add(this.getLiftedVarValue(new ArrayList<GroundedProp>(), groundedToFree)); //add zero constraint
			this.bindingConstraints.add(pureSet.get(0));
			for(List<GroundedProp> pgp : pureSetGPs){
				LiftedVarValue val = this.getLiftedVarValue(pgp, groundedToFree);
				pureSet.add(val);
				if(val.numComps() <= TaskModule.this.maxBindingConstraintsComponentSize){
					this.bindingConstraints.add(val);
				}
			}
			
			
			
			List<List<GroundedProp>> extendedConstraintSets = new ArrayList<List<GroundedProp>>(connectiveProps.size());
			for(GroundedProp cp : connectiveProps){
				
				Set<String> extendedVars = new HashSet<String>(groundedVars);
				for(String p : cp.params){
					extendedVars.add(p);
				}
				
				List<GroundedProp> extendedConstraints = new ArrayList<GroundedProp>();
				for(GroundedProp gp : pureUnused){
					int code = this.varMembership(gp, extendedVars);
					if(code == 2){
						extendedConstraints.add(gp);
					}
				}
				
				extendedConstraintSets.add(extendedConstraints);
				
			}
			
			//do the below to add the connection possibilities
			List<List<Integer>> combinationsOfConnections = this.combinationIndices(connectiveProps.size());
			for(List<Integer> combination : combinationsOfConnections){
				
				List<GroundedProp> possibleExtendedConstraints = new ArrayList<GroundedProp>();
				List<GroundedProp> connections = new ArrayList<GroundedProp>();
				for(int ind : combination){
					connections.add(connectiveProps.get(ind));
					for(GroundedProp gp : extendedConstraintSets.get(ind)){
						possibleExtendedConstraints.add(gp);
					}
				}
				
				Set<String> newVars = this.newVars(groundedVars, connections);
				
				List<List<GroundedProp>> extensionGPCombinations = this.combinations(possibleExtendedConstraints);
				for(List<GroundedProp> aComb : extensionGPCombinations){
					
					//make sure that all the extended free variables appear somewhere in this list of props, otherwise it's meaningless to add this connection
					if(allVarsAppearSomewhere(newVars, aComb)){
						
						//add a new constrain that attaches to the null pure constraint, along with the new constraints connections
						List<GroundedProp> fullExtended = new ArrayList<GroundedProp>(connections.size() + aComb.size());
						fullExtended.addAll(connections);
						fullExtended.addAll(aComb);
						LiftedVarValue val = this.getLiftedVarValue(fullExtended, groundedToFree);
						if(val.numComps() <= TaskModule.this.maxBindingConstraintsComponentSize){
							this.bindingConstraints.add(val);
						}
						
						//then we add a new constraint set for each of the previous pure constraints, along with their connections
						for(List<GroundedProp> pc : pureSetGPs){
							fullExtended = new ArrayList<GroundedProp>(pc.size() + connections.size() + aComb.size());
							fullExtended.addAll(pc);
							fullExtended.addAll(connections);
							fullExtended.addAll(aComb);
							val = this.getLiftedVarValue(fullExtended, groundedToFree);
							if(val.numComps() <= TaskModule.this.maxBindingConstraintsComponentSize){
								this.bindingConstraints.add(val);
							}
						}
					}
					
				}
				
				
			}
			
			
			
			this.uni = 1. / this.bindingConstraints.size();
			
		}
		
		
		@Override
		public boolean hasNext() {
			return next < this.bindingConstraints.size();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public GMQueryResult varSpecificNext() {
			
			GMQueryResult res = new GMQueryResult(this.uni);
			res.addQuery(this.bindingConstraints.get(this.next));
			res.addCondition(stateVal);
			res.addCondition(liftedRFVal);
			res.addCondition(groundedRFVal);
			
			next++;
			return res;
		}
		
		
		
		/**
		 * Returns 0 if none of the object variable names in vars are parameters in gp; 1 at least 1, but not all of the parameters are in vars; 2 if all of the params are in vars
		 * @param gp the grounded prop whose paramerters should be checked
		 * @param vars the set of object variable names to check against
		 * @return 0 if none of the object variable names in vars are parameters in gp; 1 at least 1, but not all of the parameters are in vars; 2 if all of the params are in vars
		 */
		protected int varMembership(GroundedProp gp, Set<String> vars){
			
			boolean foundAMatch = false;
			boolean allMatch = true;
			for(String p : gp.params){
				if(vars.contains(p)){
					foundAMatch = true;
				}
				else{
					allMatch = false;
				}
			}
			
			if(allMatch){
				return 2;
			}
			if(foundAMatch){
				return 1;
			}
			
			return 0;
		}
		
		
		protected List<List<GroundedProp>> combinations(List<GroundedProp> source){
			
			int max = (int)Math.pow(2, source.size());
			List<List<GroundedProp>> allCombs = new ArrayList<List<GroundedProp>>(max-1);
			for(int i = 1; i < max; i++){
				List<GroundedProp> comb = new ArrayList<GroundedProp>(source.size());
				String brep = Integer.toBinaryString(i);
				brep = sRep("0", source.size() - brep.length()) + brep;
				for(int j = 0; j < brep.length(); j++){
					if(brep.charAt(j) == '1'){
						comb.add(source.get(j));
					}
				}
				allCombs.add(comb);
			}
			
			return allCombs;
			
		}
		
		protected List<List<Integer>> combinationIndices(int numOptions){
			int max = (int)Math.pow(2, numOptions);
			List<List<Integer>> allCombs = new ArrayList<List<Integer>>();
			for(int i = 1; i < max; i++){
				List<Integer> comb = new ArrayList<Integer>(numOptions);
				String brep = Integer.toBinaryString(i);
				brep = sRep("0", numOptions - brep.length()) + brep;
				for(int j = 0; j < brep.length(); j++){
					if(brep.charAt(j) == '1'){
						comb.add(j);
					}
				}
				allCombs.add(comb);
			}
			return allCombs;
			
		}
		
		protected String sRep(String c, int n){
			StringBuffer buf = new StringBuffer(n);
			for(int i = 0; i < n; i++){
				buf.append(c);
			}
			return buf.toString();
		}
		
		
		protected Map<String, String> getGroundedToFree(){
			Map<String, String> reversed = new HashMap<String, String>(this.liftedRFVal.freeVariableToClassMapping.size());
			
			for(int i = 0; i < this.liftedRFVal.conditions.size(); i++){
				GroundedProp lGP = this.liftedRFVal.conditions.get(i);
				GroundedProp gGP = this.groundedRFVal.rf.gps.get(i);
				for(int j = 0; j < lGP.params.length; j++){
					reversed.put(gGP.params[j], lGP.params[j]);
				}
			}
			
			return reversed;
		}
		
		
		protected LiftedVarValue getLiftedVarValue(List<GroundedProp> groundedProps, Map<String, String> groundedToFree){
			
			Map<String, String> auxGroundedToFree = new HashMap<String, String>(groundedToFree);
			LiftedVarValue val = new LiftedVarValue(bindingConstraintVariable);
			for(GroundedProp gp : groundedProps){
				String [] fparams = new String[gp.params.length];
				for(int i = 0; i < fparams.length; i++){
					String freeVar = auxGroundedToFree.get(gp.params[i]);
					if(freeVar == null){
						String cname = gp.pf.getParameterClasses()[i];
						int id = 0;
						freeVar = cname + "_ext" + id;
						while(auxGroundedToFree.containsKey(freeVar)){
							id++;
							freeVar = cname + "_ext" + id;
						}
						auxGroundedToFree.put(gp.params[i], freeVar);
					}
					fparams[i] = freeVar;
				}
				GroundedProp fgp = new GroundedProp(gp.pf, fparams);
				val.addProp(fgp);
			}
			
			return val;
		}
		
		
		protected Set<String> newVars(Set<String> normalVars, List<GroundedProp> constraints){
			Set<String> newVars = new HashSet<String>();
			for(GroundedProp gp : constraints){
				for(String p : gp.params){
					if(!normalVars.contains(p)){
						newVars.add(p);
					}
				}
			}
			
			return newVars;
		}
		
		protected boolean allVarsAppearSomewhere(Set<String> vars, List<GroundedProp> gps){
			Set<String> unseen = new HashSet<String>(vars);
			for(GroundedProp gp : gps){
				for(String p : gp.params){
					unseen.remove(p);
				}
			}
			return unseen.size() == 0;
		}
		
	
	}
	
	
}
