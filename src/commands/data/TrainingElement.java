package commands.data;

public class TrainingElement {
	public String 			command;
	public Trajectory		trajectory;
	public String			identifier;
	
	public TrainingElement(String c, Trajectory t){
		this.command = c;
		this.trajectory = t;
	}
	
	public TrainingElement(String c, Trajectory t, String ident){
		this.command = c;
		this.trajectory = t;
		this.identifier = ident;
	}
}
