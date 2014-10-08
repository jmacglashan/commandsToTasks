package domain.singleagent.sokoban;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import burlap.oomdp.auxiliary.DomainGenerator;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.*;
import burlap.oomdp.core.Attribute.AttributeType;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.explorer.TerminalExplorer;
import burlap.oomdp.singleagent.explorer.VisualExplorer;
import burlap.oomdp.visualizer.Visualizer;


import commands.data.TrainingElement;
import commands.data.Trajectory;

public class SokobanDomain implements DomainGenerator, StateParser {

	public static final String			XATTNAME = "xAtt";
	public static final String			YATTNAME = "yAtt";
	public static final String			TOPYATTNAME = "topYAtt";	
	public static final String			BOTTOMXATTNAME = "bottomXAtt";
	public static final String			TOPXATTNAME = "topXAtt";
	public static final String			BOTTOMYATTNAME = "bottomYAtt";
	public static final String			COLORATTNAME = "colorAtt";
	public static final String			SHAPEATTNAME = "shapeAtt";
	public static final String			AGENTCLASS = "agent";
	public static final String			ROOMCLASS = "room";
	public static final String			BLOCKCLASS = "block";
	public static final String			DOORCLASS = "door";
	public static final String			ACTIONNORTH = "north";
	public static final String			ACTIONSOUTH = "south";
	public static final String			ACTIONEAST = "east";
	public static final String			ACTIONWEST = "west";
	public static final String			PFAGENTINROOM = "agentInRoom";
	public static final String			PFBLOCKINROOM = "blockInRoom";
	public static final String			PFISBLACK = "isBlack";
	public static final String			PFISBLUE = "isBlue";
	public static final String			PFISCYAN = "isCyan";
	public static final String			PFISDARKGRAY = "isDarkGray";
	public static final String			PFISGRAY = "isGray";
	public static final String			PFISGREEN = "isGreen";
	public static final String			PFISLIGHTGRAY = "isLightGray";
	public static final String			PFISMAGENTA = "isMagenta";
	public static final String			PFISORANGE = "isOrange";
	public static final String			PFISPINK = "isPink";
	public static final String			PFISRED = "isRed";
	public static final String			PFISWHITE = "isWhite";
	public static final String			PFISYELLOW = "isYellow";	
	public static final String			PFISSTAR = "isStar";	
	public static final String			PFISMOON = "isMoon";	
	public static final String			PFISCIRCLE = "isCircle";	
	public static final String			PFISSMILEY = "isSmiley";	
	public static final String			PFISSQUARE = "isSquare";	
	public static final String			PFBLOCKNEXTTO = "blockNextTo";
	public static final String			PFAGENTNEXTTO = "agentNextTo";
	public static final String			PFROOMISBLACK = "roomIsBlack";
	public static final String			PFROOMISBLUE = "roomIsBlue";
	public static final String			PFROOMISCYAN = "roomIsCyan";
	public static final String			PFROOMISDARKGRAY = "roomIsDarkGray";
	public static final String			PFROOMISGRAY = "roomIsGray";
	public static final String			PFROOMISGREEN = "roomIsGreen";
	public static final String			PFROOMISLIGHTGRAY = "roomIsLightGray";
	public static final String			PFROOMISMAGENTA = "roomIsMagenta";
	public static final String			PFROOMISORANGE = "roomIsOrange";
	public static final String			PFROOMISPINK = "roomIsPink";
	public static final String			PFROOMISRED = "roomIsRed";
	public static final String			PFROOMISWHITE = "roomIsWhite";
	public static final String			PFROOMISYELLOW = "roomIsYellow";


	public static final String			PFRCOLORCLASS = "color";
	public static final String			PFBCOLORCLASS = "BColor";
	public static final String			PFSHAPECLASS = "shape";
	public static final String			PFPOSCLASS = "position";
	public static final String			PFBPOSCLASS = "BPosition";

	public static int				MINX = 0;
	public static int				MAXX = 24;
	public static int				MINY = 0;
	public static int				MAXY = 24;
	public static int [][]		MAP = new int[MAXX+1][MAXY+1];
	public static final ArrayList<String> colors = new ArrayList<String>(Arrays.asList(new String[]{"black", "blue",
			"cyan", "darkGray", "gray", "green", "lightGray", "magenta", "orange", "pink", "red", "white", "yellow"}));

	public static final ArrayList<String> shapes = new ArrayList<String>(Arrays.asList(new String[]{"star", "moon",
			"circle", "smiley", "square"}));

	private static SADomain 			SOKOBANDOMAIN = null;

