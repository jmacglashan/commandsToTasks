package commands.auxiliarytools;


import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.imageio.ImageIO;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import burlap.oomdp.auxiliary.StateGenerator;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.visualizer.Visualizer;

import commands.data.TrainingElement;
import commands.data.TrainingElementParser;
import commands.data.Trajectory;

public class TrajectoryRecorder extends JFrame{

private static final long serialVersionUID = 1L;
	
	
	//Frontend GUI
	protected Visualizer							painter;
	protected TextArea								propViewer;
	
	protected JList									dataList;
	protected JScrollPane							dataScroller;
	
	protected JList									iterationList;
	protected JScrollPane							iterationScroller;
	
	protected Container								controlContainer;
	
	protected JDialog 								saveDialog;
	protected Container								saveContainer;
	protected JTextField							commandSaveTF;
	protected JTextField							pathSaveTF;
	
	protected JDialog								saveImageDialog;
	protected Container								saveImageContainer;
	protected JTextField							saveImageTF;
	
	protected int									cWidth;
	protected int									cHeight;
	
	
	//Backend
	protected TrainingElementParser					parser;
	
	protected List <String>							dataFiles;
	protected DefaultListModel						dataListModel;
	
	protected TrainingElement						trainEl;
	protected Trajectory							trajectory;
	protected DefaultListModel						trajectoryModel;
	protected boolean								selectedOnNewTrajectory;
	
	protected Domain								domain;
	protected StateGenerator						sg;
	
	
	private Map <String, String>					keyActionMap;

	
	
	
	protected String								directory;
	
	
	
	public TrajectoryRecorder(){
		keyActionMap = new HashMap<String, String>();
		selectedOnNewTrajectory = false;
	}
	
	public void addKeyAction(String key, String action){
		keyActionMap.put(key, action);
	}
	
	
	public void init(Visualizer v, Domain d, StateParser sp, StateGenerator sg, String dataDirectory){
		this.init(v, d, sp, sg, dataDirectory, 800, 800);
	}
	
