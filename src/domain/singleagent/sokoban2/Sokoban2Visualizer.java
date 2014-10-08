package domain.singleagent.sokoban2;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import burlap.oomdp.core.Attribute;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.visualizer.ObjectPainter;
import burlap.oomdp.visualizer.StateRenderLayer;
import burlap.oomdp.visualizer.Visualizer;

public class Sokoban2Visualizer {

	
	
	public static Visualizer getVisualizer(String...agentImagePath){
		
		Visualizer v = new Visualizer();
		v.addObjectClassPainter(Sokoban2Domain.CLASSROOM, new RoomPainter());
		v.addObjectClassPainter(Sokoban2Domain.CLASSDOOR, new DoorPainter());
		if(agentImagePath.length == 0){
			v.addObjectClassPainter(Sokoban2Domain.CLASSAGENT, new AgentPainter());
		}
		else{
			v.addObjectClassPainter(Sokoban2Domain.CLASSAGENT, new AgentPainterWithImages(agentImagePath[0]));
		}
		v.addObjectClassPainter(Sokoban2Domain.CLASSBLOCK, new BlockPainter(agentImagePath[0]));
		
		return v;
		
	}
	
	public static StateRenderLayer getStateRenderLayer(String...agentImagePath){
		
		StateRenderLayer v = new StateRenderLayer();
		
		v.addObjectClassPainter(Sokoban2Domain.CLASSROOM, new RoomPainter());
		v.addObjectClassPainter(Sokoban2Domain.CLASSDOOR, new DoorPainter());
		if(agentImagePath.length == 0){
			v.addObjectClassPainter(Sokoban2Domain.CLASSAGENT, new AgentPainter());
		}
		else{
			v.addObjectClassPainter(Sokoban2Domain.CLASSAGENT, new AgentPainterWithImages(agentImagePath[0]));
		}
		v.addObjectClassPainter(Sokoban2Domain.CLASSBLOCK, new BlockPainter(agentImagePath[0]));
		
		return v;
		
	}
	
	
	public static Visualizer getVisualizer(int maxX, int maxY, String...agentImagePath){
		
		Visualizer v = new Visualizer();
		
		v.addObjectClassPainter(Sokoban2Domain.CLASSROOM, new RoomPainter(maxX, maxY));
		v.addObjectClassPainter(Sokoban2Domain.CLASSDOOR, new DoorPainter(maxX, maxY));
		if(agentImagePath.length == 0){
			v.addObjectClassPainter(Sokoban2Domain.CLASSAGENT, new AgentPainter());
		}
		else{
			v.addObjectClassPainter(Sokoban2Domain.CLASSAGENT, new AgentPainterWithImages(agentImagePath[0], maxX, maxY));
		}
		v.addObjectClassPainter(Sokoban2Domain.CLASSBLOCK, new BlockPainter(maxX, maxY, agentImagePath[0]));
		
		return v;
		
	}
	
	
	public static StateRenderLayer getStateRenderLayer(int maxX, int maxY, String...agentImagePath){
		
		StateRenderLayer v = new StateRenderLayer();
		
		v.addObjectClassPainter(Sokoban2Domain.CLASSROOM, new RoomPainter(maxX, maxY));
		v.addObjectClassPainter(Sokoban2Domain.CLASSDOOR, new DoorPainter(maxX, maxY));
		if(agentImagePath.length == 0){
			v.addObjectClassPainter(Sokoban2Domain.CLASSAGENT, new AgentPainter());
			v.addObjectClassPainter(Sokoban2Domain.CLASSBLOCK, new BlockPainter(maxX, maxY));
		}
		else{
			v.addObjectClassPainter(Sokoban2Domain.CLASSAGENT, new AgentPainterWithImages(agentImagePath[0], maxX, maxY));
			v.addObjectClassPainter(Sokoban2Domain.CLASSBLOCK, new BlockPainter(maxX, maxY, agentImagePath[0]));
		}
		
		
		return v;
		
	}
	
	
	
	
	public static class RoomPainter implements ObjectPainter{

