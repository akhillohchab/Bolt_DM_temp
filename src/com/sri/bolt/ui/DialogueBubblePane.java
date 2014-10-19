package com.sri.bolt.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;


public class DialogueBubblePane extends JPanel {
	private GridBagLayout g = new GridBagLayout();
	private GridBagConstraints c = new GridBagConstraints();
	private JLabel space = new JLabel("<html>&nbsp;</html>"); // fills up any space on the bottom
	private int N = 0;
	private JLabel lastBubble = null;
	private Speaker lastSpeaker;
   private Speaker baseSpeaker;

	private Color englishBorderFG = new Color(53, 125, 145);
	private Color systemBorderFG = new Color(110, 110, 110);
   private Color arabicBorderFG = new Color(113, 137, 63);
   //private Color leftBorderFG;
  // private Color rightBorderFG;
   private Color englishBG = new Color(162, 212, 226);
   private Color systemBG = new Color(255, 255, 255);
   private Color arabicBG = new Color(198, 217, 159);
	//private Color leftBG;
	//private Color rightBG;

	private Font font = new Font("Tahoma", Font.PLAIN, 18);
	
	private BubbleBorder englishBorder;
	private BubbleBorder arabicBorder;
   private BubbleBorder systemBorder;

	private final int minBubbleWidth = 40;

   public enum Speaker {
      ENGLISH,
      SYSTEM,
      ARABIC
   }
	
	public DialogueBubblePane(Speaker speaker) {
		setLayout(g);
		this.setBorder(new EmptyBorder(2, 2, 50, 2));

      baseSpeaker = speaker;
      if (speaker == Speaker.ENGLISH) {
         englishBorder = new BubbleBorder(englishBorderFG, BubbleBorder.Direction.BOTTOM_LEFT);
         arabicBorder = new BubbleBorder(arabicBorderFG, BubbleBorder.Direction.BOTTOM_RIGHT);
         systemBorder  = new BubbleBorder(systemBorderFG, BubbleBorder.Direction.BOTTOM_RIGHT);
      } else {
         englishBorder = new BubbleBorder(englishBorderFG, BubbleBorder.Direction.BOTTOM_LEFT);
         arabicBorder = new BubbleBorder(arabicBorderFG, BubbleBorder.Direction.BOTTOM_RIGHT);
         systemBorder  = new BubbleBorder(systemBorderFG, BubbleBorder.Direction.BOTTOM_LEFT);
      }

		c.weightx = 1.0;
		c.gridx = 0;
		c.weighty = 1.0;
		g.setConstraints(space, c);
		add(space);
	}
	
	public void clear() {
		for (Component c : getComponents()) {
			if (c == space) continue;
			remove(c);
		}

		lastBubble = null;
		N = 0;
		c.weighty = 1.0;
		c.gridy = 0;
		g.setConstraints(space, c);
		revalidate();
		repaint();
	}

	public void addLine(String line, Speaker speaker) {
		if (lastBubble != null && speaker == lastSpeaker) {
			lastBubble.setText(lastBubble.getText() + "<br>" + line);
		}
		else {
			JLabel f = new JLabel("<html>" + line) {
				@Override
				public Dimension getPreferredSize() {
					Container parent = getParent();
					Dimension p = super.getPreferredSize();
					if (p.width < minBubbleWidth)
						p = new Dimension(minBubbleWidth, p.height);
					if (parent != null & p.width > parent.getWidth())
						p = new Dimension(parent.getWidth()-10, p.height);
					return p;
				}
			};
			f.setOpaque(true);
			f.setFont(font);
         if (speaker == Speaker.ENGLISH) {
            f.setBorder(englishBorder);
            f.setBackground(englishBG);
            c.anchor = c.WEST;
         } else if (speaker == Speaker.ARABIC) {
            f.setBorder(arabicBorder);
            f.setBackground(arabicBG);
            c.anchor = c.EAST;
         } else if (speaker == Speaker.SYSTEM) {
            f.setBorder(systemBorder);
            f.setBackground(systemBG);
            c.anchor = (baseSpeaker == Speaker.ENGLISH ? c.EAST : c.WEST);
         }
//			c.fill = c.BOTH;
			c.weighty = 0.0;
			c.gridy = N++;
			g.setConstraints(f, c);
			add(f);
			
			c.weighty = 1.0;
			c.gridy++;
			g.setConstraints(space, c);
			
			lastBubble = f;
			lastSpeaker = speaker;
		}
		
		revalidate();
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				scrollRectToVisible(new Rectangle(0, getHeight(), 1, 1));
			}
		});
	}
	
	static public void main(String[] args) {
		JFrame testFrame = new JFrame();
		
		testFrame.setTitle("Dialogue Bubble test window");
		JPanel top = new JPanel();
		top.setLayout(new BorderLayout());
		final DialogueBubblePane dbp = new DialogueBubblePane(Speaker.ENGLISH);
		top.setPreferredSize(new Dimension(250,200));
		final JTextField leftInput = new JTextField();
		final JTextField rightInput = new JTextField();
		leftInput.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dbp.addLine(leftInput.getText(), Speaker.ENGLISH);
				leftInput.setText("");
			}
		});
		rightInput.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dbp.addLine(rightInput.getText(), Speaker.SYSTEM);
				rightInput.setText("");
			}
		});
		//leftInput.setBackground(dbp.leftBG);
		//rightInput.setBackground(dbp.rightBG);
		top.add(new JScrollPane(dbp, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
//		top.add(dbp);
		JPanel inputs = new JPanel();
		inputs.setLayout(new GridLayout(1,2));
		inputs.add(leftInput);
		inputs.add(rightInput);
		top.add(inputs, BorderLayout.SOUTH);
		JButton clear = new JButton("Clear");
		clear.setFocusable(false);
		clear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dbp.clear();
			}
		});
		top.add(clear, BorderLayout.NORTH);
		
		testFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		testFrame.add(top);
		testFrame.pack();
		testFrame.setVisible(true);

	}
}
