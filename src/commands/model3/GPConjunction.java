package commands.model3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import burlap.oomdp.core.GroundedProp;
import burlap.oomdp.core.State;

public class GPConjunction implements Iterable<GroundedProp>{
	public List<GroundedProp> gps;
	
	public GPConjunction(){
		this.gps = new ArrayList<GroundedProp>();
	}
	
	public void addGP(GroundedProp gp){
		this.gps.add(gp);
	}
	
	public boolean statisifiedIn(State s){
		for(GroundedProp gp : this.gps){
			if(!gp.isTrue(s)){
				return false;
			}
		}
		return true;
	}
	
	@Override
	public String toString(){
		StringBuffer buf = new StringBuffer();
		buf.append(gps.toString());
		for(int i = 1; i < gps.size(); i++){
			buf.append(" ^ ").append(gps.get(i).toString());
		}
		return buf.toString();
	}

	@Override
	public Iterator<GroundedProp> iterator() {
		return this.gps.iterator();
	}
}
