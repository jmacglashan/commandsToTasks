package behavior.planning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.planning.ValueFunctionPlanner;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

public class DeterministicGoalDirectedPartialVI extends ValueFunctionPlanner
		implements QComputablePlanner {
	
	
	protected Map <StateHashTuple, Integer>							distanceFunction;

	public DeterministicGoalDirectedPartialVI(Domain domain, RewardFunction rf, TerminalFunction tf, double gamma, StateHashFactory hashingFactory){
		this.plannerInit(domain, rf, tf, gamma, hashingFactory);
		this.distanceFunction = new HashMap<StateHashTuple, Integer>();
	}
	
	public boolean planDefinedForState(State s){
		return this.distanceFunction.containsKey(this.hashingFactory.hashState(s));
	}
	
	@Override
	public void planFromState(State initialState) {
		
		StateHashTuple shi = this.stateHash(initialState);
		
		if(distanceFunction.containsKey(shi)){
			return ; //no plannning needed
		}
		
		if(tf.isTerminal(initialState)){
			this.distanceFunction.put(shi, 0);
			return ; //no searching needed
		}
		
		MultiBPtrSearchNode sni = new MultiBPtrSearchNode(shi);
		
		LinkedList<MultiBPtrSearchNode> openList = new LinkedList<DeterministicGoalDirectedPartialVI.MultiBPtrSearchNode>();
		Map <StateHashTuple, MultiBPtrSearchNode> openMap = new HashMap<StateHashTuple, DeterministicGoalDirectedPartialVI.MultiBPtrSearchNode>();
		Set<StateHashTuple> closedSet = new HashSet<StateHashTuple>();
		Map <StateHashTuple, Integer> cost = new HashMap<StateHashTuple, Integer>();
		
		List<MultiBPtrSearchNode> nearestCachedSolutions = new ArrayList<DeterministicGoalDirectedPartialVI.MultiBPtrSearchNode>();
		int cachedSolutionQuality = Integer.MAX_VALUE;
		MultiBPtrSearchNode goalNode = null;
		
		openList.offer(sni);
		openMap.put(shi, sni);
		cost.put(shi, 0);
		
		
		while(openList.size() > 0){
			
			MultiBPtrSearchNode sn = openList.poll();
			openMap.remove(sn.sh);
			closedSet.add(sn.sh);
			int csn = cost.get(sn.sh);
			
			if(nearestCachedSolutions.size() > 0){
				//then is the nearest cached better?
				if(csn == cachedSolutionQuality){
					//then the cached solutions offers the best paths!
					break;
				}
			}
			
			//determine if this was a goal state (indicated by a positive reward value and being a terminal state)
			if(tf.isTerminal(sn.sh.s) && rf.reward(sn.backPtrs.get(0).sh.s, sn.generartingActions.get(0), sn.sh.s) > 0.){
				goalNode = sn;
				break; //we're finished!
			}
			else if(tf.isTerminal(sn.sh.s)){
				continue; //this is a dead end, but not a goal; move on to the next state in the queue
			}
			
			int csnp = csn + 1;
			
			//otherwise we need to expand
			for(Action a : actions){
				List <GroundedAction> gas = sn.sh.s.getAllGroundedActionsFor(a);
				for(GroundedAction ga : gas){
					StateHashTuple shp = this.stateHash(ga.executeIn(sn.sh.s));
					if(closedSet.contains(shp)){
						continue; //already found better path to this node;
					}
					if(!this.distanceFunction.containsKey(shp)){
						//then we don't have a pre-cached solution and should explore this node
						//do we already have it in our queue?
						MultiBPtrSearchNode snp = openMap.get(shp);
						if(snp == null){
							//wholly new node
							snp = new MultiBPtrSearchNode(shp, sn, ga);
							openList.offer(snp);
							openMap.put(shp, snp);
							cost.put(shp, csnp);
						}
						else{
							//we already have an open entry for this, but this must be a equally good path to it, so add the back ptr
							snp.addBPtr(sn, ga);
						}
					}
					else{
						
						//then we have a cached solution for this state; add it if it's as good or better
						int d = this.distanceFunction.get(shp);
						int candidateSolution = csnp + d;
						if(candidateSolution < cachedSolutionQuality){
							MultiBPtrSearchNode snp = new MultiBPtrSearchNode(shp, sn, ga);
							nearestCachedSolutions.clear();
							nearestCachedSolutions.add(snp);
							cachedSolutionQuality = candidateSolution;
						}
						else if(candidateSolution == cachedSolutionQuality){
							//is this another path to a previously found solution?
							MultiBPtrSearchNode snp = this.getNodeForState(shp, nearestCachedSolutions);
							if(snp != null){
								snp.addBPtr(sn, ga);
							}
							else{
								snp = new MultiBPtrSearchNode(shp, sn, ga);
							}
						}
						
					}
					
				}
			}
			
			
			
			
		}
		
		
		Set <StateHashTuple> markedNodes = new HashSet<StateHashTuple>();
		if(goalNode != null){
			this.setDistanceFunctionFromSolutions(goalNode, cost.get(goalNode.sh), cost, markedNodes);
		}
		else if(nearestCachedSolutions.size() > 0){
			//we don't need to update the values for the cachedNodes, only each of their generators
			for(MultiBPtrSearchNode sn : nearestCachedSolutions){
				for(MultiBPtrSearchNode bptr : sn.backPtrs){
					this.setDistanceFunctionFromSolutions(bptr, cachedSolutionQuality, cost, markedNodes);
				}
			}
		}

	}

	
	
	protected void setDistanceFunctionFromSolutions(MultiBPtrSearchNode sourceNode, int totalDistance, Map <StateHashTuple, Integer> cost, Set <StateHashTuple> markedNodes){
		
		if(markedNodes.contains(sourceNode.sh)){
			return ; //already cached this distance and all found paths to it
		}
		
		this.distanceFunction.put(sourceNode.sh, totalDistance-cost.get(sourceNode.sh));
		markedNodes.add(sourceNode.sh);
		
		for(MultiBPtrSearchNode bptr : sourceNode.backPtrs){
			this.setDistanceFunctionFromSolutions(bptr, totalDistance, cost, markedNodes);
		}
		
	}
	
	protected MultiBPtrSearchNode getNodeForState(StateHashTuple sh, List <MultiBPtrSearchNode> nodes){
		
		for(MultiBPtrSearchNode node : nodes){
			if(node.sh.equals(sh)){
				return node;
			}
		}
		
		return null;
	}
	

	@Override
	public List<QValue> getQs(State s) {
		
		List <QValue> res = new ArrayList<QValue>();
		
		for(Action a : actions){
			List <GroundedAction> gas = s.getAllGroundedActionsFor(a);
			for(GroundedAction ga : gas){
				res.add(this.getQ(s, ga));
			}
		}
		
		return res;
	}

	@Override
	public QValue getQ(State s, AbstractGroundedAction a) {
		
		State sp = a.executeIn(s);
		StateHashTuple shp = this.stateHash(sp);
		double r = rf.reward(s, (GroundedAction)a, sp);
		double vp = this.vFor(shp);
		double q = r + (this.gamma*vp);
		
		QValue Q = new QValue(s, a, q);
		
		return Q;
	}
	
	
	
	protected double vFor(StateHashTuple sh){
		
		Integer D = this.distanceFunction.get(sh);
		if(D == null){
			return 0.; //solution not reachable from this state
		}
		int d = D;		
				
		if(d == 0){
			return 0.;
		}
		
		double v = Math.pow(gamma, d-1);
		
		return v;
		
	}
	
	
	class MultiBPtrSearchNode{
		
		public StateHashTuple sh;
		public List<MultiBPtrSearchNode> backPtrs;
		public List<GroundedAction> generartingActions;
		
		public MultiBPtrSearchNode(StateHashTuple sh){
			this.sh = sh;
			this.backPtrs = new ArrayList<DeterministicGoalDirectedPartialVI.MultiBPtrSearchNode>();
			this.generartingActions = new ArrayList<GroundedAction>();
		}
		
		public MultiBPtrSearchNode(StateHashTuple sh, MultiBPtrSearchNode bptr, GroundedAction ga){
			this.sh = sh;
			this.backPtrs = new ArrayList<DeterministicGoalDirectedPartialVI.MultiBPtrSearchNode>();
			this.generartingActions = new ArrayList<GroundedAction>();
			this.backPtrs.add(bptr);
			this.generartingActions.add(ga);
		}
		
		public void addBPtr(MultiBPtrSearchNode btr, GroundedAction ga){
			this.backPtrs.add(btr);
			this.generartingActions.add(ga);
		}
		
		
		@Override
		public int hashCode(){
			return sh.hashCode();
		}
		
		@Override
		public boolean equals(Object o){
			if(!(o instanceof MultiBPtrSearchNode)){
				return false;
			}
			MultiBPtrSearchNode mo = (MultiBPtrSearchNode)o;
			return sh.equals(mo);
		}
		
		
	}
	
	
}
