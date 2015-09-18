package commands.online;

import behavior.planning.PolicyGenerator;
import behavior.planning.sokoamdp.SokoAMDPPlannerPolicyGen;
import burlap.behavior.policy.DomainMappedPolicy;
import burlap.behavior.policy.Policy;
import burlap.behavior.singleagent.EpisodeAnalysis;


import burlap.oomdp.auxiliary.common.NullTermination;
import burlap.oomdp.core.*;
import burlap.oomdp.core.states.State;
import burlap.oomdp.singleagent.GroundedAction;

import burlap.oomdp.singleagent.common.NullRewardFunction;
import burlap.oomdp.singleagent.common.SimpleGroundedAction;
import burlap.oomdp.singleagent.environment.*;
import burlap.oomdp.statehashing.SimpleHashableStateFactory;
import burlap.oomdp.visualizer.Visualizer;
import commands.data.TrainingElement;
import commands.data.Trajectory;
import commands.model3.TaskModule;
import commands.model3.TrajectoryModule;
import commands.model3.mt.Tokenizer;
import commands.model3.weaklysupervisedinterface.MTWeaklySupervisedModel;
import commands.model3.weaklysupervisedinterface.WeaklySupervisedController;
import commands.model3.weaklysupervisedinterface.WeaklySupervisedTrainingInstance;
import domain.singleagent.sokoban2.Sokoban2Domain;
import domain.singleagent.sokoban2.Sokoban2Visualizer;
import experiments.sokoban.SokobanControllerConstructor;
import generativemodel.GMQueryResult;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author James MacGlashan.
 */
public class OnlineTraining extends JFrame{

	protected WeaklySupervisedController 				controller;
	protected List<TrainingElement> 					currentDataset = new ArrayList<TrainingElement>();
	protected List<WeaklySupervisedTrainingInstance>	weaklSupvervisedData = new ArrayList<WeaklySupervisedTrainingInstance>();
	protected int lastTESizeAtTrain = 0;

	protected Domain									planningDomain;
	protected Environment								realEnvironment;
	protected PolicyGenerator							policyGen;


	protected Visualizer v;

	protected JTextArea commandArea;
	protected JButton executeCommandButton;
	protected JButton stopExecution;
	protected JButton demonstrateButton;
	protected JButton swapEnvioronmentButton;
	protected JButton updateModelButton;
	protected JTextArea predicates;

	protected boolean isDemonstrating = false;
	protected Trajectory currentDemonstration;


	protected Thread liveStreamThread;
	protected Thread policyThread;
	protected volatile boolean humanTerminateSignal = false;

	protected volatile boolean runLiveStream = false;

	protected Map<String, GroundedAction> keyActionMap = new HashMap<String, GroundedAction>();

	protected SwapEnvironment swapEnvironment = null;

	public OnlineTraining(Domain planningDomain, Environment env, Visualizer v, PolicyGenerator policyGen, WeaklySupervisedController controller){

		this.planningDomain = planningDomain;
		this.realEnvironment = env;
		this.v = v;
		this.policyGen = policyGen;

		this.controller = controller;


	}


