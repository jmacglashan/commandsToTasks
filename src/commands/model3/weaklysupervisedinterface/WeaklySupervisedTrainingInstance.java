package commands.model3.weaklysupervisedinterface;

import logicalexpressions.LogicalExpression;

/**
 * @author James MacGlashan.
 */
public class WeaklySupervisedTrainingInstance {
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