	public void init(Visualizer v, Domain d, StateParser sp, StateGenerator sg, String dataDirectory, int w, int h){
		
		painter = v;
		parser = new TrainingElementParser(d, sp);
		domain = d;
		this.sg = sg;
		
		directory = dataDirectory;
		
		//get rid of trailing / and pull out the file paths
		if(directory.charAt(directory.length()-1) == '/'){
			directory = directory.substring(0, directory.length());
		}
		
		dataFiles = new ArrayList<String>();
		dataListModel = new DefaultListModel();
		
		this.parseDataFiles(directory);
		
		cWidth = w;
		cHeight = h;
		
		this.initGUI();
		
	}
	
	
	public void initGUI(){
		
		//set viewer components
		propViewer = new TextArea();
		propViewer.setEditable(false);
		painter.setPreferredSize(new Dimension(cWidth, cHeight));
		propViewer.setPreferredSize(new Dimension(cWidth, 100));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		getContentPane().add(painter, BorderLayout.CENTER);
		getContentPane().add(propViewer, BorderLayout.SOUTH);
		
		
		//set up key management
		addKeyListener(new KeyListener(){
			public void keyPressed(KeyEvent e) {
			}
			public void keyReleased(KeyEvent e) {	
			}
			public void keyTyped(KeyEvent e) {
				handleKeyPressed(e);
			}

		});
		
		//also add key listener to the painter in case the focus is changed
		painter.addKeyListener(new KeyListener(){
			public void keyPressed(KeyEvent e) {
			}
			public void keyReleased(KeyEvent e) {	
			}
			public void keyTyped(KeyEvent e) {
				handleKeyPressed(e);
			}

		});
		
		propViewer.addKeyListener(new KeyListener(){
			public void keyPressed(KeyEvent e) {
			}
			public void keyReleased(KeyEvent e) {	
			}
			public void keyTyped(KeyEvent e) {
				handleKeyPressed(e);
			}

		});
		
		
		
		
		
		//set episode component
		dataList = new JList(dataListModel);
		
		
		dataList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		dataList.setLayoutOrientation(JList.VERTICAL);
		dataList.setVisibleRowCount(-1);
		dataList.addListSelectionListener(new ListSelectionListener(){

			@Override
			public void valueChanged(ListSelectionEvent e) {
				handleTrajectorySelection(e);
			}
		});
		
		dataScroller = new JScrollPane(dataList);
		dataScroller.setPreferredSize(new Dimension(100, 600));
		
		
		
		//set iteration component
		trajectoryModel = new DefaultListModel();
		iterationList = new JList(trajectoryModel);
		
		iterationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		iterationList.setLayoutOrientation(JList.VERTICAL);
		iterationList.setVisibleRowCount(-1);
		iterationList.addListSelectionListener(new ListSelectionListener(){
			public void valueChanged(ListSelectionEvent e){
				handleIterationSelection(e);
			}
		});
		
		iterationScroller = new JScrollPane(iterationList);
		iterationScroller.setPreferredSize(new Dimension(150, 600));
		
		
		
		//add episode-iteration lists to window
		controlContainer = new Container();
		controlContainer.setLayout(new BorderLayout());
		
		
		controlContainer.add(dataScroller, BorderLayout.WEST);
		controlContainer.add(iterationScroller, BorderLayout.EAST);
		
		//add save button
		JButton saveButton = new JButton("Save");
		saveButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				handleSaveButton();
			}
		});
		
		//add reset new trajectory
		JButton resetButton = new JButton("Reset");
		resetButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				resetNewTrajectory();
			}
		});
		
		JButton saveImageButton = new JButton("Save Image");
		saveImageButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				handleSaveImage();
			}
		});
		
		//add button controls to window
		Container buttonContainer = new Container();
		buttonContainer.setLayout(new BorderLayout());
		buttonContainer.add(saveButton, BorderLayout.EAST);
		buttonContainer.add(resetButton, BorderLayout.WEST);
		buttonContainer.add(saveImageButton, BorderLayout.CENTER);
		
		controlContainer.add(buttonContainer, BorderLayout.SOUTH);
		
		getContentPane().add(controlContainer, BorderLayout.EAST);
		
		
		
		
		//handle the save container
		saveContainer = new Container();
		saveContainer.setLayout(new BorderLayout());
		Container TFContainer = new Container();
		TFContainer.setLayout(new BorderLayout());
		
		commandSaveTF = new JTextField(15);
		pathSaveTF = new JTextField(15);
		TFContainer.add(commandSaveTF, BorderLayout.NORTH);
		TFContainer.add(pathSaveTF, BorderLayout.SOUTH);
		saveContainer.add(TFContainer, BorderLayout.NORTH);
		
		JButton finalSave = new JButton("Save");
		finalSave.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				handleFinalSave();
			}
		});
		
		saveContainer.add(finalSave, BorderLayout.SOUTH);
		
		saveDialog = new JDialog(this, "Set Command and File Path", true);
		saveDialog.setPreferredSize(new Dimension(300, 150));
		saveDialog.setContentPane(saveContainer);
		
		
		
		saveImageContainer = new Container();
		saveImageContainer.setLayout(new BorderLayout());
		this.saveImageTF = new JTextField(15);
		saveImageContainer.add(this.saveImageTF, BorderLayout.NORTH);
		
		JButton finalSaveImage = new JButton("Save");
		finalSaveImage.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				handleFinalSaveImage();
			}
		});
		
		saveImageContainer.add(finalSaveImage, BorderLayout.SOUTH);
		
		saveImageDialog = new JDialog(this, "Set Image File Path", true);
		saveImageDialog.setPreferredSize(new Dimension(300, 75));
		saveImageDialog.setContentPane(saveImageContainer);
		
		
		//display the window
		pack();
		setVisible(true);
		
		
	}
	
	
	
	private void handleKeyPressed(KeyEvent e){
		
		if(!selectedOnNewTrajectory){
			return; //do not allow the user to change the state unless they are modifying the new trajectory space
		}
		
		
		String key = String.valueOf(e.getKeyChar());
		

		//otherwise this could be an action, see if there is an action mapping
		String mappedAction = keyActionMap.get(key);
		if(mappedAction != null){
			
			State curState = trajectory.getState(trajectory.numStates()-1);
			
			
			//then we have a action for this key
			//split the string up into components
			String [] comps = mappedAction.split(" ");
			String actionName = comps[0];
			
			//construct parameter list as all that remains
			String params[];
			if(comps.length > 1){
				params = new String[comps.length-1];
				for(int i = 1; i < comps.length; i++){
					params[i-1] = comps[i];
				}
			}
			else{
				params = new String[0];
			}
			
			Action action = domain.getAction(actionName);
			if(action == null){
				System.out.println("Unknown action: " + actionName);
			}
			else{
				State nextState = action.performAction(curState, params);
				GroundedAction ga = new GroundedAction(action, params);
				trajectory.addActionStateTransition(ga, nextState);
				
				//now update the list
				this.setIterationListData();
				
				//update the state
				painter.updateState(nextState);
				this.updatePropTextArea(nextState);
				
			}
			
		}
		
		
		
		
		
	}
	
	
	private void resetNewTrajectory(){
		if(!selectedOnNewTrajectory){
			return;
		}
		
		trainEl = null;
		State initState = sg.generateState();
		trajectory = new Trajectory(initState);
		
		painter.updateState(initState);
		this.setIterationListData();
		
	}
	
	
	
	private void handleSaveButton(){
		if(!selectedOnNewTrajectory){
			return;
		}
		if(trajectory.numStates() == 1){
			return; //don't save a trajectory with just an initial state
		}
		
		//reset text
		commandSaveTF.setText("Enter command of trajectory");
		pathSaveTF.setText("enter save path");
		
		//pop open the dialog
		saveDialog.pack();
		saveDialog.setVisible(true);
		
	}
	
	private void handleSaveImage(){
		saveImageDialog.pack();
		saveImageDialog.setVisible(true);
	}
	
	private void handleFinalSave(){
		
		//what is the save result?
		String command = commandSaveTF.getText();
		String fpath = pathSaveTF.getText();
		
		String path = directory + "/" + fpath;
		
		if(!path.endsWith(".txt")){
			path = path + ".txt";
		}
		
		TrainingElement te = new TrainingElement(command, trajectory);
		String teSRep = parser.getStringOfTrainingElement(te);
		
		//write this string out
		try{
			BufferedWriter out = new BufferedWriter(new FileWriter(path));
			out.write(teSRep);
			out.close();
		}catch(Exception E){
			System.out.println(E);
		}
		
		//update our files
		parseDataFiles(directory);
		
		//clear the dialog
		saveDialog.setVisible(false);
	}
	
	private void handleFinalSaveImage(){
		
		String fpath = this.saveImageTF.getText();
		

		String path = fpath;
		if(!path.startsWith("/")){
			path = directory + "/" + fpath;
		}
		
		if(!path.endsWith(".png")){
			path = path + ".png";
		}
		
		
		BufferedImage image = new BufferedImage(this.painter.getWidth(), this.painter.getHeight(), BufferedImage.TYPE_INT_ARGB);
		this.painter.paint(image.getGraphics());
		
		try {
			ImageIO.write(image, "png", new File(path));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		saveImageDialog.setVisible(false);
	}
	
	
	
	private void parseDataFiles(String directory){
		
		File dir = new File(directory);
		final String ext = ".txt";
		
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				if(name.endsWith(ext)){
					return true;
				}
				return false;
			}
		};
		String[] children = dir.list(filter);
		
		//dataFiles = new ArrayList<String>(children.length);
		//dataListModel = new DefaultListModel();
		dataFiles.clear();
		dataListModel.clear();
		
		
		for(int i = 0; i < children.length; i++){
			dataFiles.add(directory + "/" + children[i]);
			dataListModel.addElement(children[i].substring(0, children[i].indexOf(ext)));
			//System.out.println(files.get(i));
		}
		
		//add extra slot for making new ones
		dataListModel.addElement("new trajectory");
		
		
	}
	
	
	private void setIterationListData(){
		
		//clear the old contents
		trajectoryModel.clear();
		
		//add the initial state
		trajectoryModel.addElement("initial state");
		
		//add each action (which upon selecting would render the state resulting from that action)
		for(int i = 0; i < trajectory.numStates()-1; i++){
			trajectoryModel.addElement(trajectory.getAction(i).toString());
		}
		
	}
	
	private void handleTrajectorySelection(ListSelectionEvent e){
		
		if (e.getValueIsAdjusting() == false) {

			int ind = dataList.getSelectedIndex();
			//System.out.println("epsidoe id: " + ind);
       		if (ind != -1) {
       			
       			if(ind < dataFiles.size()){
					//System.out.println("Loading File...");
       				String fpath = dataFiles.get(ind);
       				String fcont = null;
       				try{
       					fcont = new Scanner(new File(fpath)).useDelimiter("\\Z").next();
       				}catch(Exception E){
       					System.out.println(E);
       				}
	       			trainEl = parser.getTrainingElementFromString(fcont);
	       			trajectory = trainEl.trajectory;
					//System.out.println("Finished Loading File.");
					
					painter.updateState(new State()); //clear screen
					this.setIterationListData();
					
					selectedOnNewTrajectory = false;
					
       			}
       			else if(ind == dataFiles.size()){
       				
       				//in this case the user has selected to create a new trajectory
       				trainEl = null;
       				State initState = sg.generateState();
       				trajectory = new Trajectory(initState);
       				
       				painter.updateState(initState);
       				this.setIterationListData();
       				
       				selectedOnNewTrajectory = true;
       				
       			}
				
			}
			else{
				//System.out.println("canceled selection");
			}
			
		}
		
	
	}
	
	
	
	private void handleIterationSelection(ListSelectionEvent e){
		
		if (e.getValueIsAdjusting() == false) {

       		if (iterationList.getSelectedIndex() != -1) {
				//System.out.println("Changing visualization...");
				int index = iterationList.getSelectedIndex();
				
				State curState = null;
				/*
				if(index == 0){
					curState = trajectory.getState(0);
				}
				*/
				if(index != -1){
					curState = trajectory.getState(index);
				}
				
				//draw it and update prop list
				//System.out.println(curState.getCompleteStateDescription()); //uncomment to print to terminal
				painter.updateState(curState);
				this.updatePropTextArea(curState);
				
				//System.out.println("Finished updating visualization.");
			}
			else{
				//System.out.println("canceled selection");
			}
			
		}
	
	}
	
	private void updatePropTextArea(State s){
		
		List <PropositionalFunction> props = domain.getPropFunctions();
		StringBuffer buf = new StringBuffer();
		for(PropositionalFunction pf : props){
			List <List <String>> bindings = s.getPossibleBindingsGivenParamOrderGroups(pf.getParameterClasses(), pf.getParameterOrderGroups());
			for(List <String> b : bindings){
				if(pf.isTrue(s, (String [])b.toArray(new String[b.size()]))){
					buf.append(this.getPropStringRep(pf, b));
					buf.append("\n");
				}
			}
		}
		
		propViewer.setText(buf.toString());
		
		
	}
	
	private String getPropStringRep(PropositionalFunction pf, List <String> bindings){
		
		StringBuffer buf = new StringBuffer(pf.getName());
		buf.append("(");
		boolean f = true;
		for(String s : bindings){
			if(!f){
				buf.append(", ");
			}
			buf.append(s);
			f = false;
		}
		buf.append(")");
		
		return buf.toString();
		
	}
	
	
}