	public void initGui(){

		this.v.setPreferredSize(new Dimension(600, 600));
		this.getContentPane().add(this.v, BorderLayout.CENTER);

		Container guiControls = new Container();
		this.getContentPane().add(guiControls, BorderLayout.WEST);
		guiControls.setPreferredSize(new Dimension(400, 600));
		guiControls.setLayout(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets(10, 10, 10, 10);
		c.gridwidth = 2;

		this.commandArea = new JTextArea(5, 30);
		guiControls.add(this.commandArea, c);

		c.gridwidth=1;

		c.gridy = 1;
		this.executeCommandButton = new JButton("Execute Command");
		this.executeCommandButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				executeCommand();
			}
		});
		guiControls.add(this.executeCommandButton, c);

		c.gridx = 1;
		this.demonstrateButton = new JButton("Demonstrate Command");
		this.demonstrateButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				demonstrate();
			}
		});
		guiControls.add(this.demonstrateButton, c);


		c.gridx = 0;
		c.gridy = 2;
		this.stopExecution = new JButton("Stop Execution");
		this.stopExecution.setEnabled(false);
		this.stopExecution.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stopExecution.setEnabled(false);
				humanTerminateSignal = true;
				if(isDemonstrating){
					isDemonstrating = false;
					demonstrateButton.setEnabled(true);
					executeCommandButton.setEnabled(true);
					TrainingElement te = new TrainingElement(commandArea.getText().trim(), currentDemonstration);
					currentDataset.add(te);
					System.out.println("added: " + te.command + "; demo size: " + (currentDemonstration.numStates()-1));
					System.out.println("Dataset size is now " + currentDataset.size());
				}
			}
		});
		guiControls.add(this.stopExecution, c);

		c.gridx = 1;
		this.swapEnvioronmentButton = new JButton("Change Environment");
		if(this.swapEnvironment != null){
			this.swapEnvioronmentButton.setEnabled(true);
		}
		else{
			this.swapEnvioronmentButton.setEnabled(false);
		}
		this.swapEnvioronmentButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				swapEnvironment.swapEnvironment(realEnvironment);
			}
		});
		guiControls.add(this.swapEnvioronmentButton, c);
		c.gridx = 0;


		c.gridx = 0;
		c.gridy = 3;
		this.updateModelButton = new JButton("Update Model");
		this.updateModelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateModel();
			}
		});
		guiControls.add(this.updateModelButton, c);


		this.predicates = new JTextArea(10, 30);
		this.predicates.setEditable(false);
		c.gridy = 4;
		c.insets = new Insets(30, 10, 10, 10);
		c.gridwidth=2;

		guiControls.add(this.predicates, c);


		this.v.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
				if(isDemonstrating) {
					String strKey = String.valueOf(e.getKeyChar());
					GroundedAction ga = keyActionMap.get(strKey);

					if(ga != null) {
						EnvironmentOutcome eo = ga.executeIn(realEnvironment);
						currentDemonstration.addActionStateTransition(ga, eo.op);
					}
				}
			}

			@Override
			public void keyPressed(KeyEvent e) {

			}

			@Override
			public void keyReleased(KeyEvent e) {

			}
		});


	}


	public void showGUI(){
		this.beginLiveStream();
		this.pack();
		this.setVisible(true);
	}

	public void setSwapEnvironment(SwapEnvironment swapEnv){
		this.swapEnvironment = swapEnv;
		if(this.swapEnvioronmentButton != null){
			this.swapEnvioronmentButton.setEnabled(true);
		}
	}

	private void beginLiveStream() {

		if(!this.runLiveStream) {

			this.runLiveStream = true;

			if (this.liveStreamThread == null) {
				this.liveStreamThread = new Thread(new Runnable() {
					@Override
					public void run() {
						while (runLiveStream) {
							updateState(realEnvironment.getCurrentObservation());
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
				});

			}

			this.liveStreamThread.start();

		}

	}

	public void addKeyAction(String key, GroundedAction action){
		this.keyActionMap.put(key, action);
	}

	public void demonstrate(){
		this.currentDemonstration = new Trajectory(this.realEnvironment.getCurrentObservation());
		this.isDemonstrating = true;
		this.stopExecution.setEnabled(true);
		this.demonstrateButton.setEnabled(false);
		this.executeCommandButton.setEnabled(false);
	}



	public void executeCommand(){

		String command = this.commandArea.getText().trim();
		this.executeCommandButton.setEnabled(false);
		this.humanTerminateSignal = false;

		System.out.println("Receiving command: " + command);

		List<GMQueryResult> predictions = this.controller.getRFDistribution(this.realEnvironment.getCurrentObservation(), command);
		final TaskModule.RFConVariableValue val = (TaskModule.RFConVariableValue)GMQueryResult.maxProb(predictions).getSingleQueryVar();

		final TrajectoryModule.ConjunctiveGroundedPropTF tf = new TrajectoryModule.ConjunctiveGroundedPropTF(val.rf);
		System.out.println("Selecting RF: " + val.toString());

		final Policy plannerPolicy = this.policyGen.getPolicy(this.planningDomain, this.realEnvironment.getCurrentObservation(), val.rf, tf,
				new SimpleHashableStateFactory());


		if(this.realEnvironment instanceof TaskSettableEnvironment){
			((TaskSettableEnvironment)this.realEnvironment).setRf(val.rf);
			((TaskSettableEnvironment)this.realEnvironment).setTf(new TermWithHumanIntercept(tf));
		}

		this.policyThread = new Thread(new Runnable() {
			@Override
			public void run() {
				stopExecution.setEnabled(true);
//				EpisodeAnalysis ea = envPolicy.evaluateBehavior(realEnvironment.getCurState(), val.rf,
//						new TermWithHumanIntercept(tf));
				EpisodeAnalysis ea = plannerPolicy.evaluateBehavior(realEnvironment);
				System.out.println("Executed episode size: " + ea.maxTimeStep());
				executeCommandButton.setEnabled(true);
				humanTerminateSignal = false;
				stopExecution.setEnabled(false);

				System.out.println("Policy terminated.");
			}
		});

		this.policyThread.start();


	}

	public void updateState(State s){
		this.v.updateState(s);

		List<GroundedProp> ps = PropositionalFunction.getAllGroundedPropsFromPFList(this.planningDomain.getPropFunctions(), s);
		StringBuilder buf = new StringBuilder();
		for(GroundedProp gp : ps){
			if(gp.isTrue(s)){
				buf.append(gp.toString()).append("\n");
			}
		}
		this.predicates.setText(buf.toString());

	}

	public void updateModel(){

		System.out.println("Got update model request");

		if(this.currentDataset.size() <= this.lastTESizeAtTrain){
			return ;
		}



		List<TrainingElement> newData = new ArrayList<TrainingElement>(this.currentDataset.size() - this.lastTESizeAtTrain);
		for(int i = this.lastTESizeAtTrain; i < this.currentDataset.size(); i++){
			newData.add(this.currentDataset.get(i));
		}

		System.out.println("Performing IRL on new trajectories");
		//now get the weakly supervised data for and add it to our weakly supervised training set
		for(TrainingElement te : newData){
			List<WeaklySupervisedTrainingInstance> instances = this.controller.getWeaklySupervisedTrainingInstancesForTrajectory(te.trajectory, te.command);
			for(WeaklySupervisedTrainingInstance wte : instances){
				this.weaklSupvervisedData.add(wte);
			}
		}

		System.out.println("Updating language model");
		this.controller.setWeaklySupervisedTrainingDataset(this.weaklSupvervisedData);
		this.controller.trainLanguageModel();

		this.lastTESizeAtTrain = this.currentDataset.size();

		System.out.println("Finished upadating language model");


	}



	public class TermWithHumanIntercept implements TerminalFunction {

		TerminalFunction sourceTF;

		public TermWithHumanIntercept(TerminalFunction sourceTF){
			this.sourceTF = sourceTF;
		}


		@Override
		public boolean isTerminal(State s) {

			boolean rval = humanTerminateSignal || this.sourceTF.isTerminal(s);
			if(rval == true){
				//System.out.println("Terminal state hit.");
			}
			return rval;
		}
	}






	public static class DelayedEnvironment extends SimulatedEnvironment{

		protected Domain domain;
		protected long delay = 300;

		public DelayedEnvironment(Domain domain){
			super(domain, new NullRewardFunction(), new NullTermination());
		}

		@Override
		public EnvironmentOutcome executeAction(GroundedAction ga) {
			//TODO: does sleep need to come before state change is made?
			EnvironmentOutcome eo =  super.executeAction(ga);
			try {
				Thread.sleep(this.delay);
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
			return eo;
		}

//		@Override
//		public State executeAction(String aname, String[] params) {
//			burlap.oomdp.singleagent.Action action = this.domain.getAction(aname);
//			State nextState = action.performAction(this.curState, params);
//			try {
//				Thread.sleep(this.delay);
//			} catch(InterruptedException e) {
//				e.printStackTrace();
//			}
//			this.curState = nextState;
//			return curState;
//		}


	}

	public static interface SwapEnvironment{
		void swapEnvironment(Environment env);
	}

	public static class DemonstrateStates implements SwapEnvironment{

		List<State> cycleStates = new ArrayList<State>();
		int curIndex = 0;

		public DemonstrateStates(Domain domain){

			State s1 = Sokoban2Domain.getCleanState(domain, 3, 2, 2);

			Sokoban2Domain.setRoom(s1, 0, 4, 0, 0, 8, "red");
			Sokoban2Domain.setRoom(s1, 1, 8, 0, 4, 4, "green");
			Sokoban2Domain.setRoom(s1, 2, 8, 4, 4, 8, "blue");

			Sokoban2Domain.setDoor(s1, 0, 4, 6, 4, 6);
			Sokoban2Domain.setDoor(s1, 1, 4, 2, 4, 2);

			Sokoban2Domain.setAgent(s1, 6, 6);
			Sokoban2Domain.setBlock(s1, 0, 2, 2, "chair", "yellow");
			Sokoban2Domain.setBlock(s1, 1, 4, 2, "bag", "magenta");

			this.cycleStates.add(s1);

			State s2 = Sokoban2Domain.getCleanState(domain, 4, 3, 2);
			Sokoban2Domain.setRoom(s2, 0, 4, 0, 0, 4, "green");
			Sokoban2Domain.setRoom(s2, 1, 8, 0, 4, 4, "magenta");
			Sokoban2Domain.setRoom(s2, 2, 12, 0, 8, 4, "blue");
			Sokoban2Domain.setRoom(s2, 3, 12, 4, 8, 10, "red");

			Sokoban2Domain.setDoor(s2, 0, 4, 2, 4, 2);
			Sokoban2Domain.setDoor(s2, 1, 8, 2, 8, 2);
			Sokoban2Domain.setDoor(s2, 2, 10, 4, 10, 4);

			Sokoban2Domain.setBlock(s2, 0, 7, 11, "chair", "yellow");
			Sokoban2Domain.setBlock(s2, 1, 5, 10, "bag", "magenta");

			Sokoban2Domain.setAgent(s2, 9, 10);

			//cycleStates.add(s2);

			State s3 = Sokoban2Domain.getCleanState(domain, 3, 2, 2);
			Sokoban2Domain.setRoom(s3, 0, 6, 0, 0, 6, "green");
			Sokoban2Domain.setRoom(s3, 1, 12, 0, 6, 6, "blue");
			Sokoban2Domain.setRoom(s3, 2, 12, 6, 6, 12, "red");

			Sokoban2Domain.setDoor(s3, 0, 6, 2, 6, 2);
			Sokoban2Domain.setDoor(s3, 1, 10, 6, 10, 6);

			Sokoban2Domain.setBlock(s3, 0, 8, 8, "chair", "yellow");
			Sokoban2Domain.setBlock(s3, 1, 11, 11, "bag", "magenta");

			Sokoban2Domain.setAgent(s3, 9, 10);


			this.cycleStates.add(s3);


		}

		@Override
		public void swapEnvironment(Environment env) {

			if(env instanceof StateSettableEnvironment){
				((StateSettableEnvironment) env).setCurStateTo(this.cycleStates.get(this.curIndex));
				this.curIndex = (this.curIndex+1) % this.cycleStates.size();
			}
		}
	}

	public static void main(String[] args) {

		Sokoban2Domain sokoDomain = new Sokoban2Domain();
		sokoDomain.includePullAction(true);
		sokoDomain.includeDirectionAttribute(true);
		Domain domain = sokoDomain.generateDomain();

		Environment env = new DelayedEnvironment(domain);
		Visualizer v = Sokoban2Visualizer.getVisualizer("resources/robotImages");

		PolicyGenerator policyGen = new SokoAMDPPlannerPolicyGen();

		final State initialState = Sokoban2Domain.getClassicState(domain);
		((DelayedEnvironment)env).setCurStateTo(initialState);
		v.updateState(initialState);

		SokobanControllerConstructor constructor = new SokobanControllerConstructor(false, false);
		WeaklySupervisedController controller = constructor.generateNewController();
		Tokenizer tokenizer = new Tokenizer(true, true);
		tokenizer.addDelimiter("-");
		MTWeaklySupervisedModel model = new MTWeaklySupervisedModel(controller, tokenizer, 10);
		controller.setLanguageModel(model);


		OnlineTraining ot = new OnlineTraining(domain, env, v, policyGen, controller);
		ot.addKeyAction("w", new SimpleGroundedAction(domain.getAction(Sokoban2Domain.ACTIONNORTH)));
		ot.addKeyAction("s", new SimpleGroundedAction(domain.getAction(Sokoban2Domain.ACTIONSOUTH)));
		ot.addKeyAction("a", new SimpleGroundedAction(domain.getAction(Sokoban2Domain.ACTIONWEST)));
		ot.addKeyAction("d", new SimpleGroundedAction(domain.getAction(Sokoban2Domain.ACTIONEAST)));
		ot.addKeyAction("e", new SimpleGroundedAction(domain.getAction(Sokoban2Domain.ACTIONPULL)));

//		ot.setSwapEnvironment(new SwapEnvironment() {
//			@Override
//			public void swapEnvironment(Environment env) {
//				env.setCurStateTo(initialState);
//			}
//		});

		ot.setSwapEnvironment(new DemonstrateStates(domain));

		ot.initGui();
		ot.showGUI();



	}

}
