package commands.model3.weaklysupervisedinterface;

import logicalexpressions.LogicalExpression;
import sun.rmi.runtime.Log;

/**
 * @author James MacGlashan.
 */
public class WeaklySupervisedTrainingInstance implements java.io.Serializable{
	public LogicalExpression liftedTask;
	public LogicalExpression bindingConstraints;
	public String command;
	public double weight;

	public WeaklySupervisedTrainingInstance(LogicalExpression liftedTask, LogicalExpression bindingConstraints,
											String command, double weight){

		this.liftedTask = liftedTask;
		this.bindingConstraints = bindingConstraints;
		this.command = command;
		this.weight = weight;

	}
}
