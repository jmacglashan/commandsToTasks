package em;

import java.util.List;

import generativemodel.GenerativeModel;
import generativemodel.RVariableValue;

public abstract class EMModule {

	
	protected GenerativeModel gm;
	
	
	abstract public void runEStep(int dataInstanceId, List<RVariableValue> observables);
	abstract public void runMStep();
	
	
	public void setGenerativeModelSrc(GenerativeModel gm){
		this.gm = gm;
	}

}