		protected int maxX = -1;
		protected int maxY = -1;
		
		public RoomPainter(){
			
		}
		
		public RoomPainter(int maxX, int maxY){
			this.maxX = maxX;
			this.maxY = maxY;
		}
		
		
		@Override
		public void paintObject(Graphics2D g2, State s, ObjectInstance ob, float cWidth, float cHeight) {
			
			float domainXScale = Sokoban2Domain.maxRoomXExtent(s) + 1f;
			float domainYScale = Sokoban2Domain.maxRoomYExtent(s) + 1f;
			
			if(maxX != -1){
				domainXScale = maxX;
				domainYScale = maxY;
			}
			
			//determine then normalized width
			float width = (1.0f / domainXScale) * cWidth;
			float height = (1.0f / domainYScale) * cHeight;
			
			int top = ob.getDiscValForAttribute(Sokoban2Domain.ATTTOP);
			int left = ob.getDiscValForAttribute(Sokoban2Domain.ATTLEFT);
			int bottom = ob.getDiscValForAttribute(Sokoban2Domain.ATTBOTTOM);
			int right = ob.getDiscValForAttribute(Sokoban2Domain.ATTRIGHT);
			
			Color rcol = colorForName(ob.getStringValForAttribute(Sokoban2Domain.ATTCOLOR));
			float [] hsb = new float[3];
			Color.RGBtoHSB(rcol.getRed(), rcol.getGreen(), rcol.getBlue(), hsb);
			hsb[1] = 0.4f;
			rcol = Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
			
			for(int i = left; i <= right; i++){
				for(int j = bottom; j <= top; j++){
					
					float rx = i*width;
					float ry = cHeight - height - j*height;
					
					if(i == left || i == right || j == bottom || j == top){
						if(Sokoban2Domain.doorContainingPoint(s, i, j) == null){
							g2.setColor(Color.black);
							g2.fill(new Rectangle2D.Float(rx, ry, width, height));
						}
					}
					else{
						g2.setColor(rcol);
						g2.fill(new Rectangle2D.Float(rx, ry, width, height));
					}
				}
			}
			
		}
		
	}
	
	
	public static class DoorPainter implements ObjectPainter{

		protected int maxX = -1;
		protected int maxY = -1;
		
		public DoorPainter(){
			
		}
		
		public DoorPainter(int maxX, int maxY){
			this.maxX = maxX;
			this.maxY = maxY;
		}
		
		@Override
		public void paintObject(Graphics2D g2, State s, ObjectInstance ob, float cWidth, float cHeight) {
			
			float domainXScale = Sokoban2Domain.maxRoomXExtent(s) + 1f;
			float domainYScale = Sokoban2Domain.maxRoomYExtent(s) + 1f;
			
			if(maxX != -1){
				domainXScale = maxX;
				domainYScale = maxY;
			}
			
			//determine then normalized width
			float width = (1.0f / domainXScale) * cWidth;
			float height = (1.0f / domainYScale) * cHeight;
			
			int top = ob.getDiscValForAttribute(Sokoban2Domain.ATTTOP);
			int left = ob.getDiscValForAttribute(Sokoban2Domain.ATTLEFT);
			int bottom = ob.getDiscValForAttribute(Sokoban2Domain.ATTBOTTOM);
			int right = ob.getDiscValForAttribute(Sokoban2Domain.ATTRIGHT);
			
			g2.setColor(Color.white);
			
			for(int i = left; i <= right; i++){
				for(int j = bottom; j <= top; j++){
					
					float rx = i*width;
					float ry = cHeight - height - j*height;
					g2.fill(new Rectangle2D.Float(rx, ry, width, height));
					
				}
			}
			
			
		}

	}
	
	
	
	public static class AgentPainter implements ObjectPainter{

		protected int maxX = -1;
		protected int maxY = -1;
		
		public AgentPainter(){
			
		}
		
