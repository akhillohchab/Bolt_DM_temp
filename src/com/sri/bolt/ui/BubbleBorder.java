package com.sri.bolt.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.border.AbstractBorder;


public class BubbleBorder extends AbstractBorder {
	private Color fg;
	private Direction dir;
	private int triangleWidth = 20;
	private int triangleHeight = 14;
	private Insets insets = new Insets(4, 8, 4+triangleHeight, 8);
	
	public enum Direction {
		NONE(), BOTTOM_LEFT, BOTTOM_RIGHT;
	}
	
	public BubbleBorder(Color fg, Direction dir) {
		this.fg = fg;
		this.dir = dir;
	}
	
	@Override 
	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
		Container parent = c.getParent();
		if (parent == null) return;
		Color bg = c.getBackground();
		
		Graphics2D g2 = (Graphics2D)g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		int r = height-1;
		r = 26;
		RoundRectangle2D round = new RoundRectangle2D.Float(x+1, y+1, width-3, height-triangleHeight, r, r);
		
		g2.setColor(parent.getBackground());
		Area corner = new Area(new Rectangle2D.Float(x, y, width, height));
		corner.subtract(new Area(round));
		g2.fill(corner);

		g2.setColor(fg);
		g2.setStroke(new BasicStroke(2.5f));
		g2.draw(round);
		
		if (dir != Direction.NONE) {
			int[] tx;
			if (dir == Direction.BOTTOM_LEFT) {
				int[] tmpx = {triangleWidth/3, 0, triangleWidth};
				tx = tmpx;
			}
			else {
				int[] tmpx = {width-triangleWidth/3, width, width-triangleWidth};
				tx = tmpx;
			}
			int[] ty = {height-triangleHeight-1, height-1, height-triangleHeight-1};
			int[] ty2 = {height-triangleHeight, height-1, height-triangleHeight};
			Polygon triangle = new Polygon(tx, ty, tx.length);
			g2.setColor(bg);
			g2.fillPolygon(triangle);
			g2.setColor(fg);
			g2.drawPolyline(tx, ty2, tx.length);
		}
		
		g2.dispose();
	}
	
	
	@Override 
	public Insets getBorderInsets(Component c) {
		return insets;
	}
	
	@Override 
	public Insets getBorderInsets(Component c, Insets insets) {
		insets.left = this.insets.left;
		insets.right = this.insets.right;
		insets.top = this.insets.top;
		insets.bottom= this.insets.bottom;
		return insets;
	}
}
