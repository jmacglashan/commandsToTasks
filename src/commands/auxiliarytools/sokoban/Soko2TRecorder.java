package commands.auxiliarytools.sokoban;

import commands.auxiliarytools.TrajectoryRecorder;
import commands.auxiliarytools.sokoban.soko2turkexamples.Soko2Turk1;

import burlap.oomdp.auxiliary.StateGenerator;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.visualizer.Visualizer;
import domain.singleagent.sokoban2.Sokoban2Domain;
import domain.singleagent.sokoban2.Sokoban2Parser;
import domain.singleagent.sokoban2.Sokoban2Visualizer;

public class Soko2TRecorder {

	public final static String		pathToRobotImagesDirectory = "resources/robotImages";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		if(args.length != 1){
			System.out.println("Incorrect format; use:\n\tpathToDataDirectory");
			System.exit(0);
		}
		
		Sokoban2Domain s2dg = new Sokoban2Domain();
		s2dg.includeDirectionAttribute(true);
		Domain domain = s2dg.generateDomain();
		
		StateGenerator sg = new Soko2Turk1(domain);
		Visualizer v = Sokoban2Visualizer.getVisualizer(pathToRobotImagesDirectory);
		StateParser sp = new Sokoban2Parser(domain);
		
		String datapath = args[0];
		
		TrajectoryRecorder tr = new TrajectoryRecorder();
		tr.addKeyAction("w", Sokoban2Domain.ACTIONNORTH);
		tr.addKeyAction("s", Sokoban2Domain.ACTIONSOUTH);
		tr.addKeyAction("d", Sokoban2Domain.ACTIONEAST);
		tr.addKeyAction("a", Sokoban2Domain.ACTIONWEST);

		
		tr.init(v, domain, sp, sg, datapath);
		
	}

}