		public AgentPainter(int maxX, int maxY){
			this.maxX = maxX;
			this.maxY = maxY;
		}
		
		@Override
		public void paintObject(Graphics2D g2, State s, ObjectInstance ob, float cWidth, float cHeight) {
			
			g2.setColor(Color.darkGray);
			
			float domainXScale = Sokoban2Domain.maxRoomXExtent(s) + 1f;
			float domainYScale = Sokoban2Domain.maxRoomYExtent(s) + 1f;
			
			if(maxX != -1){
				domainXScale = maxX;
				domainYScale = maxY;
			}
			
			//determine then normalized width
			float width = (1.0f / domainXScale) * cWidth;
			float height = (1.0f / domainYScale) * cHeight;
			
			int x = ob.getDiscValForAttribute(Sokoban2Domain.ATTX);
			int y = ob.getDiscValForAttribute(Sokoban2Domain.ATTY);
			
			float rx = x*width;
			float ry = cHeight - height - y*height;
			
			g2.fill(new Rectangle2D.Float(rx, ry, width, height));
			
		}
		
		
		
	}
	
	
	public static class AgentPainterWithImages implements ObjectPainter, ImageObserver{

		protected int maxX = -1;
		protected int maxY = -1;
		
		Map<String, BufferedImage>	dirToImage;
		
