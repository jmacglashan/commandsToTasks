package em;

import java.util.ArrayList;
import java.util.List;

import generativemodel.RVariableValue;

public class Dataset {

	protected List<List<RVariableValue>>		allData;
	
	
	public Dataset() {
		this.allData = new ArrayList<List<RVariableValue>>();
	}
	
	public void addDataInstance(List <RVariableValue> inst){
		this.allData.add(inst);
	}
	
	public List <RVariableValue> getDataInstance(int i){
		return this.allData.get(i);
	}
	
	public int size(){
		return this.allData.size();
	}

}
