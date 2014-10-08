package commands.model3;

import behavior.irl.TabularIRL;
import behavior.irl.TabularIRL.TaskCondition;
import behavior.irl.TabularIRLPlannerFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.GroundedProp;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.GroundedAction;
import commands.data.Trajectory;
import commands.data.TrajectoryParser;
import commands.model3.TaskModule.ConjunctiveGroundedPropRF;
import commands.model3.TaskModule.RFConVariableValue;
import generativemodel.*;

import java.io.*;
import java.util.*;

public class TrajectoryModule extends GMModule{

	public static final String								TNAME = "trajectory";
	
	protected RVariable										stateRV;
	protected RVariable										rewardRV;
	protected RVariable										behaviorRV;
	
	protected Domain										oomdpDomain;
	protected TabularIRL									irl;
	
	protected boolean										useIRLCache;
	protected Map<GMQuery, Double>							cachedResults;
	protected Map<GMQuery, Double>							cachedLogResults;
	
	
	public TrajectoryModule(String name, RVariable stateRV, RVariable rewardRV, Domain oomdpDomain, TabularIRLPlannerFactory irlPlanFactory, boolean addTerminateAction, boolean useCache){
		
		super(name);
		
		this.stateRV = stateRV;
		this.rewardRV = rewardRV;
		
		this.externalDependencyList.add(stateRV);
		this.externalDependencyList.add(rewardRV);
		
		
		behaviorRV = new RVariable(TNAME, this);
		behaviorRV.addDependency(rewardRV);
		behaviorRV.addDependency(stateRV);
		
		
		this.oomdpDomain = oomdpDomain;
		this.useIRLCache = useCache;
		
		this.irl = new TabularIRL(oomdpDomain, irlPlanFactory, addTerminateAction);
		this.irl.setTemperature(0.005);
		
		
		cachedResults = new HashMap<GMQuery, Double>();
		cachedLogResults = new HashMap<GMQuery, Double>();
		
	}


	@Override
	public GMQueryResult computeProb(GMQuery query) {
		
		TrajectoryValue bval = (TrajectoryValue)query.getSingleQueryVar();
		
		Set <RVariableValue> conditions = query.getConditionValues();
		RFConVariableValue rval = (RFConVariableValue)this.extractValueForVariableFromConditions(rewardRV, conditions);
		
		double p;
		
		Double cachedP = this.cachedResults.get(query);
		if(cachedP != null){
			p = cachedP;
		}
		else{
			ConjunctiveGroundedPropTF tf = new ConjunctiveGroundedPropTF(rval.rf);
			TaskCondition tc = new TaskCondition(rval.rf, tf);
			
			
			p = irl.getBehaviorProbability(bval.t.convertToZeroRewardEpisodeAnalysis(), tc);
			
			if(this.useIRLCache){
				this.cachedResults.put(query, p);
			}
			
			//System.out.println("t_" + rval.toString() + ": " + p);
			
			
		}
		
		
		
		return new GMQueryResult(query, p);
	}
	
	@Override
	public GMQueryResult getLogProb(GMQuery query){
		
		GMQueryResult cachedResult = owner.getCachedLoggedResultForQuery(query);
		if(cachedResult != null){
			return cachedResult;
		}
		
		return this.computeLogProb(query);
	}
	

	public GMQueryResult computeLogProb(GMQuery query){
		
		TrajectoryValue bval = (TrajectoryValue)query.getSingleQueryVar();
		
		Set <RVariableValue> conditions = query.getConditionValues();
		RFConVariableValue rval = (RFConVariableValue)this.extractValueForVariableFromConditions(rewardRV, conditions);
		
		double p;
		
		Double cachedP = this.cachedLogResults.get(query);
		if(cachedP != null){
			p = cachedP;
		}
		else{
			ConjunctiveGroundedPropTF tf = new ConjunctiveGroundedPropTF(rval.rf);
			TaskCondition tc = new TaskCondition(rval.rf, tf);
			
			p = irl.getBehaviorLogProbability(bval.t.convertToZeroRewardEpisodeAnalysis(), tc);
			
			if(this.useIRLCache){
				this.cachedLogResults.put(query, p);
			}
			
			//System.out.println(rval.toString() + ": " + p);
			
			
		}
		
		
		
		return new GMQueryResult(query, p);
		
	}


	@Override
	public ModelTrackedVarIterator getNonZeroProbIterator(RVariable queryVar,
			List<RVariableValue> conditions) {
		
		//there are a ton of different possible behaviors; so we're not implementing a method
		//to iterate them.
						
		throw new UnsupportedOperationException();
		
	}


	@Override
	public Iterator<RVariableValue> getRVariableValuesFor(RVariable queryVar) {
		//there are a ton of different possible behaviors; so we're not implementing a method
		//to iterate them.
						
		throw new UnsupportedOperationException();
	}
	
	
	
	public void writeCacheToDisk(String pathToCacheDir, Domain domain, StateParser sp){

		if(!pathToCacheDir.endsWith("/")){
			pathToCacheDir += "/";
		}

		TrajectoryParser tp = new TrajectoryParser(domain,sp);

		int queryInd = 0;
		for(Map.Entry<GMQuery, Double> e : this.cachedResults.entrySet()){
			GMQuery query = e.getKey();
			TrajectoryValue bval = (TrajectoryValue)query.getSingleQueryVar();
			Set <RVariableValue> conditions = query.getConditionValues();
			RFConVariableValue rval = (RFConVariableValue)this.extractValueForVariableFromConditions(rewardRV, conditions);

			try {
				BufferedWriter out = new BufferedWriter(new FileWriter(pathToCacheDir+queryInd+".txt"));
				out.write(e.getValue()+"\n");
				out.write(rval.toString()+"\n");
				out.write(tp.getStringRepForTrajectory(bval.t));
				out.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}




			queryInd++;


		}


	}


