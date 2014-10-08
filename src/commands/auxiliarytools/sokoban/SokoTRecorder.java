package commands.auxiliarytools.sokoban;

import burlap.oomdp.auxiliary.StateGenerator;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.visualizer.Visualizer;

import commands.auxiliarytools.TrajectoryRecorder;
import commands.auxiliarytools.sokoban.turkexamples.Turk6;

import domain.singleagent.sokoban.SokobanDomain;
import domain.singleagent.sokoban.SokobanParser;
import domain.singleagent.sokoban.SokobanVisualizer;

public class SokoTRecorder {

	public static void main(String [] args){
		
		if(args.length != 1){
			System.out.println("Incorrect format; use:\n\tpathToDataDirectory");
			System.exit(0);
		}
		
		//StateGenerator sg = new SokoSimpleStateGen();
		StateGenerator sg = new Turk6();
		Domain d = (new SokobanDomain()).generateDomain();
		StateParser sp = new SokobanParser();
		Visualizer v = SokobanVisualizer.getVisualizer();
		
		String datapath = args[0];
		
		TrajectoryRecorder rec = new TrajectoryRecorder();
		
		rec.addKeyAction("w", SokobanDomain.ACTIONNORTH);
		rec.addKeyAction("s", SokobanDomain.ACTIONSOUTH);
		rec.addKeyAction("d", SokobanDomain.ACTIONEAST);
		rec.addKeyAction("a", SokobanDomain.ACTIONWEST);
		
		//((SokoSimpleStateGen)sg).stateConfig = 1;
		
		rec.init(v, d, sp, sg, datapath);
		
	}
	
}
