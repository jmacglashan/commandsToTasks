package domain.singleagent.sokoban;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.Field;

import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.visualizer.ObjectPainter;
import burlap.oomdp.visualizer.Visualizer;

/**
 * Javadoc test
 * @author Richard Adjogah
 *
 */
public class SokobanVisualizer {

	/**
	 * 
	 * @return the visualizer object 
	 */
	public static Visualizer getVisualizer(){

		Visualizer v = new Visualizer();


		v.addObjectClassPainter(SokobanDomain.ROOMCLASS, new RoomPainter());
		v.addObjectClassPainter(SokobanDomain.AGENTCLASS, new AgentPainter());
		v.addObjectClassPainter(SokobanDomain.BLOCKCLASS, new BlockPainter());

		return v;
	}

	//each invdividual object type should have its own painter classthat determines what color to draw and where
	static class RoomPainter implements ObjectPainter{


		@Override
		public void paintObject(Graphics2D g2, State s, ObjectInstance ob,
				float cWidth, float cHeight) {
			
			float domainXScale = (SokobanDomain.MAXX + 1) - SokobanDomain.MINX;
			float domainYScale = (SokobanDomain.MAXY + 1) - SokobanDomain.MINY;
			
			//determines which java Color to use from a String. Defaults to Dark Gray if the string isn't a valid color
			String color = ob.getStringValForAttribute(SokobanDomain.COLORATTNAME);
			
			Field field;
			try {
				field = Class.forName("java.awt.Color").getField(color);
				g2.setColor((Color)field.get(null));

			} catch (Exception e) {
				g2.setColor(Color.darkGray);
			}

			float topX = ob.getDiscValForAttribute(SokobanDomain.TOPXATTNAME);
			float bottomX = ob.getDiscValForAttribute(SokobanDomain.BOTTOMXATTNAME);
			float topY = ob.getDiscValForAttribute(SokobanDomain.TOPYATTNAME);
			float bottomY = ob.getDiscValForAttribute(SokobanDomain.BOTTOMYATTNAME);
			float width = (1.0f / domainXScale) * cWidth;
			float height = (1.0f / domainYScale) * cHeight;

			for(int i = SokobanDomain.MINX; i <= SokobanDomain.MAXX; i++){
				for(int j = SokobanDomain.MINY; j <= SokobanDomain.MAXY; j++){
					if(i <= bottomX && i >= topX && j >= bottomY && j <= topY){
						
						float rx = i*width;
						float ry = cHeight - height - j*height;

						g2.fill(new Rectangle2D.Float(rx, ry, width, height));
					}			
				}
			}
			
			g2.setColor(Color.black);

			for(int i = SokobanDomain.MINX; i <= SokobanDomain.MAXX; i++){
				for(int j = SokobanDomain.MINY; j <= SokobanDomain.MAXY; j++){
					if(SokobanDomain.MAP[i][j] == 1){

						float rx = i*width;
						float ry = cHeight - height - j*height;

						g2.fill(new Rectangle2D.Float(rx, ry, width, height));
					}
				}
			}
			
		}
	}


	static class AgentPainter implements ObjectPainter{


		@Override
		public void paintObject(Graphics2D g2, State s, ObjectInstance ob,
				float cWidth, float cHeight) {
			
			//make agent gray
			g2.setColor(Color.gray);

			float domainXScale = (SokobanDomain.MAXX + 1) - SokobanDomain.MINX;
			float domainYScale = (SokobanDomain.MAXY + 1) - SokobanDomain.MINY;

			//determine then normalized width
			float width = (1.0f / domainXScale) * cWidth;
			float height = (1.0f / domainYScale) * cHeight;

			float rx = ob.getDiscValForAttribute(SokobanDomain.XATTNAME)*width;
			float ry = cHeight - height - ob.getDiscValForAttribute(SokobanDomain.YATTNAME)*height;

			g2.fill(new Rectangle2D.Float(rx, ry, width, height));
			
		}
	}

	static class GoalPainter implements ObjectPainter{


		@Override
		public void paintObject(Graphics2D g2, State s, ObjectInstance ob,
				float cWidth, float cHeight) {
			
			g2.setColor(Color.blue);

			float domainXScale = (SokobanDomain.MAXX + 1) - SokobanDomain.MINX;
			float domainYScale = (SokobanDomain.MAXY + 1) - SokobanDomain.MINY;

			//determine then normalized width
			float width = (1.0f / domainXScale) * cWidth;
			float height = (1.0f / domainYScale) * cHeight;

			float rx = ob.getDiscValForAttribute(SokobanDomain.XATTNAME)*width;
			float ry = cHeight - height - ob.getDiscValForAttribute(SokobanDomain.YATTNAME)*height;

			g2.fill(new Rectangle2D.Float(rx, ry, width, height));
			
		}
	}

	static class BlockPainter implements ObjectPainter{


		@Override
		public void paintObject(Graphics2D g2, State s, ObjectInstance ob,
				float cWidth, float cHeight) {
			
			String color = ob.getStringValForAttribute(SokobanDomain.COLORATTNAME);
			
			Field field;
			try {
				field = Class.forName("java.awt.Color").getField(color);
				g2.setColor((Color)field.get(null));

			} catch (Exception e) {
				g2.setColor(Color.darkGray);
			}
			
			float domainXScale = (SokobanDomain.MAXX + 1) - SokobanDomain.MINX;
			float domainYScale = (SokobanDomain.MAXY + 1) - SokobanDomain.MINY;

			//determine then normalized width
			float width = (1.0f / domainXScale) * cWidth;
			float height = (1.0f / domainYScale) * cHeight;

			float rx = ob.getDiscValForAttribute(SokobanDomain.XATTNAME)*width;
			float ry = cHeight - height - ob.getDiscValForAttribute(SokobanDomain.YATTNAME)*height;

			g2.fill(new Rectangle2D.Float(rx, ry, width, height));
			
		}
	}
}