		public AgentPainterWithImages(String pathToImageDir){
			if(!pathToImageDir.endsWith("/")){
				pathToImageDir = pathToImageDir + "/";
			}
			
			dirToImage = new HashMap<String, BufferedImage>(4);
			try {
				dirToImage.put("north", ImageIO.read(new File(pathToImageDir + "robotNorth.png")));
				dirToImage.put("south", ImageIO.read(new File(pathToImageDir + "robotSouth.png")));
				dirToImage.put("east", ImageIO.read(new File(pathToImageDir + "robotEast.png")));
				dirToImage.put("west", ImageIO.read(new File(pathToImageDir + "robotWest.png")));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public AgentPainterWithImages(String pathToImageDir, int maxX, int maxY){
			this.maxX = maxX;
			this.maxY = maxY;
			
			if(!pathToImageDir.endsWith("/")){
				pathToImageDir = pathToImageDir + "/";
			}
			
			dirToImage = new HashMap<String, BufferedImage>(4);
			try {
				dirToImage.put("north", ImageIO.read(new File(pathToImageDir + "robotNorth.png")));
				dirToImage.put("south", ImageIO.read(new File(pathToImageDir + "robotSouth.png")));
				dirToImage.put("east", ImageIO.read(new File(pathToImageDir + "robotEast.png")));
				dirToImage.put("west", ImageIO.read(new File(pathToImageDir + "robotWest.png")));
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
		@Override
		public void paintObject(Graphics2D g2, State s, ObjectInstance ob, float cWidth, float cHeight) {
			
			
			float domainXScale = Sokoban2Domain.maxRoomXExtent(s) + 1f;
			float domainYScale = Sokoban2Domain.maxRoomYExtent(s) + 1f;
			
			if(maxX != -1){
				domainXScale = maxX;
				domainYScale = maxY;
			}
			
			//determine then normalized width
			float width = (1.0f / domainXScale) * cWidth;
			float height = (1.0f / domainYScale) * cHeight;
			
			int x = ob.getDiscValForAttribute(Sokoban2Domain.ATTX);
			int y = ob.getDiscValForAttribute(Sokoban2Domain.ATTY);
			
			float rx = x*width;
			float ry = cHeight - height - y*height;
			
			String dir = null;
			Attribute dirAtt = ob.getObjectClass().getAttribute(Sokoban2Domain.ATTDIR);
			if(dirAtt != null){
				dir = ob.getStringValForAttribute(Sokoban2Domain.ATTDIR);
			}
			else{
				dir = "south";
			}
			
			BufferedImage img = this.dirToImage.get(dir);
			g2.drawImage(img, (int)rx, (int)ry, (int)width, (int)height, this);
			
		}

		@Override
		public boolean imageUpdate(Image img, int infoflags, int x, int y,
				int width, int height) {
			return false;
		}
		
		
		
	}
	
	
	public static class BlockPainter implements ObjectPainter, ImageObserver{

		protected int maxX = -1;
		protected int maxY = -1;
		
		protected Map<String, BufferedImage> shapeAndColToImages;
		
		public BlockPainter(){
			shapeAndColToImages = new HashMap<String, BufferedImage>();
		}
		
		public BlockPainter(int maxX, int maxY){
			this.maxX = maxX;
			this.maxY = maxY;
			shapeAndColToImages = new HashMap<String, BufferedImage>();
		}
		
		public BlockPainter(String pathToImageDir){
			shapeAndColToImages = new HashMap<String, BufferedImage>();
			this.initImages(pathToImageDir);
		}
		
		public BlockPainter(int maxX, int maxY, String pathToImageDir){
			this.maxX = maxX;
			this.maxY = maxY;
			shapeAndColToImages = new HashMap<String, BufferedImage>();
			this.initImages(pathToImageDir);
			
		}
		
		protected void initImages(String pathToImageDir){
			if(!pathToImageDir.endsWith("/")){
				pathToImageDir = pathToImageDir + "/";
			}
			for(String shapeName : Sokoban2Domain.SHAPES){
				for(String colName : Sokoban2Domain.COLORS){
					String key = this.shapeKey(shapeName, colName);
					String path = pathToImageDir + shapeName + "/" + key + ".png";
					try {
						BufferedImage img = ImageIO.read(new File(path));
						this.shapeAndColToImages.put(key, img);
					} catch (IOException e) {
						e.printStackTrace();
						System.exit(-1);
					}
				}
			}
		}
		
		@Override
		public void paintObject(Graphics2D g2, State s, ObjectInstance ob, float cWidth, float cHeight) {
			
			float domainXScale = Sokoban2Domain.maxRoomXExtent(s) + 1f;
			float domainYScale = Sokoban2Domain.maxRoomYExtent(s) + 1f;
			
			if(maxX != -1){
				domainXScale = maxX;
				domainYScale = maxY;
			}
			
			//determine then normalized width
			float width = (1.0f / domainXScale) * cWidth;
			float height = (1.0f / domainYScale) * cHeight;
			
			int x = ob.getDiscValForAttribute(Sokoban2Domain.ATTX);
			int y = ob.getDiscValForAttribute(Sokoban2Domain.ATTY);
			
			float rx = x*width;
			float ry = cHeight - height - y*height;
			
			String colName = ob.getStringValForAttribute(Sokoban2Domain.ATTCOLOR);
			String shapeName = ob.getStringValForAttribute(Sokoban2Domain.ATTSHAPE);
			String key = this.shapeKey(shapeName, colName);
			BufferedImage img = this.shapeAndColToImages.get(key);
			
			if(img == null){
				Color col = colorForName(ob.getStringValForAttribute(Sokoban2Domain.ATTCOLOR)).darker();
				
				g2.setColor(col);
				g2.fill(new Rectangle2D.Float(rx, ry, width, height));
				
			}
			else{
				g2.drawImage(img, (int)rx, (int)ry, (int)width, (int)height, this);
			}
			
		}
		
		protected String shapeKey(String shape, String color){
			return shape + this.firstLetterCapped(color);
		}
		
		protected String firstLetterCapped(String input){
			return input.substring(0, 1).toUpperCase() + input.substring(1);
		}

		@Override
		public boolean imageUpdate(Image img, int infoflags, int x, int y,
				int width, int height) {
			return false;
		}
		
		
		
	}
	
	
	
	protected static Color colorForName(String colName){
		
		Color col = Color.darkGray; //default color
		
		Field field;
		try {
			field = Class.forName("java.awt.Color").getField(colName);
			col = (Color)field.get(null);

		} catch (Exception e) {
		}
		
		return col;
	}
	
}