	public static void main(String args[]){

		//command line arguments
		int vLocation = -1;
		int fLocation = -1;
		int tLocation = -1;
		int cLocation = -1;
		int dLocation = -1;
		int wLocation = -1;
		int sLocation = -1;


		if (args.length == 0 || args[0].equals("-help")){
			System.out.println("Usage: SokobanDomain [-c commandsfile] [-t] [-v] [-f statefile]");
			System.out.println("-c runs a set of commands from a text file");
			System.out.println("-d runs a set of commands from a directory of text files");
			System.out.println("-t runs the terminal domain explorer");
			System.out.println("-v runs the visual domain explorer");
			System.out.println("-f will load a starting state from a text file rather than using the default one");
			System.out.println("-w will prevent the creation of walls (warning: this may have unintented consequences)");
			System.exit(0);
		}

		//check for all flags
		for (int i = 0; i < args.length; i++){
			if (args[i].equals("-v") && vLocation == -1){
				vLocation = i;
			}
			else if (args[i].equals("-v")) {
				System.out.println("Error: duplicate flag \"-v\".");
				System.exit(1);
			}
			if (args[i].equals("-w") && wLocation == -1){
				wLocation = i;
			}
			else if (args[i].equals("-w")) {
				System.out.println("Error: duplicate flag \"-w\".");
				System.exit(1);
			}
			if (args[i].equals("-t") && tLocation == -1){	
				tLocation = i; 
			}
			else if (args[i].equals("-t") && tLocation != -1) {
				System.out.println("Error: duplicate flag \"-t\".");
				System.exit(1);
			}
			if (args[i].equals("-f") && fLocation == -1){
				fLocation = i;
			}
			else if (args[i].equals("-f")) {
				System.out.println("Error: duplicate flag \"-f\".");
				System.exit(1);
			}
			if (args[i].equals("-c") && cLocation == -1){
				cLocation = i;
			}
			else if (args[i].equals("-c")) {
				System.out.println("Error: duplicate flag \"-c\".");
				System.exit(1);
			}
			if (args[i].equals("-d") && dLocation == -1){
				dLocation = i;
			}
			else if (args[i].equals("-d")) {
				System.out.println("Error: duplicate flag \"-d\".");
				System.exit(1);
			}
			if (args[i].equals("-s") && sLocation == -1){	
				sLocation = i; 
			}
			else if (args[i].equals("-s") && sLocation != -1) {
				System.out.println("Error: duplicate flag \"-s\".");
				System.exit(1);
			}

		}

		if (sLocation != -1){
			MAXX = Integer.parseInt(args[sLocation + 1]);
			MAXY = Integer.parseInt(args[sLocation + 2]);
		}
		else {
			MAXX = 24;
			MAXY = 24;
		}

		MAP = new int[MAXX+1][MAXY+1];
		SokobanDomain constructor = new SokobanDomain();
		Domain domain = constructor.generateDomain();

		State st = constructor.getCleanState();


		if (fLocation != -1){
			BufferedReader in = null;
			String newState = null;
			try {
				in = new BufferedReader(new FileReader(args[fLocation + 1]));
			} catch (Exception e) {
				System.out.println("Error: File \"" + args[fLocation + 1] + "\" not found.");
				System.exit(1);
			}
			try {
				newState = in.readLine();
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			st = constructor.stringToState(newState);
		}
		else{
			//default room setup
			ObjectInstance room = st.getObject(ROOMCLASS + 0);
			room.setValue(TOPXATTNAME, 0);
			room.setValue(TOPYATTNAME, 8);
			room.setValue(BOTTOMXATTNAME, 4);
			room.setValue(BOTTOMYATTNAME, 4);
			room.setValue(COLORATTNAME, "red");

			ObjectInstance room2 = st.getObject(ROOMCLASS + 1);
			room2.setValue(TOPXATTNAME, 4);
			room2.setValue(TOPYATTNAME, 8);
			room2.setValue(BOTTOMXATTNAME, 8);
			room2.setValue(BOTTOMYATTNAME, 4);
			room2.setValue(COLORATTNAME, "green");

			ObjectInstance room3 = st.getObject(ROOMCLASS + 2);
			room3.setValue(TOPXATTNAME, 0);
			room3.setValue(TOPYATTNAME, 4);
			room3.setValue(BOTTOMXATTNAME, 8);
			room3.setValue(BOTTOMYATTNAME, 0);
			room3.setValue(COLORATTNAME, "blue");

			ObjectInstance door = st.getObject(DOORCLASS + 0);
			door.setValue(TOPXATTNAME, 2);
			door.setValue(TOPYATTNAME, 4);
			door.setValue(BOTTOMXATTNAME, 2);
			door.setValue(BOTTOMYATTNAME, 3);

			ObjectInstance door2 = st.getObject(DOORCLASS + 1);
			door2.setValue(TOPXATTNAME, 6);
			door2.setValue(TOPYATTNAME, 4);
			door2.setValue(BOTTOMXATTNAME, 6);
			door2.setValue(BOTTOMYATTNAME, 3);

			if (wLocation == -1){
				constructor.createMap(st);
			}

			ObjectInstance agent = st.getObject(AGENTCLASS + 0);
			agent.setValue(XATTNAME, 1);
			agent.setValue(YATTNAME, 5);


			ObjectInstance block = st.getObject(BLOCKCLASS + 0);
			block.setValue(XATTNAME, 2);
			block.setValue(YATTNAME, 2);
			block.setValue(COLORATTNAME, "green");
			block.setValue(SHAPEATTNAME, "star");


			ObjectInstance block2 = st.getObject(BLOCKCLASS + 1);
			block2.setValue(XATTNAME, 6);
			block2.setValue(YATTNAME, 6);
			block2.setValue(COLORATTNAME, "red");
			block2.setValue(SHAPEATTNAME, "star");

		}

		//assign keys for command line interface
		if(tLocation != -1){
			TerminalExplorer exp = new TerminalExplorer(domain);
			exp.addActionShortHand("n", ACTIONNORTH);
			exp.addActionShortHand("s", ACTIONSOUTH);
			exp.addActionShortHand("e", ACTIONEAST);
			exp.addActionShortHand("w", ACTIONWEST);
			exp.exploreFromState(st);	
		}	
		//read trajectory from file
		else if(cLocation != -1){
			runTrajectory(constructor, domain, st, args[cLocation + 1]);
		}

		

		//visual explorer
		if(vLocation != -1){
			System.out.println(vLocation);
			Visualizer vis = SokobanVisualizer.getVisualizer();
			VisualExplorer exp = new VisualExplorer(domain, vis, st);

			//use w-s-a-d for north south west east
			exp.addKeyAction("w", ACTIONNORTH);
			exp.addKeyAction("s", ACTIONSOUTH);
			exp.addKeyAction("a", ACTIONWEST);
			exp.addKeyAction("d", ACTIONEAST);
			exp.initGUI();
		}		
	}

	/**
	 * Creates the domain by assigning ranges to all discrete attributes, associating them with object types,
	 * and associating all actions with the domain
	 * @return the initialized domain
	 */
	public Domain generateDomain() {

		if(SOKOBANDOMAIN != null){
			return SOKOBANDOMAIN;
		}

		//otherwise construct it!
		SOKOBANDOMAIN = new SADomain();

		Attribute xAtt = new Attribute(SOKOBANDOMAIN, XATTNAME, AttributeType.DISC);
		xAtt.setDiscValuesForRange(MINX, MAXX, 1);

		Attribute topXAtt = new Attribute(SOKOBANDOMAIN, TOPXATTNAME, AttributeType.DISC);
		topXAtt.setDiscValuesForRange(MINX, MAXX, 1);

		Attribute bottomXAtt = new Attribute(SOKOBANDOMAIN, BOTTOMXATTNAME, AttributeType.DISC);
		bottomXAtt.setDiscValuesForRange(MINX, MAXX, 1);

		Attribute yAtt = new Attribute(SOKOBANDOMAIN, YATTNAME, AttributeType.DISC);
		yAtt.setDiscValuesForRange(MINY, MAXY, 1);

		Attribute topYAtt = new Attribute(SOKOBANDOMAIN, TOPYATTNAME, AttributeType.DISC);
		topYAtt.setDiscValuesForRange(MINY, MAXY, 1);

		Attribute bottomYAtt = new Attribute(SOKOBANDOMAIN, BOTTOMYATTNAME, AttributeType.DISC);
		bottomYAtt.setDiscValuesForRange(MINY, MAXY, 1);

		Attribute colorAtt = new Attribute(SOKOBANDOMAIN, COLORATTNAME, AttributeType.DISC);
		colorAtt.setDiscValues(colors);

		Attribute shapeAtt = new Attribute(SOKOBANDOMAIN, SHAPEATTNAME, AttributeType.DISC);
		shapeAtt.setDiscValues(shapes);

		ObjectClass aClass = new ObjectClass(SOKOBANDOMAIN, AGENTCLASS);
		aClass.addAttribute(xAtt);
		aClass.addAttribute(yAtt);
		//aClass.addAttribute(shapeAtt);

		ObjectClass bClass = new ObjectClass(SOKOBANDOMAIN, BLOCKCLASS);
		bClass.addAttribute(xAtt);
		bClass.addAttribute(yAtt);
		bClass.addAttribute(colorAtt);
		bClass.addAttribute(shapeAtt);

		ObjectClass rClass = new ObjectClass(SOKOBANDOMAIN, ROOMCLASS);
		rClass.addAttribute(topXAtt);
		rClass.addAttribute(topYAtt);
		rClass.addAttribute(bottomXAtt);
		rClass.addAttribute(bottomYAtt);
		rClass.addAttribute(bottomYAtt);
		rClass.addAttribute(colorAtt);

		ObjectClass dClass = new ObjectClass(SOKOBANDOMAIN, DOORCLASS);
		dClass.addAttribute(topXAtt);
		dClass.addAttribute(topYAtt);
		dClass.addAttribute(bottomXAtt);
		dClass.addAttribute(bottomYAtt);
		dClass.addAttribute(bottomYAtt);

		PropositionalFunction isCircle = new IsCirclePF(PFISCIRCLE, SOKOBANDOMAIN, new String[]{BLOCKCLASS});
		PropositionalFunction isStar = new IsStarPF(PFISSTAR, SOKOBANDOMAIN, new String[]{BLOCKCLASS});
		//PropositionalFunction isMoon = new IsMoonPF(PFISMOON, SOKOBANDOMAIN, new String[]{BLOCKCLASS});
		//PropositionalFunction isSmiley = new IsSmileyPF(PFISSMILEY, SOKOBANDOMAIN, new String[]{BLOCKCLASS});
		//PropositionalFunction isSquare = new IsSquarePF(PFISSQUARE, SOKOBANDOMAIN, new String[]{BLOCKCLASS});
		PropositionalFunction inRoom = new InRoomPF(PFAGENTINROOM, SOKOBANDOMAIN, new String[]{AGENTCLASS, ROOMCLASS});
		PropositionalFunction inRoom2 = new InRoomPF(PFBLOCKINROOM, SOKOBANDOMAIN, new String[]{BLOCKCLASS, ROOMCLASS});
		//PropositionalFunction isBlack = new IsBlackPF(PFISBLACK, SOKOBANDOMAIN, new String[]{BLOCKCLASS});
		PropositionalFunction isBlue = new IsBluePF(PFISBLUE, SOKOBANDOMAIN, new String[]{BLOCKCLASS});
		//PropositionalFunction isCyan = new IsCyanPF(PFISCYAN, SOKOBANDOMAIN, new String[]{BLOCKCLASS});
		//PropositionalFunction isDarkGray = new IsDarkGrayPF(PFISDARKGRAY, SOKOBANDOMAIN, new String[]{BLOCKCLASS});
		//PropositionalFunction isGray = new IsGrayPF(PFISGRAY, SOKOBANDOMAIN, new String[]{BLOCKCLASS});
		//PropositionalFunction isGreen = new IsGreenPF(PFISGREEN, SOKOBANDOMAIN, new String[]{BLOCKCLASS});
		//PropositionalFunction isLightGray = new IsLightGrayPF(PFISLIGHTGRAY, SOKOBANDOMAIN, new String[]{BLOCKCLASS});
		//PropositionalFunction isMagenta = new IsMagentaPF(PFISMAGENTA, SOKOBANDOMAIN, new String[]{BLOCKCLASS});
		//PropositionalFunction isOrange = new IsOrangePF(PFISORANGE, SOKOBANDOMAIN, new String[]{BLOCKCLASS});
		//PropositionalFunction isPink = new IsPinkPF(PFISPINK, SOKOBANDOMAIN, new String[]{BLOCKCLASS});
		PropositionalFunction isRed = new IsRedPF(PFISRED, SOKOBANDOMAIN, new String[]{BLOCKCLASS});
		//PropositionalFunction isWhite = new IsWhitePF(PFISWHITE, SOKOBANDOMAIN, new String[]{BLOCKCLASS});
		PropositionalFunction isYellow = new IsYellowPF(PFISYELLOW, SOKOBANDOMAIN, new String[]{BLOCKCLASS});
		//PropositionalFunction nextTo = new NextToPF(PFBLOCKNEXTTO, SOKOBANDOMAIN, new String[]{BLOCKCLASS, ROOMCLASS});
		//PropositionalFunction nextTo2 = new NextToPF(PFAGENTNEXTTO, SOKOBANDOMAIN, new String[]{AGENTCLASS, ROOMCLASS});
		//PropositionalFunction roomIsBlack = new IsBlackPF(PFROOMISBLACK, SOKOBANDOMAIN, new String[]{ROOMCLASS});
		PropositionalFunction roomIsBlue = new IsBluePF(PFROOMISBLUE, SOKOBANDOMAIN, new String[]{ROOMCLASS});
		//PropositionalFunction roomIsCyan = new IsCyanPF(PFROOMISCYAN, SOKOBANDOMAIN, new String[]{ROOMCLASS});
		//PropositionalFunction roomIsDarkGray = new IsDarkGrayPF(PFROOMISDARKGRAY, SOKOBANDOMAIN, new String[]{ROOMCLASS});
		//PropositionalFunction roomIsGray = new IsGrayPF(PFROOMISGRAY, SOKOBANDOMAIN, new String[]{ROOMCLASS});
		PropositionalFunction roomIsGreen = new IsGreenPF(PFROOMISGREEN, SOKOBANDOMAIN, new String[]{ROOMCLASS});
		//PropositionalFunction roomIsLightGray = new IsLightGrayPF(PFROOMISLIGHTGRAY, SOKOBANDOMAIN, new String[]{ROOMCLASS});
		//PropositionalFunction roomIsMagenta = new IsMagentaPF(PFROOMISMAGENTA, SOKOBANDOMAIN, new String[]{ROOMCLASS});
		//PropositionalFunction roomIsOrange = new IsOrangePF(PFROOMISORANGE, SOKOBANDOMAIN, new String[]{ROOMCLASS});
		//PropositionalFunction roomIsPink = new IsPinkPF(PFROOMISPINK, SOKOBANDOMAIN, new String[]{ROOMCLASS});
		PropositionalFunction roomIsRed = new IsRedPF(PFROOMISRED, SOKOBANDOMAIN, new String[]{ROOMCLASS});
		//PropositionalFunction roomIsWhite = new IsWhitePF(PFROOMISWHITE, SOKOBANDOMAIN, new String[]{ROOMCLASS});
		PropositionalFunction roomIsYellow = new IsYellowPF(PFROOMISYELLOW, SOKOBANDOMAIN, new String[]{ROOMCLASS});

		//isSquare.setClassName(PFSHAPECLASS);
		isCircle.setClassName(PFSHAPECLASS);
		isStar.setClassName(PFSHAPECLASS);
		//isMoon.setClassName(PFSHAPECLASS);
		//isSmiley.setClassName(PFSHAPECLASS);

		inRoom.setClassName(PFPOSCLASS);
		inRoom2.setClassName(PFBPOSCLASS);
		//nextTo.setClassName(PFPOSCLASS);
		//nextTo2.setClassName(PFPOSCLASS);

		//isBlack.setClassName(PFCOLORCLASS);
		isBlue.setClassName(PFBCOLORCLASS);
		//isCyan.setClassName(PFCOLORCLASS);
		//isDarkGray.setClassName(PFCOLORCLASS);
		//isGray.setClassName(PFCOLORCLASS);
		//isGreen.setClassName(PFCOLORCLASS);
		//isLightGray.setClassName(PFCOLORCLASS);
		//isMagenta.setClassName(PFCOLORCLASS);
		//isOrange.setClassName(PFCOLORCLASS);
		//isPink.setClassName(PFCOLORCLASS);
		isRed.setClassName(PFBCOLORCLASS);
		//isWhite.setClassName(PFCOLORCLASS);
		isYellow.setClassName(PFBCOLORCLASS);

		//roomIsBlack.setClassName(PFCOLORCLASS);
		roomIsBlue.setClassName(PFRCOLORCLASS);
		//roomIsCyan.setClassName(PFCOLORCLASS);
		//roomIsDarkGray.setClassName(PFCOLORCLASS);
		//roomIsGray.setClassName(PFCOLORCLASS);
		roomIsGreen.setClassName(PFRCOLORCLASS);
		//roomIsLightGray.setClassName(PFCOLORCLASS);
		//roomIsMagenta.setClassName(PFCOLORCLASS);
		//roomIsOrange.setClassName(PFCOLORCLASS);
		//roomIsPink.setClassName(PFCOLORCLASS);
		roomIsRed.setClassName(PFRCOLORCLASS);
		//roomIsWhite.setClassName(PFCOLORCLASS);
		roomIsYellow.setClassName(PFRCOLORCLASS);

		Action north = new NorthAction(ACTIONNORTH, SOKOBANDOMAIN, "");
		Action south = new SouthAction(ACTIONSOUTH, SOKOBANDOMAIN, "");
		Action east = new EastAction(ACTIONEAST, SOKOBANDOMAIN, "");
		Action west = new WestAction(ACTIONWEST, SOKOBANDOMAIN, "");

		HashMap<String, String> variableMap2 = new HashMap<String, String>();
		variableMap2.put("a", AGENTCLASS);
		variableMap2.put("b1", BLOCKCLASS);
		variableMap2.put("b2", BLOCKCLASS);
		variableMap2.put("r1", ROOMCLASS);
		variableMap2.put("r2", ROOMCLASS);
		
		/*
		LogicalExpression someGoal2 = new LogicalExpression();
		someGoal2.addScope(new Scope(Scope.EXISTENTIAL, variableMap2));
		someGoal2.setExpression(
			new Disjunction(new LogicalExpressionClause[]{
				new LogicalPropositionalFunction(inRoom, new String[]{"b1", "r1"}),
				new LogicalPropositionalFunction(inRoom, new String[]{"b2", "r1"}),
				new LogicalPropositionalFunction(inRoom, new String[]{"a", "r2"})
			})
		);
		SOKOBANDOMAIN.addLogicalExpression(someGoal2);
		*/

		//	this.createMap();

		return SOKOBANDOMAIN;
	}

	/**
	 * Generates the first state by instantiating every object and adding it to the state
	 * @return the initial state of the domain
	 */
	public State getCleanState(){

		this.generateDomain(); //make sure the domain is created first

		ObjectInstance agent = new ObjectInstance(SOKOBANDOMAIN.getObjectClass(AGENTCLASS), AGENTCLASS + 0);
		ObjectInstance block = new ObjectInstance(SOKOBANDOMAIN.getObjectClass(BLOCKCLASS), BLOCKCLASS + 0);
		ObjectInstance block2 = new ObjectInstance(SOKOBANDOMAIN.getObjectClass(BLOCKCLASS), BLOCKCLASS + 1);
		ObjectInstance room = new ObjectInstance(SOKOBANDOMAIN.getObjectClass(ROOMCLASS), ROOMCLASS + 0);
		ObjectInstance room2 = new ObjectInstance(SOKOBANDOMAIN.getObjectClass(ROOMCLASS), ROOMCLASS + 1);
		ObjectInstance room3 = new ObjectInstance(SOKOBANDOMAIN.getObjectClass(ROOMCLASS), ROOMCLASS + 2);
		ObjectInstance door = new ObjectInstance(SOKOBANDOMAIN.getObjectClass(DOORCLASS), DOORCLASS + 0);
		ObjectInstance door2 = new ObjectInstance(SOKOBANDOMAIN.getObjectClass(DOORCLASS), DOORCLASS + 1);

		State st = new State();
		st.addObject(room);
		st.addObject(room2);
		st.addObject(room3);
		st.addObject(block);
		st.addObject(block2);
		st.addObject(agent);
		st.addObject(door);
		st.addObject(door2);

		return st;
	} 
	
	public State getCleanStateNBlocks(int n){
		
		this.generateDomain(); //make sure the domain is created first

		ObjectInstance agent = new ObjectInstance(SOKOBANDOMAIN.getObjectClass(AGENTCLASS), AGENTCLASS + 0);
		ObjectInstance room = new ObjectInstance(SOKOBANDOMAIN.getObjectClass(ROOMCLASS), ROOMCLASS + 0);
		ObjectInstance room2 = new ObjectInstance(SOKOBANDOMAIN.getObjectClass(ROOMCLASS), ROOMCLASS + 1);
		ObjectInstance room3 = new ObjectInstance(SOKOBANDOMAIN.getObjectClass(ROOMCLASS), ROOMCLASS + 2);
		ObjectInstance door = new ObjectInstance(SOKOBANDOMAIN.getObjectClass(DOORCLASS), DOORCLASS + 0);
		ObjectInstance door2 = new ObjectInstance(SOKOBANDOMAIN.getObjectClass(DOORCLASS), DOORCLASS + 1);
		
		List <ObjectInstance> blocks = new ArrayList<ObjectInstance>();
		for(int i = 0; i < n; i++){
			ObjectInstance block = new ObjectInstance(SOKOBANDOMAIN.getObjectClass(BLOCKCLASS), BLOCKCLASS + i);
			blocks.add(block);
		}

		State st = new State();
		st.addObject(room);
		st.addObject(room2);
		st.addObject(room3);
		
		for(ObjectInstance block : blocks){
			st.addObject(block);
		}
		
		st.addObject(agent);
		st.addObject(door);
		st.addObject(door2);

		return st;
		
	}
	
	public State getCleanStateNBlocksNDoors(int nb, int nd){
		
		this.generateDomain(); //make sure the domain is created first

		ObjectInstance agent = new ObjectInstance(SOKOBANDOMAIN.getObjectClass(AGENTCLASS), AGENTCLASS + 0);
		ObjectInstance room = new ObjectInstance(SOKOBANDOMAIN.getObjectClass(ROOMCLASS), ROOMCLASS + 0);
		ObjectInstance room2 = new ObjectInstance(SOKOBANDOMAIN.getObjectClass(ROOMCLASS), ROOMCLASS + 1);
		ObjectInstance room3 = new ObjectInstance(SOKOBANDOMAIN.getObjectClass(ROOMCLASS), ROOMCLASS + 2);
		
		
		
		List <ObjectInstance> doors = new ArrayList<ObjectInstance>();
		for(int i = 0; i < nd; i++){
			ObjectInstance door = new ObjectInstance(SOKOBANDOMAIN.getObjectClass(DOORCLASS), DOORCLASS + i);
			doors.add(door);
		}
		
		List <ObjectInstance> blocks = new ArrayList<ObjectInstance>();
		for(int i = 0; i < nb; i++){
			ObjectInstance block = new ObjectInstance(SOKOBANDOMAIN.getObjectClass(BLOCKCLASS), BLOCKCLASS + i);
			blocks.add(block);
		}

		State st = new State();
		st.addObject(room);
		st.addObject(room2);
		st.addObject(room3);
		
		for(ObjectInstance block : blocks){
			st.addObject(block);
		}
		
		st.addObject(agent);
		
		for(ObjectInstance door : doors){
			st.addObject(door);
		}

		return st;
		
		
		
	}

	//creates the walls using the coordinates of rooms
	public static void createMap(State st){

		MAXX = getMaxX(st);
		MAXY = getMaxY(st);
		
		MAP = new int[MAXX+1][MAXY+1];
		
		//start by zeroing it out
		for(int i = 0; i < MAXX; i++){
			for(int j = 0; j < MAXY; j++){
				MAP[i][j] = 0;
			}
		}

		List<ObjectInstance> rooms = st.getObjectsOfTrueClass(ROOMCLASS);
		List<ObjectInstance> doors = st.getObjectsOfTrueClass(DOORCLASS);

		for (int i = 0; i < rooms.size(); i++){
			ObjectInstance room = rooms.get(i);

			int topX = room.getDiscValForAttribute(TOPXATTNAME);
			int topY = room.getDiscValForAttribute(TOPYATTNAME);
			int botX = room.getDiscValForAttribute(BOTTOMXATTNAME);
			int botY = room.getDiscValForAttribute(BOTTOMYATTNAME);

			//put walls at the edges of rooms
			for(int j = 0; j < MAXX; j++){
				for(int k = 0; k < MAXY; k++){
					if (j >= topX && j <= botX && k == topY){
						MAP[j][k] = 1;
					}
					else if (j >= topX && j <= botX && k == botY){
						MAP[j][k] = 1;
					}
					else if (j == topX && k >= botY && k <= topY){
						MAP[j][k] = 1;
					}
					else if (j == botX && k >= botY && k <= topY){
						MAP[j][k] = 1;
					}
				}
			}
		}

		//remove walls where doorways are
		for (int i = 0; i < doors.size(); i++){
			ObjectInstance door = doors.get(i);

			int topX = door.getDiscValForAttribute(TOPXATTNAME);
			int topY = door.getDiscValForAttribute(TOPYATTNAME);
			int botX = door.getDiscValForAttribute(BOTTOMXATTNAME);
			int botY = door.getDiscValForAttribute(BOTTOMYATTNAME);

			for(int j = 0; j < MAXX; j++){
				for(int k = 0; k < MAXY; k++){
					if (j >= topX && j <= botX && k == topY){
						MAP[j][k] = 0;
					}
					else if (j >= topX && j <= botX && k == botY){
						MAP[j][k] = 0;
					}
					else if (j == topX && k >= botY && k <= topY){
						MAP[j][k] = 0;
					}
					else if (j == botX && k >= botY && k <= topY){
						MAP[j][k] = 0;
					}
				}
			}
		}
	}

	/**
	 * Attempts to move the agent into the given position, taking into account walls and blocks
	 * @param the current state
	 * @param the attempted new X position of the agent
	 * @param the attempted new Y position of the agent
	 */
	public static void move(State st, int x, int y){

		ObjectInstance agent = st.getObjectsOfTrueClass(AGENTCLASS).get(0);
		List<ObjectInstance> blocks = st.getObjectsOfTrueClass(BLOCKCLASS);
		int curX = agent.getDiscValForAttribute(XATTNAME);
		int curY = agent.getDiscValForAttribute(YATTNAME);
		int nx = curX + x;
		int ny = curY + y;

		//check to see if the agent is at the boundary
		if(nx < MINX || nx > MAXX){
			nx = curX;
		}
		if(ny < MINY || ny > MAXY){
			ny = curY;
		}

		if(MAP[nx][ny] == 1){ //wall cannot move there
			nx = curX;
			ny = curY;
		}

		//check to see if you're moving into a block
		for (int i = 0; i < blocks.size(); i++){
			ObjectInstance block = blocks.get(i);
			int blockX = block.getDiscValForAttribute(XATTNAME);
			int blockY = block.getDiscValForAttribute(YATTNAME);

			if(nx == blockX && ny == blockY){ //block found
				int bx = pushBlockCoords(curX, curY, nx, ny)[0];
				int by = pushBlockCoords(curX, curY, nx, ny)[1];
				if (moveBlock(st, block, bx, by) == false){
					nx = curX;
					ny = curY;
				}
			}	
		}

		agent.setValue(XATTNAME, nx);
		agent.setValue(YATTNAME, ny);
	}

	//move function for pushing blocks
	public static boolean moveBlock(State st, ObjectInstance block, int x, int y){

		int curX = block.getDiscValForAttribute(XATTNAME);
		int curY = block.getDiscValForAttribute(YATTNAME);
		List<ObjectInstance> blocks = st.getObjectsOfTrueClass(BLOCKCLASS);
		int nx = x;
		int ny = y;

		//check for the boundaries of the world
		if(nx < MINX || nx > MAXX){
			nx = curX;
			return false;
		}
		if(ny < MINY || ny > MAXY){
			ny = curY;
			return false;
		}

		//check for collisions with other blocks
		for (int i = 0; i < blocks.size(); i++){
			ObjectInstance block2 = blocks.get(i);
			int blockX = block2.getDiscValForAttribute(XATTNAME);
			int blockY = block2.getDiscValForAttribute(YATTNAME);

			if(nx == blockX && ny == blockY){ //block cannot move there
				int bx = pushBlockCoords(curX, curY, nx, ny)[0];
				int by = pushBlockCoords(curX, curY, nx, ny)[1];
				if (nx == bx || ny == by){
					nx = curX;
					ny = curY;
					return false;
				}
			}	
		}

		//check for collision with walls
		if(MAP[nx][ny] == 1){
			nx = curX;
			ny = curY;
			return false;
		}

		block.setValue(XATTNAME, nx);
		block.setValue(YATTNAME, ny);
		return true;
	}

	//don't let the title deceive you: this propositional function should work for both agents and blocks inside
	//both rooms and doorways
	class InRoomPF extends PropositionalFunction{

		public InRoomPF(String name, Domain domain, String parameterClasses) {
			super(name, domain, parameterClasses);
		}

		public InRoomPF(String name, Domain domain, String [] parameterClasses) {
			super(name, domain, parameterClasses);
		}

		public boolean isTrue(State st, String[] params) {

			if(params.length != 2){
				return false;
			}

			ObjectInstance block = st.getObject(params[0]);
			ObjectInstance room = st.getObject(params[1]);

			if(block == null || room == null){
				return false;
			}

			int aX = block.getDiscValForAttribute(XATTNAME);
			int aY = block.getDiscValForAttribute(YATTNAME);
			int rX1 = room.getDiscValForAttribute(TOPXATTNAME);
			int rX2 = room.getDiscValForAttribute(BOTTOMXATTNAME);
			int rY1 = room.getDiscValForAttribute(TOPYATTNAME);
			int rY2 = room.getDiscValForAttribute(BOTTOMYATTNAME);

			if(aX < rX2 && aX > rX1 && aY > rY2 && aY < rY1){
				return true;
			}

			return false;
		}
	}

	//propositional functions for colors + shapes
	class IsStarPF extends PropositionalFunction{
		public IsStarPF(String name, Domain domain, String parameterClasses) {super(name, domain, parameterClasses);}
		public IsStarPF(String name, Domain domain, String[] parameterClasses) {super(name, domain, parameterClasses);}
		public boolean isTrue(State st, String[] params) {
			if(params.length != 1){ return false; }
			if (st.getObject(params[0]).getDiscValForAttribute(SHAPEATTNAME) == 0){ return true; }
			return false;
		}
	}

	class IsMoonPF extends PropositionalFunction{
		public IsMoonPF(String name, Domain domain, String parameterClasses) {super(name, domain, parameterClasses);}
		public IsMoonPF(String name, Domain domain, String[] parameterClasses) {super(name, domain, parameterClasses);}
		public boolean isTrue(State st, String[] params) {
			if(params.length != 1){ return false; }
			if (st.getObject(params[0]).getDiscValForAttribute(SHAPEATTNAME) == 1){ return true; }
			return false;
		}
	}
	class IsCirclePF extends PropositionalFunction{
		public IsCirclePF(String name, Domain domain, String parameterClasses) {super(name, domain, parameterClasses);}
		public IsCirclePF(String name, Domain domain, String[] parameterClasses) {super(name, domain, parameterClasses);}
		public boolean isTrue(State st, String[] params) {
			if(params.length != 1){ return false; }
			if (st.getObject(params[0]).getDiscValForAttribute(SHAPEATTNAME) == 2){ return true; }
			return false;
		}
	}
	class IsSmileyPF extends PropositionalFunction{
		public IsSmileyPF(String name, Domain domain, String parameterClasses) {super(name, domain, parameterClasses);}
		public IsSmileyPF(String name, Domain domain, String[] parameterClasses) {super(name, domain, parameterClasses);}
		public boolean isTrue(State st, String[] params) {
			if(params.length != 1){ return false; }
			if (st.getObject(params[0]).getDiscValForAttribute(SHAPEATTNAME) == 3){ return true; }
			return false;
		}
	}
	class IsSquarePF extends PropositionalFunction{
		public IsSquarePF(String name, Domain domain, String parameterClasses) {super(name, domain, parameterClasses);}
		public IsSquarePF(String name, Domain domain, String[] parameterClasses) {super(name, domain, parameterClasses);}
		public boolean isTrue(State st, String[] params) {
			if(params.length != 1){ return false; }
			if (st.getObject(params[0]).getDiscValForAttribute(SHAPEATTNAME) == 4){ return true; }
			return false;
		}
	}
	class IsBlackPF extends PropositionalFunction{
		public IsBlackPF(String name, Domain domain, String parameterClasses) {super(name, domain, parameterClasses);}
		public IsBlackPF(String name, Domain domain, String[] parameterClasses) {super(name, domain, parameterClasses);}
		public boolean isTrue(State st, String[] params) {
			if(params.length != 1){ return false; }
			if (st.getObject(params[0]).getDiscValForAttribute(COLORATTNAME) == 0){ return true; }
			return false;
		}
	}
	class IsBluePF extends PropositionalFunction{
		public IsBluePF(String name, Domain domain, String parameterClasses) {super(name, domain, parameterClasses);}
		public IsBluePF(String name, Domain domain, String[] parameterClasses) {super(name, domain, parameterClasses);}
		public boolean isTrue(State st, String[] params) {
			if(params.length != 1){ return false; }
			if (st.getObject(params[0]).getDiscValForAttribute(COLORATTNAME) == 1){ return true; }
			return false;
		}
	}
	class IsCyanPF extends PropositionalFunction{
		public IsCyanPF(String name, Domain domain, String parameterClasses) {super(name, domain, parameterClasses);}
		public IsCyanPF(String name, Domain domain, String[] parameterClasses) {super(name, domain, parameterClasses);}
		public boolean isTrue(State st, String[] params) {
			if(params.length != 1){ return false; }
			if (st.getObject(params[0]).getDiscValForAttribute(COLORATTNAME) == 2){ return true; }
			return false;
		}
	}
	class IsDarkGrayPF extends PropositionalFunction{
		public IsDarkGrayPF(String name, Domain domain, String parameterClasses) {super(name, domain, parameterClasses);}
		public IsDarkGrayPF(String name, Domain domain, String[] parameterClasses) {super(name, domain, parameterClasses);}
		public boolean isTrue(State st, String[] params) {
			if(params.length != 1){ return false; }
			if (st.getObject(params[0]).getDiscValForAttribute(COLORATTNAME) == 3){ return true; }
			return false;
		}
	}
	class IsGrayPF extends PropositionalFunction{
		public IsGrayPF(String name, Domain domain, String parameterClasses) {super(name, domain, parameterClasses);}
		public IsGrayPF(String name, Domain domain, String[] parameterClasses) {super(name, domain, parameterClasses);}
		public boolean isTrue(State st, String[] params) {
			if(params.length != 1){ return false; }
			if (st.getObject(params[0]).getDiscValForAttribute(COLORATTNAME) == 4){ return true; }
			return false;
		}
	}
	class IsGreenPF extends PropositionalFunction{
		public IsGreenPF(String name, Domain domain, String parameterClasses) {super(name, domain, parameterClasses);}
		public IsGreenPF(String name, Domain domain, String[] parameterClasses) {super(name, domain, parameterClasses);}
		public boolean isTrue(State st, String[] params) {
			if(params.length != 1){ return false; }
			if (st.getObject(params[0]).getDiscValForAttribute(COLORATTNAME) == 5){ return true; }
			return false;
		}
	}
	class IsLightGrayPF extends PropositionalFunction{
		public IsLightGrayPF(String name, Domain domain, String parameterClasses) {super(name, domain, parameterClasses);}
		public IsLightGrayPF(String name, Domain domain, String[] parameterClasses) {super(name, domain, parameterClasses);}
		public boolean isTrue(State st, String[] params) {
			if(params.length != 1){ return false; }
			if (st.getObject(params[0]).getDiscValForAttribute(COLORATTNAME) == 6){ return true; }
			return false;
		}
	}
	class IsMagentaPF extends PropositionalFunction{
		public IsMagentaPF(String name, Domain domain, String parameterClasses) {super(name, domain, parameterClasses);}
		public IsMagentaPF(String name, Domain domain, String[] parameterClasses) {super(name, domain, parameterClasses);}
		public boolean isTrue(State st, String[] params) {
			if(params.length != 1){ return false; }
			if (st.getObject(params[0]).getDiscValForAttribute(COLORATTNAME) == 7){ return true; }
			return false;
		}
	}
	class IsOrangePF extends PropositionalFunction{
		public IsOrangePF(String name, Domain domain, String parameterClasses) {super(name, domain, parameterClasses);}
		public IsOrangePF(String name, Domain domain, String[] parameterClasses) {super(name, domain, parameterClasses);}
		public boolean isTrue(State st, String[] params) {
			if(params.length != 1){ return false; }
			if (st.getObject(params[0]).getDiscValForAttribute(COLORATTNAME) == 8){ return true; }
			return false;
		}
	}
	class IsPinkPF extends PropositionalFunction{
		public IsPinkPF(String name, Domain domain, String parameterClasses) {super(name, domain, parameterClasses);}
		public IsPinkPF(String name, Domain domain, String[] parameterClasses) {super(name, domain, parameterClasses);}
		public boolean isTrue(State st, String[] params) {
			if(params.length != 1){ return false; }
			if (st.getObject(params[0]).getDiscValForAttribute(COLORATTNAME) == 9){ return true; }
			return false;
		}
	}
	class IsRedPF extends PropositionalFunction{
		public IsRedPF(String name, Domain domain, String parameterClasses) {super(name, domain, parameterClasses);}
		public IsRedPF(String name, Domain domain, String[] parameterClasses) {super(name, domain, parameterClasses);}
		public boolean isTrue(State st, String[] params) {
			if(params.length != 1){ return false; }
			if (st.getObject(params[0]).getDiscValForAttribute(COLORATTNAME) == 10){ return true; }
			return false;
		}
	}
	class IsWhitePF extends PropositionalFunction{
		public IsWhitePF(String name, Domain domain, String parameterClasses) {super(name, domain, parameterClasses);}
		public IsWhitePF(String name, Domain domain, String[] parameterClasses) {super(name, domain, parameterClasses);}
		public boolean isTrue(State st, String[] params) {
			if(params.length != 1){ return false; }
			if (st.getObject(params[0]).getDiscValForAttribute(COLORATTNAME) == 11){ return true; }
			return false;
		}
	}
	class IsYellowPF extends PropositionalFunction{
		public IsYellowPF(String name, Domain domain, String parameterClasses) {super(name, domain, parameterClasses);}
		public IsYellowPF(String name, Domain domain, String[] parameterClasses) {super(name, domain, parameterClasses);}
		public boolean isTrue(State st, String[] params) {
			if(params.length != 1){ return false; }
			if (st.getObject(params[0]).getDiscValForAttribute(COLORATTNAME) == 12){ return true; }
			return false;
		}
	}

	class NextToPF extends PropositionalFunction{
		public NextToPF(String name, Domain domain, String parameterClasses) {super(name, domain, parameterClasses);}
		public NextToPF(String name, Domain domain, String[] parameterClasses) {super(name, domain, parameterClasses);}
		public boolean isTrue(State st, String[] params) {
			if(params.length != 2){ return false; }
			int topX = st.getObject(params[1]).getDiscValForAttribute(TOPXATTNAME);
			int botX = st.getObject(params[1]).getDiscValForAttribute(BOTTOMXATTNAME);
			int topY = st.getObject(params[1]).getDiscValForAttribute(TOPYATTNAME);
			int botY = st.getObject(params[1]).getDiscValForAttribute(BOTTOMYATTNAME);
			int x = st.getObject(params[0]).getDiscValForAttribute(XATTNAME);
			int y = st.getObject(params[0]).getDiscValForAttribute(YATTNAME);
			if ((topX - 1) == x && y <= topY && y >= botY) { return true; }
			else if ((botX + 1) == x && y <= topY && y >= botY) { return true; }
			else if ((topY + 1) == y && x <= botX && x >= topX) { return true; }
			else if ((botY - 1) == y && x <= botX && x >= topX) {  return true; }
			return false;
		}
	}

	//gives coordinates to move a pushed block to
	private static int[] pushBlockCoords(int x, int y, int nx, int ny){
		int[] coords = new int[2];
		if (nx == x && ny == (y + 1)){
			coords[0] = x;
			coords[1] = y + 2;
		}
		else if (nx == x && ny == (y - 1)){
			coords[0] = x;
			coords[1] = y - 2;
		}
		else if (nx == (x + 1) && ny == y){
			coords[0] = x + 2;
			coords[1] = y;
		}
		else if (nx == (x - 1) && ny == y){
			coords[0] = x - 2;
			coords[1] = y;
		}

		return coords;
	}

	class NorthAction extends Action{
		public NorthAction(String name, Domain domain, String parameterClasses) {super(name, domain, parameterClasses);}
		public NorthAction(String name, Domain domain, String [] parameterClasses) {super(name, domain, parameterClasses);}
		protected State performActionHelper(State st, String[] params) {
			SokobanDomain.createMap(st);
			SokobanDomain.move(st, 0, 1);
			return st;
		}
		@Override
		public List<TransitionProbability> getTransitions(State s, String [] params){
			return this.deterministicTransition(s, params);
		}
	}

	class SouthAction extends Action{
		public SouthAction(String name, Domain domain, String parameterClasses) {super(name, domain, parameterClasses);}
		public SouthAction(String name, Domain domain, String [] parameterClasses) {super(name, domain, parameterClasses);}
		protected State performActionHelper(State st, String[] params) {
			SokobanDomain.createMap(st);
			SokobanDomain.move(st, 0, -1);
			return st;
		}
		@Override
		public List<TransitionProbability> getTransitions(State s, String [] params){
			return this.deterministicTransition(s, params);
		}
	}

	class EastAction extends Action{
		public EastAction(String name, Domain domain, String parameterClasses) {super(name, domain, parameterClasses);}
		public EastAction(String name, Domain domain, String [] parameterClasses) {super(name, domain, parameterClasses);}
		protected State performActionHelper(State st, String[] params) {
			SokobanDomain.createMap(st);
			SokobanDomain.move(st, 1, 0);
			return st;
		}
		@Override
		public List<TransitionProbability> getTransitions(State s, String [] params){
			return this.deterministicTransition(s, params);
		}
	}

	class WestAction extends Action{
		public WestAction(String name, Domain domain, String parameterClasses) {super(name, domain, parameterClasses);}
		public WestAction(String name, Domain domain, String [] parameterClasses) {super(name, domain, parameterClasses);}
		protected State performActionHelper(State st, String[] params) {
			SokobanDomain.createMap(st);
			SokobanDomain.move(st, -1, 0);
			return st;
		}
		@Override
		public List<TransitionProbability> getTransitions(State s, String [] params){
			return this.deterministicTransition(s, params);
		}
	}

	@Override
	/**
	 * @return The string representation of a state
	 */
	public String stateToString(State s) {
		String output = "";
		List<ObjectInstance> rooms = s.getObjectsOfTrueClass(ROOMCLASS);
		List<ObjectInstance> doors = s.getObjectsOfTrueClass(DOORCLASS);
		List<ObjectInstance> blocks = s.getObjectsOfTrueClass(BLOCKCLASS);
		List<ObjectInstance> agents = s.getObjectsOfTrueClass(AGENTCLASS);

		for (int i = 0; i < rooms.size(); i++){
			output += "room,";
			output = output + rooms.get(i).getDiscValForAttribute(COLORATTNAME) + ",";
			output = output + rooms.get(i).getDiscValForAttribute(TOPXATTNAME) + ",";
			output = output + rooms.get(i).getDiscValForAttribute(TOPYATTNAME) + ",";
			output = output + rooms.get(i).getDiscValForAttribute(BOTTOMXATTNAME) + ",";
			output = output + rooms.get(i).getDiscValForAttribute(BOTTOMYATTNAME) + " ";
		}

		for (int i = 0; i < doors.size(); i++){
			output += "door,";
			output = output + doors.get(i).getDiscValForAttribute(TOPXATTNAME) + ",";
			output = output + doors.get(i).getDiscValForAttribute(TOPYATTNAME) + ",";
			output = output + doors.get(i).getDiscValForAttribute(BOTTOMXATTNAME) + ",";
			output = output + doors.get(i).getDiscValForAttribute(BOTTOMYATTNAME) + " ";
		}

		for (int i = 0; i < blocks.size(); i++){
			output += "block,";
			output = output + blocks.get(i).getDiscValForAttribute(COLORATTNAME) + ",";
			output = output + blocks.get(i).getDiscValForAttribute(SHAPEATTNAME) + ",";
			output = output + blocks.get(i).getDiscValForAttribute(XATTNAME) + ",";
			output = output + blocks.get(i).getDiscValForAttribute(YATTNAME) + " ";
		}

		for (int i = 0; i < agents.size(); i++){
			output += "agent,";
			output = output + agents.get(i).getDiscValForAttribute(XATTNAME) + ",";
			output = output + agents.get(i).getDiscValForAttribute(YATTNAME);
		}
		return output;
	}

	@Override
	public State stringToState(String str) {
		State st = new State();
		int rooms = 0;
		int doors = 0;
		int blocks = 0;
		int agents = 0;
		String[] objects = str.split(" ");
		for (int i = 0; i < objects.length; i++){
			String[] splitobject = objects[i].split(",");
			if (splitobject[0].equals("room")){
				ObjectInstance room = new ObjectInstance(SOKOBANDOMAIN.getObjectClass(ROOMCLASS), ROOMCLASS + rooms);
				room.setValue(COLORATTNAME, colors.get(Integer.parseInt(splitobject[1])));
				room.setValue(TOPXATTNAME, Integer.parseInt(splitobject[2]));
				room.setValue(TOPYATTNAME, Integer.parseInt(splitobject[3]));
				room.setValue(BOTTOMXATTNAME, Integer.parseInt(splitobject[4]));
				room.setValue(BOTTOMYATTNAME, Integer.parseInt(splitobject[5]));
				st.addObject(room);
				rooms++;
			}
			else if (splitobject[0].equals("door")){
				ObjectInstance door = new ObjectInstance(SOKOBANDOMAIN.getObjectClass(DOORCLASS), DOORCLASS + doors);
				door.setValue(TOPXATTNAME, Integer.parseInt(splitobject[1]));
				door.setValue(TOPYATTNAME, Integer.parseInt(splitobject[2]));
				door.setValue(BOTTOMXATTNAME, Integer.parseInt(splitobject[3]));
				door.setValue(BOTTOMYATTNAME, Integer.parseInt(splitobject[4]));
				st.addObject(door);
				doors++;
			}
			else if (splitobject[0].equals("block")){
				ObjectInstance block = new ObjectInstance(SOKOBANDOMAIN.getObjectClass(BLOCKCLASS), BLOCKCLASS + blocks);
				block.setValue(COLORATTNAME, colors.get(Integer.parseInt(splitobject[1])));
				block.setValue(SHAPEATTNAME, shapes.get(Integer.parseInt(splitobject[2])));
				block.setValue(XATTNAME, Integer.parseInt(splitobject[3]));
				block.setValue(YATTNAME, Integer.parseInt(splitobject[4]));
				st.addObject(block);
				blocks++;
			}
			else if (splitobject[0].equals("agent")){
				ObjectInstance agent = new ObjectInstance(SOKOBANDOMAIN.getObjectClass(AGENTCLASS), AGENTCLASS + agents);
				agent.setValue(XATTNAME, Integer.parseInt(splitobject[1].trim()));
				agent.setValue(YATTNAME, Integer.parseInt(splitobject[2].trim()));
				st.addObject(agent);
				agents++;
			}
		}
		createMap(st);
		return st;
	}
	public static void runTrajectory(SokobanDomain constructor, Domain domain, State st, String file){
		BufferedReader in = null;
		String currentState = null;
		String trajectoryString = null;
		String command = null;
		Trajectory trajectory = null;
		ArrayList<State> states = new ArrayList<State>();

		try {
			in = new BufferedReader(new FileReader(file));
			command = in.readLine();
			trajectoryString = in.readLine();
			currentState = in.readLine();
			while(currentState != null){
				states.add(constructor.stringToState(currentState));
				currentState = in.readLine();
			}
			in.close();
		} catch (FileNotFoundException e) {
			System.out.println("Error: File \"" + file + "\" not found.");
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		ArrayList<GroundedAction> actions = new ArrayList<GroundedAction>();

		ArrayList<String> stringActions = new ArrayList<String>(Arrays.asList(trajectoryString.split(" ")));

		//perform actions from file
		for (int i = 0; i < stringActions.size(); i++){
			Action action = domain.getAction(stringActions.get(i));
			actions.add(new GroundedAction(action, ""));
			st = action.performAction(st, "");
		}
		trajectory = new Trajectory(states, actions);
		TrainingElement element = new TrainingElement(command, trajectory);
		//System.out.println(constructor.stateToString(element.trajectory.getState(element.trajectory.numStates() - 1)));
		System.out.println("Trajectory completed");
	}

	public static int getMaxX(State st){
		List<ObjectInstance> rooms = st.getObjectsOfTrueClass(ROOMCLASS);
		int x = 0;
		for (int i = 0; i < rooms.size(); i++){
			ObjectInstance room = rooms.get(i); 
			if (room.getDiscValForAttribute(BOTTOMXATTNAME) > x){
				x = room.getDiscValForAttribute(BOTTOMXATTNAME);
			}
		}
		return x+1;
	}

	public static int getMaxY(State st){
		List<ObjectInstance> rooms = st.getObjectsOfTrueClass(ROOMCLASS);
		int y = 0;
		for (int i = 0; i < rooms.size(); i++){
			ObjectInstance room = rooms.get(i); 
			if (room.getDiscValForAttribute(TOPYATTNAME) > y){
				y = room.getDiscValForAttribute(TOPYATTNAME);
			}
		}
		return y+1;
	}


	
	public static Map <SokoSAS, Double> getTransitionsFromSourceState(State s){
		
		SokobanDomain constructor = new SokobanDomain();
		Domain d = constructor.generateDomain();
		
		List <Action> actions = d.getActions();
		
		
		Map <SokoSAS, Double> transitionMatrix = new HashMap<SokoSAS,Double>();
		LinkedList <State> openList = new LinkedList<State>();
		Set <Integer> closedList = new HashSet<Integer>();
		
		openList.addLast(s);
		while(openList.size() > 0){
			
			//dequeue a state to expand
			State ex = openList.poll();
			int id = SokoSAS.getStateId(ex);
			
			//only expand if it's not been previously expanded
			if(!closedList.contains(id)){
				
				//add to close list to avoid no-ops from re-adding this state to the open list
				closedList.add(id);
				
				//expand it
				for(int i = 0; i < actions.size(); i++){
					
					Action act = actions.get(i);
					State sPrime = act.performAction(ex, "");
					int spid = SokoSAS.getStateId(sPrime);
					
					SokoSAS sas = new SokoSAS(id, i, spid);
					transitionMatrix.put(sas, 1.0);
					
					//add prime to open list if not already closed
					if(!closedList.contains(spid)){
						openList.addLast(sPrime);
					}
					
				}
				
			}
			
			
		}
		
		return transitionMatrix;
	}
	
	public static class SokoSAS{

		public int sId;
		public int aId;
		public int sPrimeId;



		public SokoSAS(int si, int ai, int spi){
			sId = si;
			aId = ai;
			sPrimeId = spi;
		}
		
		
		@Override
		public int hashCode(){
			return sId + (sPrimeId*23) + (aId*23*23);
		}
		
		@Override
		public boolean equals(Object obj){
			SokoSAS o = (SokoSAS)obj;
			if(sId != o.sId || aId != o.aId || sPrimeId != o.sPrimeId){
				return false;
			}
			return true;
		}
		

		public static int getStateId(State s){
			int col = getMaxX(s);
			int rows = getMaxY(s);
			List<ObjectInstance> blocks = s.getObjectsOfTrueClass(BLOCKCLASS);
			ObjectInstance agent = s.getObjectsOfTrueClass(AGENTCLASS).get(0);
			int stateID = agent.getDiscValForAttribute(XATTNAME) + agent.getDiscValForAttribute(YATTNAME) * col;

			for (int i = 0; i < blocks.size(); i++){
				ObjectInstance block = blocks.get(i);
				int x = block.getDiscValForAttribute(XATTNAME);
				int y = block.getDiscValForAttribute(YATTNAME);

				stateID += x * ((int)Math.pow(rows,i+1)) * ((int)Math.pow(col, i+1));
				stateID += y * ((int)Math.pow(rows,i+1)) * ((int)Math.pow(col, i+2));

			}

			return stateID;
		}

		public static State getStateFromId(int id, State referenceState){
			State st = new State(referenceState);
			List<ObjectInstance> blocks = referenceState.getObjectsOfTrueClass(BLOCKCLASS);
			int cols = getMaxX(referenceState);
			int rows = getMaxY(referenceState);
			int x, y;
			for (int i = 0; i < (blocks.size() + 1) ; i++){
				x = id % cols;
				id = (id - (id % cols)) / cols;
				y = id % rows;
				if (i == 0){
					ObjectInstance agent = st.getObject(AGENTCLASS + 0);
					agent.setValue(XATTNAME, x);
					agent.setValue(YATTNAME, y);
				}
				else{
					ObjectInstance block = st.getObjectsOfTrueClass(BLOCKCLASS).get(i-1);
					block.setValue(XATTNAME, x);
					block.setValue(YATTNAME, y);
				}
				id = (id - (id % rows)) / rows;
			}
			return st;
		}


	}

}


