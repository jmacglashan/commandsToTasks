package commands.data;

import burlap.oomdp.core.State;

public class TestElement {
	public String testIdenitifier;
	public String command;
	public State initialState;
	
	public TestElement(String testIdenitifier, State s, String command){
		this.testIdenitifier = testIdenitifier;
		this.initialState = s;
		this.command = command;
	}
	
	
}
