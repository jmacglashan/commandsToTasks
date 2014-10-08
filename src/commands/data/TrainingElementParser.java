package commands.data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Domain;

public class TrainingElementParser {

	TrajectoryParser tp;
	
	
	public TrainingElementParser(Domain d, StateParser sp){
		tp = new TrajectoryParser(d, sp);
	}
	
	
	public TrainingElement getTrainingElementFromString(String sdata){
		
		int firstNewLineIndex = sdata.indexOf("\n");
		String command = sdata.substring(0, firstNewLineIndex).trim();
		String trajectoryString = sdata.substring(firstNewLineIndex+1);
		Trajectory t = tp.getTrajectoryFromString(trajectoryString);
		
		
		return new TrainingElement(command, t);
	}
	
	
	public String getStringOfTrainingElement(TrainingElement te){
		return te.command + "\n" + tp.getStringRepForTrajectory(te.trajectory);
	}
	
	
	public TrainingElement getTrainingElementFromFile(String path){
		
		//read whole file into string first
		String fcont = null;
		try{
			fcont = new Scanner(new File(path)).useDelimiter("\\Z").next();
		}catch(Exception E){
			System.out.println(E);
		}
		
		//then use string parser
		return this.getTrainingElementFromString(fcont);
		
	}
	
	public List <TrainingElement> getTrainingElementDataset(String pathToDataDir, String dataFileExtension){
		
		List<TrainingElement> data = new ArrayList<TrainingElement>();
		
		
		//get rid of trailing /
		if(pathToDataDir.charAt(pathToDataDir.length()-1) == '/'){
			pathToDataDir = pathToDataDir.substring(0, pathToDataDir.length());
		}
		
		
		File dir = new File(pathToDataDir);
		final String ext = new String(dataFileExtension);
		
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				if(name.endsWith(ext)){
					return true;
				}
				return false;
			}
		};
		String[] children = dir.list(filter);
		for(int i = 0; i < children.length; i++){
			String path = pathToDataDir + "/" + children[i];
			TrainingElement te = this.getTrainingElementFromFile(path);
			te.identifier = children[i];
			data.add(te);
		}
		
		
		return data;
		
	}
	
	
	public void writeTrainingElementToFile(TrainingElement te, String path){
		//write this string out
		try{
			BufferedWriter out = new BufferedWriter(new FileWriter(path));
			out.write(this.getStringOfTrainingElement(te));
			out.close();
		}catch(Exception E){
			System.out.println(E);
		}
	}
	
}
