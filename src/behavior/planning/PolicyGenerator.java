package behavior.planning;


import burlap.behavior.policy.Policy;
import burlap.oomdp.core.Domain;

import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.core.states.State;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.statehashing.HashableStateFactory;

/**
 * @author James MacGlashan.
 */
public interface PolicyGenerator {
	public Policy getPolicy(Domain domain, State initialState, RewardFunction rf, TerminalFunction tf, HashableStateFactory hashingFactory);
}
