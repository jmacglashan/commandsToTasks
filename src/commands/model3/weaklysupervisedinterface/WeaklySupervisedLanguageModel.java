package commands.model3.weaklysupervisedinterface;

import logicalexpressions.LogicalExpression;

import java.util.List;

/**
 * An interface for language model that can be trained with weakly supervised training data and return the probability
 * of a command given a lifted task and set of object binding constraints.
 * @author James MacGlashan.
 */
public interface WeaklySupervisedLanguageModel {

	/**
	 * Returns the probability of an input command be generated: Pr(command | liftedTask, bindingConstraints)
	 * @param liftedTask a logical expression representing the lifted task
	 * @param bindingConstraints a logical expression representing binding constraints
	 * @param command the query command
	 * @return the probability of the command being generated.
	 */
	public double probabilityOfCommand(LogicalExpression liftedTask, LogicalExpression bindingConstraints, String command);


	/**
	 * Trains this language model's parameters to fit the weakly supervised training instances.
	 * Each training instance consists of a lifted task, binding constraint, command, and a weight (probability) of
	 * that task being the true task for the associated command.
	 * @param dataset the weakly supervised training dataset.
	 */
	public void learnFromDataset(List<WeaklySupervisedTrainingInstance> dataset);

}