	public void readCacheFromDisk(String pathToCacheDir, Domain domain, StateParser sp, StateHashFactory hashingFactory){

		TrajectoryParser tp = new TrajectoryParser(domain,sp);

		//get rid of trailing /
		if(pathToCacheDir.charAt(pathToCacheDir.length()-1) == '/'){
			pathToCacheDir = pathToCacheDir.substring(0, pathToCacheDir.length());
		}


		File dir = new File(pathToCacheDir);
		final String ext = new String("txt");

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
			String path = pathToCacheDir + "/" + children[i];

			String content = "";
			try {
				content = new Scanner(new File(path)).useDelimiter("\\Z").next();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				System.exit(-1);
			}

			String remainder = content;

			int nlIndex = remainder.indexOf("\n");
			String pString = remainder.substring(0, nlIndex).trim();
			remainder = remainder.substring(nlIndex+1);


			nlIndex = remainder.indexOf("\n");
			String rfString = remainder.substring(0, nlIndex).trim();
			remainder = remainder.substring(nlIndex+1).trim();

			Trajectory t = tp.getTrajectoryFromString(remainder);

			double prob = Double.parseDouble(pString);

			List<GroundedProp> gps = parseGPsFromString(rfString, domain);
			ConjunctiveGroundedPropRF cgprf = new ConjunctiveGroundedPropRF();
			for(GroundedProp gp : gps){
				cgprf.addGP(gp);
			}

			RFConVariableValue rfVar = new RFConVariableValue(this.owner.getRVarWithName(TaskModule.GROUNDEDRFNAME), cgprf);

			TaskModule.StateRVValue sval = new TaskModule.StateRVValue(t.getState(0), hashingFactory, this.owner.getRVarWithName(TaskModule.STATENAME));

			TrajectoryValue tv = new TrajectoryValue(t, behaviorRV);

			GMQuery query = new GMQuery();
			query.addQuery(tv);
			query.addCondition(sval);
			query.addCondition(rfVar);

			this.cachedResults.put(query, prob);

		}

	}


	private static List<GroundedProp> parseGPsFromString(String str, Domain domain){

		String [] gpComps = str.split("\\) ");
		List<GroundedProp> gps = new ArrayList<GroundedProp>();
		for(String gp : gpComps){
			gps.add(parseGPFromString(gp, domain));
		}

		return gps;

	}

	private static GroundedProp parseGPFromString(String str, Domain domain){
		if(!str.endsWith(")")){
			str = str + ")";
		}

		int lParenInd = str.indexOf("(");
		String pName = str.substring(0, lParenInd);

		String paramString = str.substring(lParenInd+1, str.length()-1);

		String [] params = paramString.split(", ");

		GroundedProp gp = new GroundedProp(domain.getPropFunction(pName), params);


		return gp;


	}
	
	
	
	
	
	
	
	public static class TrajectoryValue extends RVariableValue{

		public Trajectory		t;
		
		public TrajectoryValue(Trajectory t, RVariable owner){
			
			this.t = t;
			
			this.setOwner(owner);
		}
		
		@Override
		public boolean valueEquals(RVariableValue other) {
			
			if(this == other){
				return true;
			}
			
			if(!(other instanceof TrajectoryValue)){
				return false;
			}
			
			TrajectoryValue that = (TrajectoryValue)other;
			
			if(this.t.numStates() != that.t.numStates()){
				return false;
			}
			
			for(int i = 0; i < this.t.numStates(); i++){
				State s = this.t.getState(i);
				State ts = that.t.getState(i);
				if(!s.equals(ts)){
					return false;
				}
				
				if(i < this.t.numStates()-1){
					GroundedAction ga = this.t.getAction(i);
					GroundedAction tga = that.t.getAction(i);
					
					if(!ga.equals(tga)){
						return false;
					}
					
				}
				
			}
			
			
			return true;
		}

		@Override
		public String stringRep() {
			
			StringBuffer buf = new StringBuffer();
			
			for(int i = 0; i < this.t.numStates(); i++){
				buf.append(t.getState(i).getCompleteStateDescription()).append("\n");
				if(i < this.t.numStates()-1){
					buf.append(t.getAction(i).toString()).append("\n");
				}
			}
			
			
			return buf.toString();
		}



		@Override
		public String toString(){
			return this.stringRep();
		}
		
		
		
		
	}
	
	
	
	
	public static class ConjunctiveGroundedPropTF implements TerminalFunction{
		
		public List <GroundedProp>			gps;

		public ConjunctiveGroundedPropTF(List <GroundedProp> gps){
			this.gps = gps;
		}
		
		public ConjunctiveGroundedPropTF(ConjunctiveGroundedPropRF rf){
			this.gps = new ArrayList<GroundedProp>(rf.gps);
		}
		
		@Override
		public String toString(){
			StringBuffer buf = new StringBuffer();
			buf.append(gps.get(0).toString());
			for(int i = 1; i < gps.size(); i++){
				buf.append(" ^ ").append(gps.get(i).toString());
			}
			return buf.toString();
		}
		
		
		@Override
		public boolean isTerminal(State s) {
			for(GroundedProp gp : gps){
				if(!gp.isTrue(s)){
					return false;
				}
			}
			
			return true;
		}
		
	}
	
	

}
