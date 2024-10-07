package tclib.utils.petrinets.gui;

import java.awt.*;
import javax.swing.*;

import tclib.utils.petrinets.*;
import tclib.utils.petrinets.gui.dialog.*;

public class PNEditor extends JFrame
{
	JMenuBar menubar;                                   // the menubar
	JMenu file, help, Settings, Parallel, Sequentiell;  // menu panels
	JCheckBoxMenuItem ParMan, ParRan, SeqMan, SeqRan;   // menu checkboxes
	String InputFile, OutputFile;
	JFileChooser chooser;
	boolean alreadySaved;
	boolean runHolding;
	boolean demo;
	PNOptionsDialog ShowOp;
	JTextField StatusLine, StatusMode, StatusCount;
	PetriNet PNet;
	PetriNet OldPNet;
	PNCanvas Vis;
	PNAbout HelpAb;
	PNExecutor runStep;
	int StepCount;
	int runDelay;
	JPanel Status;
	JPanel top;
	JButton m_button[];
	
	public PNEditor()
	{
		super ("Petri Net Editor & Simulator");

		Toolkit tk;

		PNet = new PetriNet();
		StepCount = 0;
		runDelay = 100;
		runHolding = false;
		demo = false;
		
		// Create the menubar.  Tell the frame about it.
		menubar = new JMenuBar();
		chooser	= new JFileChooser ();
		alreadySaved = false;
		
		// Create the file menu.  Add two items to it.  Add to menubar.
		JMenuItem mnew, mload, msave, msaveas, mquit;
		
		file = new JMenu("File");
		file.add((mnew = new JMenuItem("New")));
		file.add((mload = new JMenuItem("Load...")));
		file.add((msave = new JMenuItem("Save")));
		file.add((msaveas = new JMenuItem("Save As...")));
		file.add((mquit = new JMenuItem("Quit")));
		menubar.add(file);
		
		// Create submenus for Setting menu
		ParMan = new JCheckBoxMenuItem("Manual");
		ParRan = new JCheckBoxMenuItem("Random");
		Parallel = new JMenu("Parallel");
		Parallel.add(ParRan);
		Parallel.add(ParMan);
		
		SeqMan = new JCheckBoxMenuItem("Manual");
		SeqRan = new JCheckBoxMenuItem("Random");
		Sequentiell = new JMenu("Sequential");
		Sequentiell.add(SeqRan);
		Sequentiell.add(SeqMan);
		SeqRan.setState(true);
				
		// Create the Settings menu
		JMenuItem mopts;

		Settings = new JMenu("Settings");
		Settings.add(Parallel);
		Settings.add(Sequentiell);
		Settings.addSeparator();
		Settings.add((mopts = new JMenuItem("Options")));
		menubar.add(Settings);
		
		// Create Help menu; add an item; add to menubar
		JMenuItem mabout;

		help = new JMenu("Help");
		help.add((mabout = new JMenuItem("About")));
		menubar.add(help);
		
		// Buttonbar
		tk = this.getToolkit();
		Image[] ups = new Image[13];
		Image[] downs = new Image[13];
		Image[] diss = new Image[13];
		
		ups[0] = tk.getImage(ClassLoader.getSystemResource ("tclib/utils/petrinets/gui/images/Release.GIF"));
		ups[1] = tk.getImage(ClassLoader.getSystemResource ("tclib/utils/petrinets/gui/images/AddNode.GIF"));
		ups[2] = tk.getImage(ClassLoader.getSystemResource ("tclib/utils/petrinets/gui/images/AddTransition.GIF"));
		ups[3] = tk.getImage(ClassLoader.getSystemResource ("tclib/utils/petrinets/gui/images/AddEdge.GIF"));
		ups[4] = tk.getImage(ClassLoader.getSystemResource ("tclib/utils/petrinets/gui/images/AddToken.GIF"));
		ups[5] = tk.getImage(ClassLoader.getSystemResource ("tclib/utils/petrinets/gui/images/SubToken.GIF"));
		ups[6] = tk.getImage(ClassLoader.getSystemResource ("tclib/utils/petrinets/gui/images/Edit.GIF"));
		ups[7] = tk.getImage(ClassLoader.getSystemResource ("tclib/utils/petrinets/gui/images/Delete.GIF"));
		ups[8] = tk.getImage(ClassLoader.getSystemResource ("tclib/utils/petrinets/gui/images/Save.GIF"));
		ups[9] = tk.getImage(ClassLoader.getSystemResource ("tclib/utils/petrinets/gui/images/Memorize.GIF"));
		ups[10] = tk.getImage(ClassLoader.getSystemResource ("tclib/utils/petrinets/gui/images/Memback.GIF"));
		ups[11] = tk.getImage(ClassLoader.getSystemResource ("tclib/utils/petrinets/gui/images/Step.GIF"));
		ups[12] = tk.getImage(ClassLoader.getSystemResource ("tclib/utils/petrinets/gui/images/StepSing.GIF"));
		downs[0] = tk.getImage(ClassLoader.getSystemResource ("tclib/utils/petrinets/gui/images/ReleaseSelected.GIF"));
		downs[1] = tk.getImage(ClassLoader.getSystemResource ("tclib/utils/petrinets/gui/images/AddNodeSelected.GIF"));
		downs[2] = tk.getImage(ClassLoader.getSystemResource ("tclib/utils/petrinets/gui/images/AddTransitionSelected.GIF"));
		downs[3] = tk.getImage(ClassLoader.getSystemResource ("tclib/utils/petrinets/gui/images/AddEdgeSelected.GIF"));
		downs[4] = tk.getImage(ClassLoader.getSystemResource ("tclib/utils/petrinets/gui/images/AddTokenSelected.GIF"));
		downs[5] = tk.getImage(ClassLoader.getSystemResource ("tclib/utils/petrinets/gui/images/SubTokenSelected.GIF"));
		downs[6] = tk.getImage(ClassLoader.getSystemResource ("tclib/utils/petrinets/gui/images/EditSelected.GIF"));
		downs[7] = tk.getImage(ClassLoader.getSystemResource ("tclib/utils/petrinets/gui/images/DeleteSelected.GIF"));
		downs[8] = tk.getImage(ClassLoader.getSystemResource ("tclib/utils/petrinets/gui/images/SaveSelected.GIF"));
		downs[9] = tk.getImage(ClassLoader.getSystemResource ("tclib/utils/petrinets/gui/images/MemorizeSelected.GIF"));
		downs[10] = tk.getImage(ClassLoader.getSystemResource ("tclib/utils/petrinets/gui/images/MembackSelected.GIF"));
		downs[11] = tk.getImage(ClassLoader.getSystemResource ("tclib/utils/petrinets/gui/images/StepSelected.GIF"));
		downs[12] = tk.getImage(ClassLoader.getSystemResource ("tclib/utils/petrinets/gui/images/StepSingSelected.GIF"));
		diss[0] = tk.getImage(ClassLoader.getSystemResource ("tclib/utils/petrinets/gui/images/ReleaseSelected.GIF"));
		diss[1] = tk.getImage(ClassLoader.getSystemResource ("tclib/utils/petrinets/gui/images/AddNodeSelected.GIF"));
		diss[2] = tk.getImage(ClassLoader.getSystemResource ("tclib/utils/petrinets/gui/images/AddTransitionSelected.GIF"));
		diss[3] = tk.getImage(ClassLoader.getSystemResource ("tclib/utils/petrinets/gui/images/AddEdgeSelected.GIF"));
		diss[4] = tk.getImage(ClassLoader.getSystemResource ("tclib/utils/petrinets/gui/images/AddTokenSelected.GIF"));
		diss[5] = tk.getImage(ClassLoader.getSystemResource ("tclib/utils/petrinets/gui/images/SubTokenSelected.GIF"));
		diss[6] = tk.getImage(ClassLoader.getSystemResource ("tclib/utils/petrinets/gui/images/EditSelected.GIF"));
		diss[7] = tk.getImage(ClassLoader.getSystemResource ("tclib/utils/petrinets/gui/images/DeleteSelected.GIF"));
		diss[8] = tk.getImage(ClassLoader.getSystemResource ("tclib/utils/petrinets/gui/images/SaveSelected.GIF"));
		diss[9] = tk.getImage(ClassLoader.getSystemResource ("tclib/utils/petrinets/gui/images/MemorizeSelected.GIF"));
		diss[10] = tk.getImage(ClassLoader.getSystemResource ("tclib/utils/petrinets/gui/images/MembackSelected.GIF"));
		diss[11] = tk.getImage(ClassLoader.getSystemResource ("tclib/utils/petrinets/gui/images/StepSelected.GIF"));
		diss[12] = tk.getImage(ClassLoader.getSystemResource ("tclib/utils/petrinets/gui/images/StepSingSelected.GIF"));
		
		m_button = new JButton[ups.length];
		top	= new JPanel ();
		top.setLayout (new GridLayout (1, m_button.length));
		for (int i = 0; i < m_button.length; i++)
		{
			m_button[i] = new JButton (new ImageIcon (ups[i]));
			m_button[i].setPressedIcon (new ImageIcon (downs[i]));
			m_button[i].setDisabledIcon (new ImageIcon (diss[i]));
			m_button[i].addActionListener(new java.awt.event.ActionListener() {
				public void  actionPerformed(java.awt.event.ActionEvent e) {
					buttonActionPerformed(e);
				}
			});		
			
			top.add (m_button[i]);
		}
		
		Dimension dimvis = new Dimension(1000,1000);
		Vis = new PNCanvas(PNet, dimvis, this);
		Vis.setBackground(Color.white);
		
		// Status
		Status = new JPanel();
		Status.setLayout(new FlowLayout());
		StatusLine = new JTextField("Everything OK!", 30);
		StatusLine.setEditable(false);
		StatusMode = new JTextField("SeqRan", 10);
		StatusMode.setEditable(false);
		StatusCount = new JTextField("0", 5);
		StatusCount.setEditable(false);
		Status.add(StatusLine);
		Status.add(StatusMode);
		Status.add(StatusCount);
		
		setBackground(Color.lightGray);
		
		setJMenuBar(menubar);
		getContentPane ().setLayout(new BorderLayout());
		getContentPane ().add(BorderLayout.NORTH, top);
		getContentPane ().add(BorderLayout.CENTER, Vis);
		getContentPane ().add(BorderLayout.SOUTH, Status);				
		
		mnew.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				mnewActionPerformed(e);
			}
		});		
		mload.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				mloadActionPerformed(e);
			}
		});		
		msave.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				msaveActionPerformed(e);
			}
		});		
		msaveas.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				msaveasActionPerformed(e);
			}
		});		
		mquit.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				mquitActionPerformed(e);
			}
		});		
		mabout.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				maboutActionPerformed(e);
			}
		});		
		mopts.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				moptsActionPerformed(e);
			}
		});		
		ParRan.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				parRanActionPerformed(e);
			}
		});		
		ParMan.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				parManActionPerformed(e);
			}
		});		
		SeqRan.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				seqRanActionPerformed(e);
			}
		});		
		SeqMan.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				seqManActionPerformed(e);
			}
		});		
		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent e) {
				thisWindowClosing(e);
			}
		});        
	}
	
	public static void main(String[] args) {
		PNEditor f = new PNEditor();
		f.setSize (700, 600);
		f.setVisible (true);;
	}	
	
	protected void thisWindowClosing(java.awt.event.WindowEvent e)
	{
		setVisible (false);
		dispose ();
		System.exit (0);
	}
	
	protected void mnewActionPerformed(java.awt.event.ActionEvent e) 
	{
		controlRunStep();
		PNet = new PetriNet();
		alreadySaved = false;
		Vis.setPN(PNet);
		Vis.repaint();
	}
	
	protected void mloadActionPerformed(java.awt.event.ActionEvent e) 
	{
		controlRunStep();
		
		StatusLine.setText("Insert file name to load PetriNet from..");

		int val = chooser.showOpenDialog (this);
		if (val == JFileChooser.APPROVE_OPTION)
		{
			InputFile = chooser.getSelectedFile().getPath();

			PNet		= new PetriNet ();
			PNet.fromFile (InputFile);

			Vis.setPN(PNet);
			Vis.repaint ();
			OutputFile = InputFile;
			alreadySaved = false;
			StatusLine.setText("PetriNet loaded.");
PetriNetWindow win = new PetriNetWindow (null);
win.setPetriNet(PNet);
		}
	}
	
	protected void msaveActionPerformed(java.awt.event.ActionEvent e) 
	{
		controlRunStep();
		
		if (!alreadySaved) 
		{
			StatusLine.setText("Insert file name to save PetriNet to...");
			int val = chooser.showSaveDialog (this);
			if (val == JFileChooser.APPROVE_OPTION)
			{
				OutputFile = chooser.getSelectedFile().getPath();

				PNet.toFile(OutputFile);
				alreadySaved = true;
				StatusLine.setText("Petri Net saved.");
			}
		}
		else
		{
			PNet.toFile(OutputFile);
			StatusLine.setText("Petri Net saved.");
		}
	}
	
	protected void msaveasActionPerformed(java.awt.event.ActionEvent e) 
	{
		controlRunStep();

		StatusLine.setText("Insert file name to save PetriNet to...");
		int val = chooser.showSaveDialog (this);
		if (val == JFileChooser.APPROVE_OPTION)
		{
			OutputFile = chooser.getSelectedFile().getPath();

			PNet.toFile(OutputFile);
			alreadySaved = true;
			StatusLine.setText("Petri Net saved.");
		}
	}
	
	protected void mquitActionPerformed(java.awt.event.ActionEvent e) 
	{
		controlRunStep();
		
		this.dispose();
		System.exit(0);
	}
	
	protected void maboutActionPerformed(java.awt.event.ActionEvent e) 
	{
		if (HelpAb == null) {
			HelpAb = new PNAbout(this);
		}
		HelpAb.setVisible(true);
	}

	protected void moptsActionPerformed(java.awt.event.ActionEvent e) 
	{
		if (ShowOp == null) {
			ShowOp = new PNOptionsDialog(this);
		}
		ShowOp.setDelay(runDelay);
		ShowOp.setDemo(demo);
		controlRunStep();
		ShowOp.setVisible(true);
	}

	protected void parManActionPerformed(java.awt.event.ActionEvent e) 
	{
		controlRunStep();
		StatusLine.setText("Step mode set to Parallel Manual.");
		StatusMode.setText("ParMan");
		ParRan.setState(false);
		SeqRan.setState(false);
		SeqMan.setState(false);
	}

	protected void parRanActionPerformed(java.awt.event.ActionEvent e) 
	{
		controlRunStep();
		StatusLine.setText("Step mode set to Parallel Random.");
		StatusMode.setText("ParRan");
		ParMan.setState(false);
		SeqRan.setState(false);
		SeqMan.setState(false);
	}

	protected void seqManActionPerformed(java.awt.event.ActionEvent e) 
	{
		controlRunStep();
		StatusLine.setText("Step mode set to Sequential Manual.");
		StatusMode.setText("SeqMan");
		SeqRan.setState(false);
		ParRan.setState(false);
		ParMan.setState(false);
	}

	protected void seqRanActionPerformed(java.awt.event.ActionEvent e) 
	{
		controlRunStep();
		StatusLine.setText("Step mode set to Sequential Random.");
		StatusMode.setText("SeqRan");
		SeqMan.setState(false);
		ParRan.setState(false);
		ParMan.setState(false);
	}

	protected void buttonActionPerformed (java.awt.event.ActionEvent event)
	{
		if (event.getSource () == (Component)m_button[0]) {
			if (Vis.setMode(PNCanvas.MODE_DRAG, 0)) {
				enableAll();
				// highlight(0);
				StatusLine.setText("No draw mode selected.");
			}
			return;
		}
		else if (event.getSource () == (Component)m_button[1]) {
			if (Vis.setMode(PNCanvas.MODE_NODE, 0)) {
				enableAll();
				setEnabled(1, false);
				StatusLine.setText("Draw mode set to Add Node.");
			}
			return;
		}
		else if (event.getSource () == (Component)m_button[2]) {
			if (Vis.setMode(PNCanvas.MODE_TRANS, 0)) {
				enableAll();
				setEnabled(2, false);
				StatusLine.setText("Draw mode set to Add Transition.");
			}
			return;
		}
		else if (event.getSource () == (Component)m_button[3]) {
			if (Vis.setMode(PNCanvas.MODE_EDGE, 0)) {
				enableAll();
				setEnabled(3, false);
				StatusLine.setText("Draw mode set to Add Edge.");
			}
			return;
		}
		else if (event.getSource () == (Component)m_button[4]) {
			if (Vis.setMode(PNCanvas.MODE_ATOKEN, 0)) {
				enableAll();
				setEnabled(4, false);
				StatusLine.setText("Draw mode set to Add Token.");
			}
			return;
		}
		else if (event.getSource () == (Component)m_button[5]) {
			if (Vis.setMode(PNCanvas.MODE_STOKEN, 0)) {
				enableAll();
				setEnabled(5, false);
				StatusLine.setText("Draw mode set to Substract Token.");
			}
			return;
		}
		else if (event.getSource () == (Component)m_button[6]) {
			if (Vis.setMode(PNCanvas.MODE_EDIT, 0)) {
				enableAll();
				setEnabled(6, false);
				StatusLine.setText("Draw mode set to Edit.");
			}
			return;
		}
		else if (event.getSource () == (Component)m_button[7]) {
			if (Vis.setMode(PNCanvas.MODE_DELETE, 0)) {
				enableAll();
				setEnabled(7, false);
				StatusLine.setText("Draw mode set to Delete.");
			}
			return;
		}
		else if (event.getSource () == (Component)m_button[8]) {
			controlRunStep();
			if (!alreadySaved) 
			{
				StatusLine.setText("Insert file name to save PetriNet to...");
				int val = chooser.showSaveDialog (this);
				if (val == JFileChooser.APPROVE_OPTION)
				{
					OutputFile = chooser.getSelectedFile().getPath();

					PNet.toFile(OutputFile);
					alreadySaved = true;
					StatusLine.setText("Petri Net saved.");
				}
			}
			else
			{
				PNet.toFile(OutputFile);
				StatusLine.setText("Petri Net saved.");
			}
			setEnabled(8, true);
			return;
		}
		else if (event.getSource () == (Component)m_button[9]) {
			controlRunStep();
			if (PNet != null) {
				OldPNet = (PetriNet)PNet.clone();
				StatusLine.setText("PetriNet memorized.");
			}
			return;
		}
		else if (event.getSource () == (Component)m_button[10]) {
			if (OldPNet != null) {
				controlRunStep();
				PNet = (PetriNet)OldPNet.clone();
				Vis.setPN(PNet);
				Vis.repaint();
				StatusLine.setText("Memorized PetriNet loaded.");
			} else StatusLine.setText("Sorry, no PetriNet was memorized.");
			return;
		}
		else if (event.getSource () == (Component)m_button[11]) {
			if (ParMan.getState()) {
				if (runStep == null) {
					runStep = new PNExecutor(PNet, this, Vis, PNExecutor.PARMAN, false);
					runSeqRan(11);
				} else {
					if (runStep.isAlive()) {
						if (runHolding) {
							runHolding = false;
							runStep.disableSingleStep();
							runButtonHighlight(11);
							runStep.resume();
						} else {
							controlRunStep();
						}
					} else {
						runStep = new PNExecutor(PNet, this, Vis, PNExecutor.PARMAN, false);
						runSeqRan(11);
					}
				}
			}
			if (ParRan.getState()) {
				if (runStep == null) {
					runStep = new PNExecutor(PNet, this, Vis, PNExecutor.PARRAN, false);
					runSeqRan(11);
				} else {
					if (runStep.isAlive()) {
						if (runHolding) {
							runHolding = false;
							runStep.disableSingleStep();
							runButtonHighlight(11);
							runStep.resume();
						} else {
							controlRunStep();
						}
					} else {
						runStep = new PNExecutor(PNet, this, Vis, PNExecutor.PARRAN, false);
						runSeqRan(11);
					}
				}
			}
			if (SeqMan.getState()) {
				if (runStep == null) {
					runStep = new PNExecutor(PNet, this, Vis, PNExecutor.SEQMAN, false);
					runSeqRan(11);
				} else {
					if (runStep.isAlive()) {
						if (runHolding) {
							runHolding = false;
							runStep.disableSingleStep();
							runButtonHighlight(11);
							runStep.resume();
						} else {
							controlRunStep();
						}
					} else {
						runStep = new PNExecutor(PNet, this, Vis, PNExecutor.SEQMAN, false);
						runSeqRan(11);
					}
				}
			}
			if (SeqRan.getState()) {
				if (runStep == null) {
					runStep = new PNExecutor(PNet, this, Vis, PNExecutor.SEQRAN, false);
					runSeqRan(11);
				} else {
					if (runStep.isAlive()) {
						if (runHolding) {
							runHolding = false;
							runStep.disableSingleStep();
							runButtonHighlight(11);
							runStep.resume();
						} else {
							controlRunStep();
						}
					} else {
						runStep = new PNExecutor(PNet, this, Vis, PNExecutor.SEQRAN, false);
						runSeqRan(11);
					}
				}
			}
			return;
		}
		else if (event.getSource () == (Component)m_button[12]) {
			if (ParMan.getState()) {
				if (runStep == null) {
					runStep = new PNExecutor(PNet, this, Vis, PNExecutor.PARMAN, true);
					runSeqRan(12);
				} else {
					if (runStep.isAlive()) {
						if (runHolding) {
							runHolding = false;
							runButtonHighlight(12);
							runStep.resume();
						} else {
							controlRunStep();
						}
					} else {
						runStep = new PNExecutor(PNet, this, Vis, PNExecutor.PARMAN, true);
						runSeqRan(12);
					}
				}
			}
			if (ParRan.getState()) {
				if (runStep == null) {
					runStep = new PNExecutor(PNet, this, Vis, PNExecutor.PARRAN, true);
					runSeqRan(12);
				} else {
					if (runStep.isAlive()) {
						if (runHolding) {
							runHolding = false;
							runButtonHighlight(12);
							runStep.resume();
						} else {
							controlRunStep();
						}
					} else {
						runStep = new PNExecutor(PNet, this, Vis, PNExecutor.PARRAN, true);
						runSeqRan(12);
					}
				}
			}
			if (SeqMan.getState()) {
				if (runStep == null) {
					runStep = new PNExecutor(PNet, this, Vis, PNExecutor.SEQMAN, true);
					runSeqRan(12);
				} else {
					if (runStep.isAlive()) {
						if (runHolding) {
							runHolding = false;
							runButtonHighlight(12);
							runStep.resume();
						} else {
							controlRunStep();
						}
					} else {
						runStep = new PNExecutor(PNet, this, Vis, PNExecutor.SEQMAN, true);
						runSeqRan(12);
					}
				}
			}
			if (SeqRan.getState()) {
				if (runStep == null) {
					runStep = new PNExecutor(PNet, this, Vis, PNExecutor.SEQRAN, true);
					runSeqRan(12);
				} else {
					if (runStep.isAlive()) {
						if (runHolding) {
							runHolding = false;
							runButtonHighlight(12);
							runStep.resume();
						} else {
							controlRunStep();
						}
					} else {
						runStep = new PNExecutor(PNet, this, Vis, PNExecutor.SEQRAN, true);
						runSeqRan(12);
					}
				}
			}
			return;
		}
	}
	
	public void enableAll() {
		for (int i = 0; i < m_button.length; i++) {
			m_button[i].setEnabled (true);
		}
	}
	
	public void setEnabled (int i, boolean enabled) {
		m_button[i].setEnabled(enabled);
	}
				
	public void setAlreadySaved(boolean value) {
		alreadySaved = value;
	}
	
	public void setCount(int c) {
		StepCount = c;
		StatusCount.setText(Integer.toString(StepCount));
		Status.repaint();
	}
	
	public void runIsHolding() {
		runButtonsUnHighlight();
		runHolding = true;
	}
	
	public int getDelay() {
		return runDelay;
	}
	
	public void setDelay(int d) {
		runDelay = d;
	}
	
	public void setDemo(boolean d) {
		demo = d;
		if (runStep != null) {
			runStep.setDemo(demo);
		}
	}
	
	public boolean getDemo() {
		return demo;
	}
	
	public void setStatus(String s) {
		StatusLine.setText(s);
	}
	
	public PNExecutor getRunStep() {
		return runStep;
	}
	
	void controlRunStep() {
		if (runStep != null) {
			if (runStep.isAlive()) {
				runStep.restoreVisMode();
				runStep.stop();
				runHolding = false;                             //new 27.10.97 jw
				runButtonsUnHighlight();
				StatusLine.setText("Petri Net stopped.");
			}
		}
	}
	
	void runSeqRan(int i) {
		runButtonHighlight(i);
		StatusLine.setText("Petri Net is running...");
		runStep.start();
	}
	
	public void runButtonsUnHighlight()
	{
		m_button[11].setSelected (false);
		m_button[12].setSelected (false);
	}
	
	void runButtonHighlight(int i)
	{
		if (i == 11)
		{
			m_button[11].setSelected (true);
			m_button[12].setSelected (false);
		}
		else
		{
			m_button[11].setSelected (false);
			m_button[12].setSelected (true);
		}
	}
}




