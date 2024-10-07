package tc.shared.world.gui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.StringTokenizer;

import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileFilter;

import tc.fleet.VehicleDesc;
import tc.gui.visualization.World2D;
import tc.shared.world.World;
import wucore.utils.dxf.DXFFile;
import wucore.utils.dxf.DoubleFormat;
import wucore.utils.dxf.entities.LineDxf;
import wucore.utils.geom.Point2;
import wucore.widgets.Component2D;
/*
 * Created on 06-jun-2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

/**
 * @author Jose
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class WorldDxfWindow extends JFrame {

	private javax.swing.JPanel jContentPane = null;

	private javax.swing.JMenuBar jMenuMain = null;
	private javax.swing.JMenu jMenuFile = null;
	private javax.swing.JMenu jMenuHelp = null;
	private javax.swing.JMenuItem jMenuAbout = null;
	private javax.swing.JMenuItem jMenuExit = null;
	//Converter conv;
	String FILE_TEMP = "./preview.world";
	String lastpath = null;
	World map;
	World2D world;
	Component2D mapdraw;
	private JMenuItem jMenuItem = null;
	private JMenuItem jMenuItem1 = null;
	private JMenuItem jMenuItem2 = null;
	private JMenuItem jMenuItem3 = null;
	private JMenu jMenuOpen = null;
	private JMenu jMenuSave = null;
	private JMenuItem jMenuClose = null;
	private JPanel jPanel = null;
	private JPanel jPanel1 = null;  //  @jve:decl-index=0:visual-constraint="488,59"
	private JLabel jLabel = null;
	private JPanel jPanel2 = null;
	
	private JMenuItem jMenuPreview = null;
	private JPanel jContentPane1 = null;
	private JFrame jFramePreview = null;  //  @jve:decl-index=0:visual-constraint="652,40"
	private JMenuBar jJMenuBar = null;
	private JMenu jMenu = null;
	private JMenuItem jMenuItem4 = null;
	private JMenuItem jMenuItem5 = null;
	private JMenuItem jMenuItem6 = null;
	private JScrollPane jScrollPane = null;
	private JTextArea jTextArea = null;
	private JMenuItem jMenuHelp1 = null;
	private JMenu jMenuIcon = null;
	private JMenuItem jMenuIconDxf = null;
	private JMenuItem jMenuIconRob = null;
	/**
	 * This method initializes jMenuItem	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */    
	private JMenuItem getJMenuItem() {
		if (jMenuItem == null) {
			jMenuItem = new JMenuItem();
			jMenuItem.setText("World (ThinkingCap)");
			jMenuItem.addActionListener(new java.awt.event.ActionListener() { 
				public void actionPerformed(java.awt.event.ActionEvent e) {    
					String fileworld;
					JFileChooser f = new JFileChooser();
					f.setFileFilter(new FileFilter() {
						public boolean accept(File f) {
							if (f.isDirectory())
								return true;
							if (f.getName().toLowerCase().endsWith(".world"))
								return true;
							return false;
						}
						public String getDescription() {
							return "World Map (*.world)";
						}
					});
					if(lastpath==null)
					    f.setCurrentDirectory(new File("./"));
					else
					    f.setCurrentDirectory(new File(lastpath));
					f.setDialogTitle("Load World file");
					if (f.showOpenDialog(jContentPane)
						!= JFileChooser.APPROVE_OPTION)
						return;
					fileworld = f.getSelectedFile().getAbsolutePath();
					lastpath = f.getSelectedFile().getParent();					
					try {
						map = new World(fileworld);
					} catch (Exception e1) {
						JOptionPane.showMessageDialog(null,"Error opening file "+fileworld,"Error",JOptionPane.WARNING_MESSAGE);
						e1.printStackTrace();
					}
					//getDraw();
					updaterobot();
				}
			});
		}
		return jMenuItem;
	}
	/**
	 * This method initializes jMenuItem1	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */    
	private JMenuItem getJMenuItem1() {
		if (jMenuItem1 == null) {
			jMenuItem1 = new JMenuItem();
			jMenuItem1.setText("Dxf (AutoCad)");
			jMenuItem1.addActionListener(new java.awt.event.ActionListener() { 
				public void actionPerformed(java.awt.event.ActionEvent e) {    
					String filedxf;
					JFileChooser f = new JFileChooser();
					f.setFileFilter(new FileFilter() {
						public boolean accept(File f) {
							if (f.isDirectory())
								return true;
							if (f.getName().toLowerCase().endsWith(".dxf"))
								return true;
							return false;
						}
						public String getDescription() {
							return "Dxf Map (*.dxf)";
						}
					});
					if(lastpath==null)
					    f.setCurrentDirectory(new File("./"));
					else
					    f.setCurrentDirectory(new File(lastpath));
					f.setDialogTitle("Load Dxf file");
					if (f.showOpenDialog(jContentPane)
						!= JFileChooser.APPROVE_OPTION)
						return;
					filedxf = f.getSelectedFile().getAbsolutePath();
					lastpath = f.getSelectedFile().getParent();
					try {
						map = new World();
						map.fromDxfFile(filedxf);
					} catch (Exception e1) {
						e1.printStackTrace();
						JOptionPane.showMessageDialog(null,"Error opening file "+filedxf,"Error",JOptionPane.WARNING_MESSAGE);
					}
					//getDraw(); 
					updaterobot();
				}
			});
		}
		return jMenuItem1;
	}
	/**
	 * This method initializes jMenuItem2	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */    
	private JMenuItem getJMenuItem2() {
		if (jMenuItem2 == null) {
			jMenuItem2 = new JMenuItem();
			jMenuItem2.setText("World file");
			jMenuItem2.addActionListener(new java.awt.event.ActionListener() { 
				public void actionPerformed(java.awt.event.ActionEvent e) {    
					String fileworld;
					JFileChooser f = new JFileChooser();
					f.setFileFilter(new FileFilter() {
						public boolean accept(File f) {
							if (f.isDirectory())
								return true;
							if (f.getName().toLowerCase().endsWith(".world"))
								return true;
							return false;
						}
						public String getDescription() {
							return "World Map (*.world)";
						}
					});
					if(lastpath==null)
					    f.setCurrentDirectory(new File("./"));
					else
					    f.setCurrentDirectory(new File(lastpath));
					f.setDialogTitle("Save World file");
					if (f.showSaveDialog(jContentPane)
						!= JFileChooser.APPROVE_OPTION)
						return;
					fileworld = f.getSelectedFile().getAbsolutePath();
					lastpath = f.getSelectedFile().getParent();
					if (f.getSelectedFile().exists()) 
						if (JOptionPane.OK_OPTION
							!= JOptionPane.showConfirmDialog(null,"The file '"+ fileworld +"' already exists. Do you want to replace existing file?","Question", JOptionPane.WARNING_MESSAGE)) {
							return;
						}
					try {
						map.toFile(fileworld);
					} catch (Exception e1) {
						JOptionPane.showMessageDialog(null,"Error writing file "+fileworld,"Error",JOptionPane.WARNING_MESSAGE);
						e1.printStackTrace();
					}
					updaterobot();
				}
			});
		}
		return jMenuItem2;
	}
	/**
	 * This method initializes jMenuItem3	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */    
	private JMenuItem getJMenuItem3() {
		if (jMenuItem3 == null) {
			jMenuItem3 = new JMenuItem();
			jMenuItem3.setText("Dxf file");
			jMenuItem3.addActionListener(new java.awt.event.ActionListener() { 
				public void actionPerformed(java.awt.event.ActionEvent e) {    
					String filedxf;
					JFileChooser f = new JFileChooser();
					f.setFileFilter(new FileFilter() {
						public boolean accept(File f) {
							if (f.isDirectory())
								return true;
							if (f.getName().toLowerCase().endsWith(".dxf"))
								return true;
							return false;
						}
						public String getDescription() {
							return "Dxf Map (*.dxf)";
						}
					});
					if(lastpath==null)
					    f.setCurrentDirectory(new File("./"));
					else
					    f.setCurrentDirectory(new File(lastpath));
					f.setDialogTitle("Save Dxf file");
					if (f.showSaveDialog(jContentPane)
						!= JFileChooser.APPROVE_OPTION)
						return;
					filedxf = f.getSelectedFile().getAbsolutePath();
					lastpath = f.getSelectedFile().getParent();
					if (f.getSelectedFile().exists()) 
						if (JOptionPane.OK_OPTION
							!= JOptionPane.showConfirmDialog(null,"The file '"+ filedxf +"' already exists. Do you want to replace existing file?","Question", JOptionPane.WARNING_MESSAGE)) {
							return;
						}
					try {
						map.toDxfFile(filedxf);
					} catch (Exception e1) {
						JOptionPane.showMessageDialog(null,"Error writing file "+filedxf,"Error",JOptionPane.WARNING_MESSAGE);
						e1.printStackTrace();
					}
					updaterobot();
				}
			});
		}
		return jMenuItem3;
	}
	/**
	 * This method initializes jMenuOpen	
	 * 	
	 * @return javax.swing.JMenu	
	 */    
	private JMenu getJMenuOpen() {
		if (jMenuOpen == null) {
			jMenuOpen = new JMenu();
			jMenuOpen.setText("Open File");
			jMenuOpen.add(getJMenuItem());
			jMenuOpen.add(getJMenuItem1());
		}
		return jMenuOpen;
	}
	/**
	 * This method initializes jMenuSave	
	 * 	
	 * @return javax.swing.JMenu	
	 */    
	private JMenu getJMenuSave() {
		if (jMenuSave == null) {
			jMenuSave = new JMenu();
			jMenuSave.setText("Save File");
			jMenuSave.add(getJMenuItem2());
			jMenuSave.add(getJMenuItem3());
			//jMenuSave.add(getJMenuTopol());
		}
		return jMenuSave;
	}
	/**
	 * This method initializes jMenuClose	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */    
	private JMenuItem getJMenuClose() {
		if (jMenuClose == null) {
			jMenuClose = new JMenuItem();
			jMenuClose.setText("Close");
			jMenuClose.addActionListener(new java.awt.event.ActionListener() { 
				public void actionPerformed(java.awt.event.ActionEvent e) {    
					map = null;
					//getDraw();
					updaterobot();
					world.resetModel();
					repaint();
				}
			});
		}
		return jMenuClose;
	}
	/**
	 * This method initializes jPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getJPanel() {
		if (jPanel == null) {
			jPanel = new JPanel();
			jPanel.setLayout(new BorderLayout());
			jPanel.add(getJPanel2(), java.awt.BorderLayout.CENTER);
			jPanel.add(getJPanel1(), java.awt.BorderLayout.SOUTH);
		}
		return jPanel;
	}
	/**
	 * This method initializes jPanel1	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getJPanel1() {
		if (jPanel1 == null) {
			GridLayout gridLayout2 = new GridLayout();
			jPanel1 = new JPanel();
			jLabel = new JLabel();
			jPanel1.setLayout(gridLayout2);
			jLabel.setText("Cursor X: 0.0 Cursor Y: 0.0 (m)");
			jLabel.setPreferredSize(new java.awt.Dimension(50,16));
			gridLayout2.setRows(1);
			jPanel1.add(jLabel, null);
		}
		return jPanel1;
	}
	/**
	 * This method initializes jPanel2	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getJPanel2() {
		if (jPanel2 == null) {
			GridLayout gridLayout1 = new GridLayout();
			jPanel2 = new JPanel();
			jPanel2.setLayout(gridLayout1);
			gridLayout1.setRows(1);
			jPanel2.add(getMapdraw(), null);
		}
		return jPanel2;
	}
	/**
	 * This method initializes jMenuPreview	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */    
	private JMenuItem getJMenuPreview() {
		if (jMenuPreview == null) {
			jMenuPreview = new JMenuItem();
			jMenuPreview.setText("Preview File");
			jMenuPreview.addActionListener(new java.awt.event.ActionListener() { 
				public void actionPerformed(java.awt.event.ActionEvent e) {    
					try{
						getJFramePreview().setVisible(true);
					}catch(Exception e1){
					}
				}
			});
		}
		return jMenuPreview;
	}
	
	/**
	 * This method initializes jContentPane1	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getJContentPane1() {
		if (jContentPane1 == null) {
			jContentPane1 = new JPanel();
			jContentPane1.setLayout(new BorderLayout());
			jContentPane1.add(getJScrollPane(), java.awt.BorderLayout.CENTER);
		}
		return jContentPane1;
	}
	/**
	 * This method initializes jFramePreview	
	 * 	
	 * @return javax.swing.JFrame	
	 */    
	private JFrame getJFramePreview() {
		if (jFramePreview == null) {
			jFramePreview = new JFrame();
			jFramePreview.setContentPane(getJContentPane1());
			jFramePreview.setSize(600, 400);
			jFramePreview.setTitle("Preview File");
			jFramePreview.setJMenuBar(getJJMenuBar());
		}
		refrestPreview();
		
		return jFramePreview;
	}

	private void refrestPreview() {
		try{
			map.toFile(FILE_TEMP);
			BufferedReader br = new BufferedReader(new FileReader(FILE_TEMP));
			String linea;
			String buffer = "";
			while((linea=br.readLine())!=null)
				buffer+=linea+"\n";
			br.close();
			getJTextArea().setText(buffer);
		}catch(Exception e){
		}
	}
	
	private void updateWorld() {
		try{
			BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_TEMP));
			bw.write(getJTextArea().getText());
			bw.close();
			map = new World(FILE_TEMP);
			updaterobot();
		}catch(Exception e){

		    //e.printStackTrace();

		}
	}
	
	/**
	 * This method initializes jJMenuBar	
	 * 	
	 * @return javax.swing.JMenuBar	
	 */    
	private JMenuBar getJJMenuBar() {
		if (jJMenuBar == null) {
			jJMenuBar = new JMenuBar();
			jJMenuBar.add(getJMenu());
		}
		return jJMenuBar;
	}
	/**
	 * This method initializes jMenu	
	 * 	
	 * @return javax.swing.JMenu	
	 */    
	private JMenu getJMenu() {
		if (jMenu == null) {
			jMenu = new JMenu();
			jMenu.setText("Options");
			jMenu.add(getJMenuItem4());
			jMenu.add(getJMenuItem5());
			jMenu.add(getJMenuItem6());
		}
		return jMenu;
	}
	/**
	 * This method initializes jMenuItem4	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */    
	private JMenuItem getJMenuItem4() {
		if (jMenuItem4 == null) {
			jMenuItem4 = new JMenuItem();
			jMenuItem4.setText("Update World");
			jMenuItem4.addActionListener(new java.awt.event.ActionListener() { 
				public void actionPerformed(java.awt.event.ActionEvent e) {    
					updateWorld();
				}
			});
		}
		return jMenuItem4;
	}
	/**
	 * This method initializes jMenuItem5	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */    
	private JMenuItem getJMenuItem5() {
		if (jMenuItem5 == null) {
			jMenuItem5 = new JMenuItem();
			jMenuItem5.setText("Restart");
			jMenuItem5.addActionListener(new java.awt.event.ActionListener() { 
				public void actionPerformed(java.awt.event.ActionEvent e) {    
					refrestPreview();
				}
			});
		}
		return jMenuItem5;
	}
	/**
	 * This method initializes jMenuItem6	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */    
	private JMenuItem getJMenuItem6() {
		if (jMenuItem6 == null) {
			jMenuItem6 = new JMenuItem();
			jMenuItem6.setText("Close");
			jMenuItem6.addActionListener(new java.awt.event.ActionListener() { 
				public void actionPerformed(java.awt.event.ActionEvent e) {    
					closePreview();
				}
			});
		}
		return jMenuItem6;
	}

	protected void closePreview() {
		getJFramePreview().setVisible(false);
	}
	
	/**
	 * This method initializes jScrollPane	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */    
	private JScrollPane getJScrollPane() {
		if (jScrollPane == null) {
			jScrollPane = new JScrollPane();
			jScrollPane.setViewportView(getJTextArea());
		}
		return jScrollPane;
	}
	/**
	 * This method initializes jTextArea	
	 * 	
	 * @return javax.swing.JTextArea	
	 */    
	private JTextArea getJTextArea() {
		if (jTextArea == null) {
			jTextArea = new JTextArea();
			jTextArea.setFont(new java.awt.Font("DialogInput", java.awt.Font.PLAIN, 12));
		}
		return jTextArea;
	}
	/**
	 * This method initializes jMenuHelp1	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */    
	private JMenuItem getJMenuHelp1() {
		if (jMenuHelp1 == null) {
			jMenuHelp1 = new JMenuItem();
			jMenuHelp1.setText("Help");
			jMenuHelp1.addActionListener(new java.awt.event.ActionListener() { 
					public void actionPerformed(java.awt.event.ActionEvent e) {    
						JFrame helpFrame = new JFrame("Help");
						URL s = getClass().getResource("/tc/navigation/world/gui/help.html");
						try{
							JPanel panel = new JPanel();
							JScrollPane scrollpane = new JScrollPane();
							JEditorPane editpane = new JEditorPane(s);
							editpane.setEditable(false);
							panel.setLayout(new BorderLayout());
							scrollpane.setViewportView(editpane);
							panel.add(scrollpane, java.awt.BorderLayout.CENTER);
							helpFrame.setContentPane(panel);
						}catch(Exception e1){e1.printStackTrace();}
						helpFrame.setSize(500,600);
						helpFrame.setVisible(true);
					}
				});
		}
		return jMenuHelp1;
	}
	/**
	 * This method initializes jMenu1	
	 * 	
	 * @return javax.swing.JMenu	
	 */    
	private JMenu getJMenuIcon() {
		if (jMenuIcon == null) {
			jMenuIcon = new JMenu();
			jMenuIcon.setText("Icon Convert");
			jMenuIcon.add(getJMenuIconDxf());
			jMenuIcon.add(getJMenuIconRob());
		}
		return jMenuIcon;
	}
	/**
	 * This method initializes jMenuItem7	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */    
	private JMenuItem getJMenuIconDxf() {
		if (jMenuIconDxf == null) {
			jMenuIconDxf = new JMenuItem();
			jMenuIconDxf.setText("toDxf");
			jMenuIconDxf.addActionListener(new java.awt.event.ActionListener() { 
				public void actionPerformed(java.awt.event.ActionEvent e) {    
				    String filerobot;
				    VehicleDesc vdesc;
					JFileChooser f = new JFileChooser();
					int index;
					f.setFileFilter(new FileFilter() {
						public boolean accept(File f) {
							if (f.isDirectory())
								return true;
							if (f.getName().toLowerCase().endsWith(".robot"))
								return true;
							return false;
						}
						public String getDescription() {
							return "Robot Description (*.robot)";
						}
					});
					if(lastpath==null)
					    f.setCurrentDirectory(new File("./"));
					else
					    f.setCurrentDirectory(new File(lastpath));
					f.setDialogTitle("Load Robot file");
					if (f.showOpenDialog(jContentPane)
						!= JFileChooser.APPROVE_OPTION)
						return;
					filerobot = f.getSelectedFile().getAbsolutePath();
					lastpath = f.getSelectedFile().getParent();					
					try {
						vdesc = new VehicleDesc(filerobot);
					} catch (Exception e1) {
						JOptionPane.showMessageDialog(null,"Error opening file "+filerobot,"Error",JOptionPane.WARNING_MESSAGE);
						e1.printStackTrace();
						return;
					}
					DXFFile dxf = new DXFFile();
					if(vdesc.icon == null){
					    System.out.println("No se ha encontrado el icono");
					    return;
					}
					for(int i=0; i<vdesc.icon.length; i++){
					    System.out.println("A�adiendo linea["+i+"] "+vdesc.icon[i]);
					    dxf.addEntity(new LineDxf(vdesc.icon[i]));
					}
					
					f.setFileFilter(new FileFilter() {
						public boolean accept(File f) {
							if (f.isDirectory())
								return true;
							if (f.getName().toLowerCase().endsWith(".dxf"))
								return true;
							return false;
						}
						public String getDescription() {
							return "Dxf File (*.dxf)";
						}
					});
					f.setDialogTitle("Save Robot file");
					index = filerobot.lastIndexOf('.');
					if(index >= 0)
					    filerobot = filerobot.substring(0,index)+".dxf";
					else
					    filerobot = filerobot+".dxf";
					f.setSelectedFile(new File(filerobot));
					if (f.showSaveDialog(jContentPane)
						!= JFileChooser.APPROVE_OPTION)
						return;
					filerobot = f.getSelectedFile().getAbsolutePath();
					lastpath = f.getSelectedFile().getParent();
					try{
					    dxf.createDxf(filerobot);
					}catch(Exception e1){
					    System.out.println("Error guardando archivo dxf");
					}
					    // TODO Crear el metodo
				}
			});
		}
		return jMenuIconDxf;
	}
	/**
	 * This method initializes jMenuItem8	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */    
	private JMenuItem getJMenuIconRob() {
		if (jMenuIconRob == null) {
			jMenuIconRob = new JMenuItem();
			jMenuIconRob.setText("toRobot");
			jMenuIconRob.addActionListener(new java.awt.event.ActionListener() { 
				public void actionPerformed(java.awt.event.ActionEvent e) {    
				    String filerobot;
				    DXFFile dxf = new DXFFile();
					JFileChooser f = new JFileChooser();
					f.setFileFilter(new FileFilter() {
						public boolean accept(File f) {
							if (f.isDirectory())
								return true;
							if (f.getName().toLowerCase().endsWith(".dxf"))
								return true;
							return false;
						}
						public String getDescription() {
							return "DXF File (*.dxf)";
						}
					});
					if(lastpath==null)
					    f.setCurrentDirectory(new File("./"));
					else
					    f.setCurrentDirectory(new File(lastpath));
					f.setDialogTitle("Load Dxf file");
					if (f.showOpenDialog(jContentPane)
						!= JFileChooser.APPROVE_OPTION)
						return;
					filerobot = f.getSelectedFile().getAbsolutePath();
					lastpath = f.getSelectedFile().getParent();					
					try {
					    dxf.load(filerobot);
					} catch (Exception e1) {
						JOptionPane.showMessageDialog(null,"Error opening file "+filerobot,"Error",JOptionPane.WARNING_MESSAGE);
						e1.printStackTrace();
						return;
					}
					LineDxf[] lines = dxf.getLines();
					if(lines == null) return;
					System.out.println("LINES 			= "+lines.length);
					System.out.println("# iFork drawing");
					for(int i = 0; i<lines.length; i++){
					    System.out.println("\niconxi"+i+" = "+DoubleFormat.format(lines[i].getStart().x()));
					    System.out.println("iconyi"+i+" = "+DoubleFormat.format(lines[i].getStart().y()));
					    System.out.println("iconxf"+i+" = "+DoubleFormat.format(lines[i].getEnd().x()));
					    System.out.println("iconyf"+i+" = "+DoubleFormat.format(lines[i].getEnd().y()));
					}

				}
			});
		}
		return jMenuIconRob;
	}
     public static void main(String[] args) {
		new WorldDxfWindow().setVisible(true);
	}

	/**
	 * This is the default constructor
	 */
	public WorldDxfWindow() {
		super();
		initialize();		
	}
	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		this.setJMenuBar(getJMenuMain());
		this.setSize(600,400);
		this.setContentPane(getJContentPane());
		this.setTitle("Conversor de Formatos DXF-World");
		this.addWindowListener(new java.awt.event.WindowAdapter() { 
			public void windowClosing(java.awt.event.WindowEvent e) {    
				File tempfile = new File(FILE_TEMP);
				if(tempfile.exists())
					try{tempfile.delete();}catch(Exception e1){};
					
				System.exit(0);
			}
		});
		if(new File("../TCApps/conf/maps").exists()) lastpath = "../TCApps/conf/maps";
	}
	/**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private javax.swing.JPanel getJContentPane() {
		if (jContentPane == null) {
			BorderLayout borderLayout1 = new BorderLayout();
			jContentPane = new javax.swing.JPanel();
			jContentPane.setLayout(borderLayout1);
			borderLayout1.setHgap(0);
			borderLayout1.setVgap(0);
			jContentPane.add(getJPanel(), java.awt.BorderLayout.CENTER);
		}
		return jContentPane;
	}
	/**
	 * This method initializes jMenuMain
	 * 
	 * @return javax.swing.JMenuBar
	 */
	private javax.swing.JMenuBar getJMenuMain() {
		if(jMenuMain == null) {
			jMenuMain = new javax.swing.JMenuBar();		
			jMenuMain.add(getJMenuFile());
			jMenuMain.add(getJMenuHelp());
		}
		return jMenuMain;
	}
	/**
	 * This method initializes jMenuFile
	 * 
	 * @return javax.swing.JMenu
	 */
	private javax.swing.JMenu getJMenuFile() {
		if(jMenuFile == null) {
			jMenuFile = new javax.swing.JMenu();
			jMenuFile.setText("File");
			jMenuFile.add(getJMenuOpen());
			jMenuFile.add(getJMenuSave());
			jMenuFile.add(getJMenuIcon());
			jMenuFile.add(getJMenuPreview());
			jMenuFile.addSeparator();
			jMenuFile.add(getJMenuClose());
			
			jMenuFile.addSeparator();
			jMenuFile.add(getJMenuExit());
		}
		return jMenuFile;
	}
	/**
	 * This method initializes jMenuHelp
	 * 
	 * @return javax.swing.JMenu
	 */
	private javax.swing.JMenu getJMenuHelp() {
		if(jMenuHelp == null) {
			jMenuHelp = new javax.swing.JMenu();
			jMenuHelp.setText("Help");
			jMenuHelp.add(getJMenuHelp1());
			jMenuHelp.add(getJMenuAbout());
		}
		return jMenuHelp;
	}
	/**
	 * This method initializes jMenuAbout
	 * 
	 * @return javax.swing.JMenuItem
	 */
	private javax.swing.JMenuItem getJMenuAbout() {
		if(jMenuAbout == null) {
			jMenuAbout = new javax.swing.JMenuItem();
			jMenuAbout.setText("About");
			jMenuAbout.addActionListener(new java.awt.event.ActionListener() { 
				public void actionPerformed(java.awt.event.ActionEvent e) {    
					JOptionPane.showMessageDialog(null,
					"Conversor de formatos DXF - WORLD\n\n"+
					"     Autor: Jose Antonio Marin\n" +
					"        Fecha: 13 Enero 2005\n\n",
					"About",
					JOptionPane.PLAIN_MESSAGE);
				}
			});
		}
		return jMenuAbout;
	}
	/**
	 * This method initializes jMenuExit
	 * 
	 * @return javax.swing.JMenuItem
	 */
	private javax.swing.JMenuItem getJMenuExit() {
		if(jMenuExit == null) {
			jMenuExit = new javax.swing.JMenuItem();
			jMenuExit.setText("Exit");
			jMenuExit.addActionListener(new java.awt.event.ActionListener() { 
				public void actionPerformed(java.awt.event.ActionEvent e) {    
					System.exit(0);
				}
			});
		}
		return jMenuExit;
	}
	
	private Component2D getMapdraw() {
		try{
			
			mapdraw = new Component2D();
			mapdraw.setVisible(true);
			mapdraw.setLayout(null);
			mapdraw.setBackground(java.awt.Color.white);;
			mapdraw.addMouseListener(new java.awt.event.MouseAdapter() {
				public void mousePressed(java.awt.event.MouseEvent e) {
					mapdrawMousePressed(e);
				}
				public void mouseReleased(java.awt.event.MouseEvent e) {
					mapdrawMouseReleased(e);
				}
			});
			mapdraw.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
				public void mouseDragged(java.awt.event.MouseEvent e) {
					mapdrawMouseDragged(e);
				}
				public void mouseMoved(java.awt.event.MouseEvent e) {
					mapdrawMouseMoved(e);
				}
			}); 

		}catch(Exception e){
			e.printStackTrace();
		}
		return mapdraw;
	}
	
	public void updaterobot(){
		//System.out.println();
		if(map == null) return;

		if(world == null) world = new World2D (mapdraw.getModel (), map);
		else{
		    world.setmap(map);
		    world.resetModel();
		    repaint();
		}
		world.drawsensors (false);
		world.autoscale (false);
		world.simulated (false);
		world.drawdanger (false);
		world.drawwarning (false);
		world.drawStart ();
		world.drawMap ();
		world.drawObjects ();
		world.drawPath ();	
		world.drawDocks ();
		world.drawStripBeacons ();	
		world.drawDoors ();
		world.drawWayPoints ();
		world.drawCilindricalBeacons ();				
		world.drawZones ();
		world.drawFAreas();
		
		
		mapdraw.setSize(jPanel2.getSize());
	}
	
	public void mapdrawMouseDragged(java.awt.event.MouseEvent e) 
	{
		//System.out.println("mouse ["+e.getX()+", "+e.getY()+ "] scale: "+ mapdraw.getScale());
		
		if (e.isControlDown ())
		{
			mapdraw.setCursor (Cursor.getPredefinedCursor (Cursor.N_RESIZE_CURSOR));		
			mapdraw.mouseZoom (e.getX (), e.getY ());
		}
		else if (e.isShiftDown ())
		{
			mapdraw.setCursor (Cursor.getPredefinedCursor (Cursor.MOVE_CURSOR));		
			mapdraw.mousePan (e.getX (), e.getY ());
		}
	}
	
	protected void mapdrawMouseMoved(java.awt.event.MouseEvent e) 
	{
		Point2		pt;	
		mapdraw.mouseAxis (e.getX (), e.getY ());
		pt		= mapdraw.mouseClick (e.getX (), e.getY ());
		if (pt != null)
			jLabel.setText ("Cursor X:" + getPosString (pt.x (), 3) + " Y:" + getPosString (pt.y (), 3) + " (m)");
	}
	
	public void mapdrawMousePressed(java.awt.event.MouseEvent e) 
	{	
		if (e.isControlDown ())
			mapdraw.setCursor (Cursor.getPredefinedCursor (Cursor.N_RESIZE_CURSOR));
		else if (e.isShiftDown ())
			mapdraw.setCursor (Cursor.getPredefinedCursor (Cursor.MOVE_CURSOR));
			
		mapdraw.mouseDown (e.getX (), e.getY ());
	}
	
	public void mapdrawMouseReleased(java.awt.event.MouseEvent e) 
	{
		mapdraw.setCursor (Cursor.getPredefinedCursor (Cursor.DEFAULT_CURSOR));	
	}
	
	// Static methods
	static public String getPosString (double pos, int dig)
	{
		String			str;
		
		str		= Double.toString (Math.round (pos * 1000.0) * 0.001);
		while (str.length () - str.indexOf ('.') > dig + 1)
			str		= str.substring (0, str.length () - 1);
		while (str.length () - str.indexOf ('.') < dig + 1)
			str		= str + "0";
		
		return str;
	}
	
}  //  @jve:decl-index=0:visual-constraint="10,10"


class visibility{
	public double[][] map;

	public visibility(String file) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line;
		StringTokenizer token;
		double[][] data = new double[200][5];
		int counter = 0;
		while( (line = br.readLine()) != null){
			token = new StringTokenizer(line," ,");
			for(int i=0;token.hasMoreTokens() && i<5;i++){
				data[counter][i] = Double.parseDouble(token.nextToken());
			}
			counter++;
		}
		map = new double[counter][5];
		System.out.println("Map Visibility");
		for(int i=0;i<counter;i++){
			for(int j=0; j<5; j++){
				map[i][j] = data[i][j];
				System.out.print(map[i][j]+" ");
			}
			System.out.println();
		}
	}
	
			
}