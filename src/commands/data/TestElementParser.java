package commands.data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.State;

public class TestElementParser {

	protected StateParser sp;
	
	public TestElementParser(StateParser sp){
		this.sp = sp;
	}
	
	
	public TestElement getTestElementFromString(String idenitifier, String sdata){
		int firstNewLineIndex = sdata.indexOf("\n");
		String command = sdata.substring(0, firstNewLineIndex);
		String stateString = sdata.substring(firstNewLineIndex+1);
		
		State s = sp.stringToState(stateString);
		
		return new TestElement(idenitifier, s, command);
		
	}
	
	public String getStringFromTestElement(TestElement te){
		return te.command + "\n" + sp.stateToString(te.initialState);
	}
	
	public TestElement getTestElementFromFile(String path){
		
		String [] pathElements = path.split("/");
		String fileName = pathElements[pathElements.length-1];
		if(fileName.length() == 0){
			fileName = pathElements[pathElements.length-2];
		}
		
		//read whole file into string first
		String fcont = null;
		try{
			fcont = new Scanner(new File(path)).useDelimiter("\\Z").next();
		}catch(Exception E){
			System.out.println(E);
		}
		
		return this.getTestElementFromString(fileName, fcont);
		
	}
	
	public List<TestElement> getTestElementDataset(String pathToDataDir, String dataFileExtension){
		
		List <TestElement> dataset = new ArrayList<TestElement>();
		
		
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
			dataset.add(this.getTestElementFromFile(path));
		}
		
		
		return dataset;
		
	}
	
	
	public void writeTestElementToFile(TestElement te, String path){
		//write this string out
		try{
			BufferedWriter out = new BufferedWriter(new FileWriter(path));
			out.write(this.getStringFromTestElement(te));
			out.close();
		}catch(Exception E){
			System.out.println(E);
		}
	}
	
}
