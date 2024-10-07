package tclib.utils.petrinets.gui;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

import tclib.utils.petrinets.*;
import tclib.utils.petrinets.gui.dialog.*;
import wucore.utils.color.ColorTool;

class PNCanvas extends JComponent
{
    // height and width of tokens/items (in pixel)
    private static final int ItemH = 10;
    private static final int ItemW = 3;

    // different modes, mouse events are handled by
    // (e.g. what kind of item to add when the mousebutton is
    // pressed down.)
    public static final int MODE_NODE = 1;
    public static final int MODE_TRANS = 2;
    public static final int MODE_EDGE = 3;
    public static final int MODE_ATOKEN = 4;
    public static final int MODE_STOKEN = 5;
    public static final int MODE_DELETE = 6;
    public static final int MODE_DRAG = 7;
    public static final int MODE_EDIT = 8;
    public static final int MODE_SELECT = 9;

    private int     mode;

    //
    private static final int MAX_NUM_DELETE_EDGE = 255;

    private int     iDeleteNode;
    private int     iDeleteTransition;
    private int     nDeleteEdge;
    private int     iDeleteEdge[];

    //
    private PNNode        dragingNode;
    private PNTransition  dragingTransition;

    //
    private PNEdge    newEdge;
    private boolean newEdgeFinished;

    //
    private PetriNet      PNet;

    //
    private Image   BackBuffer;
    protected JFrame	parent;
     
    PNNodeDialog NodeDial;
    PNTransitionDialog TransDial;
    PNEdgeDialog EdgeDial;

    public PNCanvas(PetriNet PN, Dimension dim, JFrame parent) {
    	
    		this.parent = parent;
    		
        PNet = PN;
         setSize (dim);
        newEdgeFinished = true;

        iDeleteNode = -1;
        iDeleteTransition = -1;
        nDeleteEdge = 0;
        iDeleteEdge = new int[MAX_NUM_DELETE_EDGE];
        
		addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
			public void mouseDragged(java.awt.event.MouseEvent e) {
				cmouseDragged(e);
			}
			public void mouseMoved(java.awt.event.MouseEvent e) {
				cmouseMoved(e);
			}
		});		
		addMouseListener(new java.awt.event.MouseAdapter() {
			public void mousePressed(java.awt.event.MouseEvent e) {
				cmousePressed(e);
			}		
			public void mouseReleased(java.awt.event.MouseEvent e) {
				cmouseReleased(e);
			}
		});	
    }

     public int getMode() {
        return mode;
    }

    public boolean setMode(int mode, int autorisation) {
        if (this.mode == MODE_SELECT) {
            if (autorisation == PNExecutor.CODE) {
                this.mode = mode;
                iDeleteNode = -1;
                iDeleteTransition = -1;
                nDeleteEdge = 0;
                return true;
            }
        } else {
            this.mode = mode;
            iDeleteNode = -1;
            iDeleteTransition = -1;
            nDeleteEdge = 0;
            return true;
        }
        return false;
    }

    public void setPN(PetriNet PN) {
        PNet = PN;
    }

	protected void cmousePressed(MouseEvent evt)
	{
        // right mouse button down
	   if ((mode == MODE_DRAG) || (evt.isControlDown () || ((evt.getModifiers () & MouseEvent.BUTTON2_MASK) != 0)))
            mouseMetaDown(evt);
        else 
        {
            if (mode == MODE_SELECT)
                mouseDownSelect(evt);
            else if (mode == MODE_NODE)
                mouseDownNode(evt);
            else if (mode == MODE_TRANS)
                mouseDownTransition(evt);
            else if (mode == MODE_EDGE)
                mouseDownEdge(evt);
            else if (mode == MODE_ATOKEN)
                mouseDownAToken(evt);
            else if (mode == MODE_STOKEN)
                mouseDownSToken(evt);
            else if (mode == MODE_DELETE)
                mouseDownDelete(evt);
            else if (mode == MODE_EDIT)
                mouseDownEdit(evt);
        }
        repaint();
	}
	
	protected void cmouseReleased(java.awt.event.MouseEvent evt)
	{
        if (mode == MODE_EDGE)
        	{
        		mouseDownEdge (evt);
        		if (!newEdgeFinished)
        		{
        	           PNet.removeEdge(newEdge);
        	            newEdgeFinished = true;
        	    }
        	}
        repaint();
	}
	
	protected void cmouseMoved(java.awt.event.MouseEvent evt)
	{
        if (mode == MODE_NODE)
            ;
        else if (mode == MODE_TRANS)
            ;
        else if (mode == MODE_EDGE)
            mouseMoveEdge(evt);
        else if (mode == MODE_DELETE)
            mouseMoveDelete(evt);
        repaint();
	}
	
	protected void cmouseDragged(java.awt.event.MouseEvent evt)
	{
        if ((mode == MODE_DRAG) || (evt.isControlDown () || ((evt.getModifiers () & MouseEvent.BUTTON2_MASK) != 0)))
            mouseMetaDrag(evt);
        else 
        {
            if (mode == MODE_NODE)
                ;
            else if (mode == MODE_TRANS)
                ;
            else if (mode == MODE_EDGE)
                mouseDragEdge(evt);
        }
        repaint();
	}
	
    private void mouseDownSelect(MouseEvent evt) {
        PNTransition t = getTransitionAtXY(evt.getX (), evt.getY (), null);
        if ((t != null) && (t.highlight)) {
            ((PNEditor)getParent()).getRunStep().select(t);
        }
    }

    private void mouseDownEdit(MouseEvent evt) {
        PNNode n = getNodeAtXY(evt.getX (), evt.getY (), null);
        PNTransition t = getTransitionAtXY(evt.getX (), evt.getY (), null);
        PNEdge e = getEdgeAtXY(evt.getX (), evt.getY (), null);
        if (n != null) {
            if (NodeDial == null) {
                NodeDial = new PNNodeDialog(parent, n);
            } else {
                NodeDial.setNode(n);
            }
            NodeDial.setVisible(true);
            this.repaint();
            e = null;
        }
        if (t != null) {
            if (TransDial == null) {
                TransDial = new PNTransitionDialog(parent, t);
            } else {
                TransDial.setTransition(t);
            }
            TransDial.setVisible(true);
            this.repaint();
            e = null;
        }
        if (e != null) {
            if (EdgeDial == null) {
                EdgeDial = new PNEdgeDialog(parent, e);
            } else {
                EdgeDial.setEdge(e);
            }
            EdgeDial.setVisible(true);
            this.repaint();
        }
    }


    private void mouseMoveDelete(MouseEvent evt) {
        PNNode n = getNodeAtXY(evt.getX (), evt.getY (), null);
        PNTransition t = getTransitionAtXY(evt.getX (), evt.getY (), null);
        PNEdge e = getEdgeAtXY(evt.getX (), evt.getY (), null);
        if (n != null) {
            iDeleteNode = PNet.getIndexOf(n);
            iDeleteTransition = -1;
            nDeleteEdge = 0;
            for (int i = 0; i < PNet.numberOfEdges(); i++) {
                PNEdge ee = PNet.getEdge(i);
                if (ee.getTFrom() == PNEdge.NODE && ee.getIFrom() == iDeleteNode ||
                    ee.getTTo() == PNEdge.NODE && ee.getITo() == iDeleteNode) {
                    iDeleteEdge[nDeleteEdge] = i;
                    nDeleteEdge++;
                }
            }
        }
        else if (t != null) {
            iDeleteNode = -1;
            iDeleteTransition = PNet.getIndexOf(t);
            nDeleteEdge = 0;
            for (int i = 0; i < PNet.numberOfEdges(); i++) {
                PNEdge ee = PNet.getEdge(i);
                if (ee.getTFrom() == PNEdge.TRANSITION && ee.getIFrom() == iDeleteTransition ||
                    ee.getTTo() == PNEdge.TRANSITION && ee.getITo() == iDeleteTransition) {
                    iDeleteEdge[nDeleteEdge] = i;
                    nDeleteEdge++;
                }
            }
        }
        else if (e != null) {
            iDeleteNode = -1;
            iDeleteTransition = -1;
            nDeleteEdge = 1;
            iDeleteEdge[0] = PNet.getIndexOf(e);
        }
        else {
            iDeleteNode = -1;
            iDeleteTransition = -1;
            nDeleteEdge = 0;
        }
    }

    private void mouseDownDelete(MouseEvent evt) {
        PNNode n = getNodeAtXY(evt.getX (), evt.getY (), null);
        PNTransition t = getTransitionAtXY(evt.getX (), evt.getY (), null);
        PNEdge e = getEdgeAtXY(evt.getX (), evt.getY (), null);
        if (n != null) {
            iDeleteNode = PNet.getIndexOf(n);
            iDeleteTransition = -1;
            nDeleteEdge = 0;
            for (int i = 0; i < PNet.numberOfEdges(); i++) {
                PNEdge ee = PNet.getEdge(i);
                if (ee.getTFrom() == PNEdge.NODE && ee.getIFrom() == iDeleteNode ||
                    ee.getTTo() == PNEdge.NODE && ee.getITo() == iDeleteNode) {
                    PNet.removeEdge(ee);
                }
            }
            PNet.removeNode(n);
            iDeleteNode = -1;
        }
        else if (t != null) {
            iDeleteNode = -1;
            iDeleteTransition = PNet.getIndexOf(t);
            nDeleteEdge = 0;
            for (int i = 0; i < PNet.numberOfEdges(); i++) {
                PNEdge ee = PNet.getEdge(i);
                if (ee.getTFrom() == PNEdge.TRANSITION && ee.getIFrom() == iDeleteTransition ||
                    ee.getTTo() == PNEdge.TRANSITION && ee.getITo() == iDeleteTransition) {
                    PNet.removeEdge(ee);
                }
            }
            PNet.removeTransition(t);
            iDeleteTransition = -1;
        }
        else if (e != null) {
            iDeleteNode = -1;
            iDeleteTransition = -1;
            nDeleteEdge = 0;
            PNet.removeEdge(e);
        }
        else {
            iDeleteNode = -1;
            iDeleteTransition = -1;
            nDeleteEdge = 0;
        }
    }

    private void mouseMetaDown(MouseEvent evt) {
        PNNode n = getNodeAtXY(evt.getX (), evt.getY (), null);
        PNTransition t = getTransitionAtXY(evt.getX (), evt.getY (), null);
        if (n != null) {
            if (mode == MODE_ATOKEN) {
                n.decTokens();
                dragingNode = null;
                dragingTransition = null;
            } else {
                dragingNode = n;
                dragingTransition = null;
            }
        }
        else if (t != null) {
            dragingNode = null;
            dragingTransition = t;
        }
        else {
            dragingNode = null;
            dragingTransition = null;
        }
    }

    private void mouseMetaDrag(MouseEvent evt) {
        if (dragingNode != null) {
            dragingNode.x(evt.getX ());
            dragingNode.y(evt.getY ());
        }
        else if (dragingTransition != null) {
            dragingTransition.x(evt.getX ());
            dragingTransition.y(evt.getY ());
        }
    }

    private void mouseDownNode(MouseEvent evt) {
        if (!tooCloseToNode(evt.getX (), evt.getY (), null) &&
            !tooCloseToTransition(evt.getX (), evt.getY () , null))
            PNet.addNode(evt.getX (), evt.getY ());
    }

    private void mouseDownTransition(MouseEvent evt) {
        if (!tooCloseToNode(evt.getX (), evt.getY (), null) &&
            !tooCloseToTransition(evt.getX (), evt.getY (), null))
            PNet.addTransition(evt.getX (), evt.getY ());
        else {
            PNTransition t = getTransitionAtXY(evt.getX (), evt.getY (), null);
            if (t != null)
                t.cycleOrientation();
        }
    }

    private void mouseDownAToken(MouseEvent evt) {
        PNNode n = getNodeAtXY(evt.getX (), evt.getY (), null);
        if (n != null)
            n.incTokens();
    }

    private void mouseDownSToken(MouseEvent evt) {
        PNNode n = getNodeAtXY(evt.getX (), evt.getY (), null);
        if (n != null)
            n.decTokens();
    }

   private void mouseDownEdge(MouseEvent evt)
    {
        PNNode n = getNodeAtXY(evt.getX (), evt.getY (), null);
        PNTransition t = getTransitionAtXY(evt.getX (), evt.getY (), null);

        if (newEdgeFinished)
        {
            if (n != null) {
                newEdge = PNet.addEdge(PNEdge.NODE, PNet.getIndexOf(n), PNEdge.NOTHING, 0, null);
                newEdgeFinished = false;
            }
            else if (t != null) {
                newEdge = PNet.addEdge(PNEdge.TRANSITION, PNet.getIndexOf(t), PNEdge.NOTHING, 0, null);
                newEdgeFinished = false;
            }
        }
        else 
        {
            if (n != null) {
                // edge starts and ends at a node
                if (newEdge.getTFrom() == PNEdge.NODE) {
                    PNet.removeEdge(newEdge);
                    newEdgeFinished = true;
                }
                else {
                    newEdge.clearLastPoint();
                    newEdge.setTTo(PNEdge.NODE);
                    newEdge.setITo(PNet.getIndexOf(n));
                    newEdgeFinished = true;
                }
            }
            else if (t != null) {
                // edge starts and ends at a transition
                if (newEdge.getTFrom() == PNEdge.TRANSITION) {
                    PNet.removeEdge(newEdge);
                    newEdgeFinished = true;
                }
                else {
                    newEdge.clearLastPoint();
                    newEdge.setTTo(PNEdge.TRANSITION);
                    newEdge.setITo(PNet.getIndexOf(t));
                    newEdgeFinished = true;
                }
            }
            else {
                newEdge.setLastPoint(evt.getX (), evt.getY ());
                newEdge.addPoint(evt.getX (), evt.getY ());
            }
        }
    }

    private void mouseMoveEdge(MouseEvent evt) 
    {   
    	}

    private void mouseDragEdge(MouseEvent evt) {
          PNNode n = getNodeAtXY(evt.getX (), evt.getY (), null);
         PNTransition t = getTransitionAtXY(evt.getX (), evt.getY (), null);

         if (!newEdgeFinished)
         {
             if (n != null) {
                 Point p;
                 if (newEdge.getNumberOfPoints() > 1)
                     p = newEdge.getPoint(newEdge.getNumberOfPoints() - 2);
                 else
                     p = newEdge.getXYFrom(PNet);
                 newEdge.setLastPoint(p.x, p.y);
                 newEdge.setTTo(PNEdge.NODE);
                 newEdge.setITo(PNet.getIndexOf(n));
             }
             else if (t != null) {
                 Point p;
                 if (newEdge.getNumberOfPoints() > 1)
                     p = newEdge.getPoint(newEdge.getNumberOfPoints() - 2);
                 else
                     p = newEdge.getXYFrom(PNet);
                 newEdge.setLastPoint(p.x, p.y);
                 newEdge.setTTo(PNEdge.TRANSITION);
                 newEdge.setITo(PNet.getIndexOf(t));
             }
             else {
                 newEdge.setLastPoint(evt.getX (), evt.getY ());
                 newEdge.setTTo(PNEdge.NOTHING);
                 newEdge.setITo(0);
             }
         }
    }

    public void update(Graphics g)
    {
    		Dimension ViewDim = getSize ();
        if (BackBuffer == null)
            BackBuffer = createImage(ViewDim.width, ViewDim.height);
        Graphics gBB = BackBuffer.getGraphics();
        gBB.setColor(getBackground());
        gBB.fillRect(0, 0, ViewDim.width, ViewDim.height);
        gBB.setColor(Color.black);

        drawNodes(PNet, gBB);
        drawTransitions(PNet, gBB);
        drawEdges(PNet, gBB);

        g.drawImage(BackBuffer, 0, 0, ViewDim.width, ViewDim.height, null);
    }

    public void paint(Graphics g)
    {
   		Dimension ViewDim = getSize ();
   	 
        if (BackBuffer == null)
            BackBuffer = createImage(ViewDim.width, ViewDim.height);
        Graphics gBB = BackBuffer.getGraphics();
        gBB.setColor(getBackground());
        gBB.fillRect(0, 0, ViewDim.width, ViewDim.height);
        gBB.setColor(Color.black);

        drawNodes(PNet, gBB);
        drawTransitions(PNet, gBB);
        drawEdges(PNet, gBB);

        g.drawImage(BackBuffer, 0, 0, ViewDim.width, ViewDim.height, null);
    }

    // ge�ndert (04.05.1997 MK)
    private boolean drawNodes(PetriNet PN, Graphics g) {
        for (int i = 0; i < PN.numberOfNodes(); i++) {
            PNNode n;
            n = PN.getNode(i);

            if (iDeleteNode == -1)
                g.setColor(ColorTool.fromWColorToColor(n.getColor()));
            else if (iDeleteNode == i)
                g.setColor(Color.gray);
            else
                g.setColor(ColorTool.fromWColorToColor(n.getColor()));

            g.fillOval(n.x() - n.getRadius(), 
                       n.y() - n.getRadius(),
                       2 * n.getRadius(),
                       2 * n.getRadius());

 
            g.setColor(Color.black);
               g.drawOval(n.x() - n.getRadius(), 
               n.y() - n.getRadius(),
               2 * n.getRadius(),
               2 * n.getRadius());

           if (n.getTokens() == 1) {
                g.fillRect(n.x() - (ItemW / 2), n.y() - (ItemH / 2), ItemW, ItemH);
            }
            else if (n.getTokens() == 2) {
                g.fillRect(n.x() + (ItemW / 2), n.y() - (ItemH / 2), ItemW, ItemH);
                g.fillRect(n.x() - (3 * (ItemW / 2)), n.y() - (ItemH / 2), ItemW, ItemH);
            }
            else if (n.getTokens() == 3) {
                g.fillRect(n.x() - (5 * (ItemW / 2)), n.y() - (ItemH / 2), ItemW, ItemH);
                g.fillRect(n.x() - (ItemW / 2), n.y() - (ItemH / 2), ItemW, ItemH);
                g.fillRect(n.x() + (3 * (ItemW / 2)), n.y() - (ItemH / 2), ItemW, ItemH);
            }
            else if (n.getTokens() > 3 && n.getTokens() < 10) {
                Font f = g.getFont();
                int size = f.getSize();
                g.drawString(Integer.toString(n.getTokens()), n.x() - (size / 4), n.y() + (size / 2));
            }
            else if (n.getTokens() > 9) {
                Font f = g.getFont();
                int size = f.getSize();
                g.drawString(Integer.toString(n.getTokens()), n.x() - (size / 2), n.y() + (size / 2));
            }
 
            // display name of node
            if (n.isNamed ())
            {
                String s = n.getName();
                FontMetrics fm = g.getFontMetrics();
                int w = fm.stringWidth(s);
                int h = fm.getHeight();
                g.drawString(s, n.x() - (w / 2), n.y() - (n.getRadius() / 2) - h);
            }
         }
        return true;
    }

    // ge�ndert (04.05.1997 MK)
    private boolean drawTransitions(PetriNet PN, Graphics g) {
        for (int i = 0; i < PN.numberOfTransitions(); i++) {
            PNTransition t;
            t = PN.getTransition(i);

            if (iDeleteTransition != -1 && iDeleteTransition == i)
                g.setColor(Color.lightGray);
            else
                g.setColor(Color.blue);

            if (t.highlight) g.setColor(Color.yellow);

            switch(t.getOrientation()) {
            case PNTransition.ORIENTATION_VERTICAL:
                g.fillRect(t.x() - (t.getWidth() / 2),
                           t.y() - (t.getHeight() / 2),
                           t.getWidth(),
                           t.getHeight());
                break;
            case PNTransition.ORIENTATION_DIAGONAL1:
                {

                    int[] xpoints = new int[4];
                    int[] ypoints = new int[4];

                    double Sin45 = 0.707106781;

                    xpoints[0] = (int) (t.x() + ((-t.getWidth() / 2.0) * Sin45 + (-t.getHeight() / 2.0) * Sin45));
                    ypoints[0] = (int) (t.y() + ((-t.getWidth() / 2.0) * Sin45 + (-t.getHeight() / 2.0) * (-Sin45)));
                    xpoints[1] = (int) (t.x() + (( t.getWidth() / 2.0) * Sin45 + (-t.getHeight() / 2.0) * Sin45));
                    ypoints[1] = (int) (t.y() + (( t.getWidth() / 2.0) * Sin45 + (-t.getHeight() / 2.0) * (-Sin45)));
                    xpoints[2] = (int) (t.x() + (( t.getWidth() / 2.0) * Sin45 + ( t.getHeight() / 2.0) * Sin45));
                    ypoints[2] = (int) (t.y() + (( t.getWidth() / 2.0) * Sin45 + ( t.getHeight() / 2.0) * (-Sin45)));
                    xpoints[3] = (int) (t.x() + ((-t.getWidth() / 2.0) * Sin45 + ( t.getHeight() / 2.0) * Sin45));
                    ypoints[3] = (int) (t.y() + ((-t.getWidth() / 2.0) * Sin45 + ( t.getHeight() / 2.0) * (-Sin45)));

                    g.fillPolygon(xpoints, ypoints, 4);
                } 
                break;
            case PNTransition.ORIENTATION_HORIZONTAL:
                g.fillRect(t.x() - (t.getHeight() / 2),
                           t.y() - (t.getWidth() / 2),
                           t.getHeight(),
                           t.getWidth());
                break;
            case PNTransition.ORIENTATION_DIAGONAL2:
                {
                    int[] xpoints = new int[4];
                    int[] ypoints = new int[4];

                    double Sin45 = 0.707106781;

                    xpoints[0] = (int) (t.x() + ((-t.getWidth() / 2.0) * Sin45 + (-t.getHeight() / 2.0) * Sin45));
                    ypoints[0] = (int) (t.y() + ((-t.getWidth() / 2.0) * (-Sin45) + (-t.getHeight() / 2.0) * Sin45));
                    xpoints[1] = (int) (t.x() + (( t.getWidth() / 2.0) * Sin45 + (-t.getHeight() / 2.0) * Sin45));
                    ypoints[1] = (int) (t.y() + (( t.getWidth() / 2.0) * (-Sin45) + (-t.getHeight() / 2.0) * Sin45));
                    xpoints[2] = (int) (t.x() + (( t.getWidth() / 2.0) * Sin45 + ( t.getHeight() / 2.0) * Sin45));
                    ypoints[2] = (int) (t.y() + (( t.getWidth() / 2.0) * (-Sin45) + ( t.getHeight() / 2.0) * Sin45));
                    xpoints[3] = (int) (t.x() + ((-t.getWidth() / 2.0) * Sin45 + ( t.getHeight() / 2.0) * Sin45));
                    ypoints[3] = (int) (t.y() + ((-t.getWidth() / 2.0) * (-Sin45) + ( t.getHeight() / 2.0) * Sin45));

                    g.fillPolygon(xpoints, ypoints, 4);
                }
                break;
            case PNTransition.ORIENTATION_ALL:
                for (int j = 0; j < t.getWidth(); j++) {
                    g.drawRect(t.x() - (t.getHeight() / 2) + j,
                               t.y() - (t.getHeight() / 2) + j,
                               t.getHeight() - (2*j),
                               t.getHeight() - (2*j));
                }
                break;
            }
            // display name of transition
            if (t.isNamed ())
            {
                String s = t.getName();
                FontMetrics fm = g.getFontMetrics();
                int w = fm.stringWidth(s);
                int h = fm.getHeight();
                g.drawString(s, t.x() - (w / 2), t.y() - (t.getHeight() / 2) - h);
            }
         }
        return true;
    }

    private boolean drawEdges(PetriNet PN, Graphics g) {
        int iDel = 0;
        for (int i = 0; i < PN.numberOfEdges(); i++) {
            PNEdge e;
            e = PN.getEdge(i);
            if (iDel < nDeleteEdge && iDeleteEdge[iDel] == i) {
                g.setColor(Color.lightGray);
                iDel++;
            }
            else
                g.setColor(Color.black);
            drawEdge(e, g, PN);
        }
        return true;
    }

    // ge�ndert (25.03.1997 MK)
    // v�llig ge�ndert (04.05.1997 MK)
    private boolean drawEdge(PNEdge e, Graphics g, PetriNet PN) {
        Polygon points = e.getPoints();
        //
        if (e.getTFrom() == e.getTTo()) {
            g.setColor(Color.red);
            if (e.getIFrom() == e.getITo() && points != null) {
                if (points.npoints <= 2)
                    return false;
            }
        }
        // edges that will not be drawn
        if (e.getTFrom() == PNEdge.NOTHING)
            return false;
        if (e.getTTo() == PNEdge.NOTHING && points == null)
            return false;

        Point pFrom, pTo;
        pFrom = e.getXYFrom(PN);
        pTo = e.getXYTo(PN);/*       if (e.getTFrom() == edge.TRANSITION) {
            transition t = PN.getTransition(e.getIFrom());
            if (t.getOrientation() == transition.ORIENTATION_ALL) {
                pFrom = e.getXYFrom(PN, 15.0);
                pTo = e.getXYTo(PN, 15.0);
            } else {
                pFrom = e.getXYFrom(PN);
                pTo = e.getXYTo(PN);            }
        } else {
            pFrom = e.getXYFrom(PN);
            pTo = e.getXYTo(PN);
        }
*/
        // if edge is negative, the edge is drawn further away from the transition,
        // so it doesnt stick out of the oval at the end of the edge
        if (e.isNegated() == true) {
            if (e.getTFrom() == PNEdge.TRANSITION) {
                pFrom = e.getXYFrom(PN, 6.0);
                g.fillOval(pFrom.x - 4, pFrom.y - 4, 8, 8);
            }
            else if (e.getTTo() == PNEdge.TRANSITION) {
                pTo = e.getXYTo(PN, 6.0);
                g.fillOval(pTo.x - 4, pTo.y - 4, 8, 8);
            }
        }

        // if edge is build out of more than one line draw, draw all lines except
        // the last one (which will be an arrow or a just line again)
        if (points != null) {
            points.xpoints[0] = pFrom.x;
            points.ypoints[0] = pFrom.y;
            g.drawPolygon(points);

            // x and y position for the last line are adjusted
            pFrom.x = points.xpoints[points.npoints-1];
            pFrom.y = points.ypoints[points.npoints-1];
        }

        // draw arrow or line (the line already got the oval at the transition)
        if (e.isNegated() == false) {
            drawArrow(g, pFrom, pTo);
        }
        else {
            g.drawLine(pFrom.x, pFrom.y, pTo.x, pTo.y);
        }

        // the weight of the edge will be drawn if the weight of the edge
        // is higher than 1 and not negated
        if (e.getWeight() > 1 && e.isNegated() == false) {
            Point weightPos = e.getWeightPosition(PN);
            g.drawString("" + e.getWeight(), weightPos.x, weightPos.y);
        }

        return true;
    }

    private static void drawArrow(Graphics g, Point p1, Point p2) {
 //     System.out.println("drawArrow from (" + p1.x + ", " + p1.y + ") to (" + p2.x + ", " + p2.y + ")");
        drawArrow(g, p1.x, p1.y, p2.x, p2.y);
    }

    private static void drawArrow(Graphics g, int xS, int yS, int xE, int yE) {
        // variables to store the x, y koordinates of the polygon
        // forming the head of the arrow.
        int xP[] = new int[3];
        int yP[] = new int[3];

        if (xE < 0.0)
            xP[0] = (int) (xE - 1.0);
        else
            xP[0] = (int) (xE + 1.0);

        if (yE < 0.0)
            yP[0] = (int) (yE - 1.0);
        else
            yP[0] = (int) (yE + 1.0);

        double  dx = xS - xE;
        double  dy = yS - yE;
 //       System.out.println(xS + " - " + xE + " = " + dx);
 //       System.out.println(yS + " - " + yE + " = " + dy);
        double  length = Math.sqrt(dx * dx + dy * dy);

        double  xAdd = 9.0 * dx / length;
        double  yAdd = 9.0 * dy / length;

        xP[1] = (int) Math.round (xE + xAdd - (yAdd / 3.0));
        yP[1] = (int) Math.round (yE + yAdd + (xAdd / 3.0));
        xP[2] = (int) Math.round (xE + xAdd + (yAdd / 3.0));
        yP[2] = (int) Math.round (yE + yAdd - (xAdd / 3.0));

//      System.out.println(xS + " " + yS + ", " + xE + " " + yE + ", " + xAdd + " " + yAdd);

        g.drawLine(xS, yS, xP[0], yP[0]);
        g.fillPolygon(xP, yP, 3);
    }

    private boolean tooCloseToNode(int x, int y, PNNode IgnoreNode) {
        PNNode n = getClosestNode(x, y, IgnoreNode);
        if (n == null)
            return false;
        if (n.distance(x, y) < (1.5 * n.getRadius()))
            return true;
        return false;
    }

    private boolean tooCloseToTransition(int x, int y, PNTransition IgnoreTransition) {
        PNTransition t = getClosestTransition(x, y, IgnoreTransition);
        if (t == null)
            return false;
        if (t.distance(x, y) < (1.5 * t.getHeight()))
            return true;
        return false;
    }

    public PNNode getNodeAtXY(int x, int y, PNNode IgnoreNode) {
        PNNode n = getClosestNode(x, y, IgnoreNode);
        if (n == null)
            return null;
        if (n.distance(x, y) < n.getRadius())
            return n;
        return null;
    }

    public PNTransition getTransitionAtXY(int x, int y, PNTransition IgnoreTransition) {
        PNTransition t = getClosestTransition(x, y, IgnoreTransition);
        if (t == null)
            return null;
        if (t.distance(x, y) < (t.getHeight() / 2))
            return t;
        return null;
    }

    public PNEdge getEdgeAtXY(int x, int y, PNEdge IgnoreEdge) {
        PNEdge e = getClosestEdge(x, y, IgnoreEdge);
        if (e == null)
            return null;
        if (e.distance(x, y, PNet) < 10.0)
            return e;
        return null;
    }

    public PNNode getClosestNode(int x, int y, PNNode IgnoreNode) {
        PNNode    n, cn;
        double  d;
        double  cd = Double.MAX_VALUE;
        if (PNet.numberOfNodes() == 0)
            return null;
        else
            cn = PNet.getNode(0);

        for (int i = 0; i < PNet.numberOfNodes(); i++) {
            n = PNet.getNode(i);
            // Node is ignored, if it has the same index (-> nodes are the same)
            if (! PNet.equal (n, IgnoreNode)) {
                d = n.distance((double) x, (double) y);
                if (d < cd) {
                    cn = n;
                    cd = d;
                }
            }
        }
        return cn;
    }

/*  public double getDistanceToClosestNode(int x, int y, node IgnoreNode) {
        double d = Double.MAX_VALUE;
        node n = getClosestNode(x, y, IgnoreNode);
        if (n != null)
            d = n.distance(x, y);
        return d;
    }
*/
    public PNTransition getClosestTransition(int x, int y, PNTransition IgnoreTrans) {
        PNTransition    t, ct;
        double  d;
        double  cd = Double.MAX_VALUE;
        if (PNet.numberOfTransitions() == 0)
            return null;
        else
            ct = PNet.getTransition(0);

        for (int i = 0; i < PNet.numberOfTransitions(); i++) {
            t = PNet.getTransition(i);
            // Transition is ignored, if it has the same index (-> Transitions
            // are the same)
            if (!PNet.equal (t, IgnoreTrans)) {
                d = t.distance((double) x, (double) y);
                if (d < cd) {
                    ct = t;
                    cd = d;
                }
            }
        }
        return ct;
    }

/*  public double getDistanceToClosestTransition(int x, int y, transition IgnoreTrans) {
        double d = Double.MAX_VALUE;
        transition t = getClosestTransition(x, y, IgnoreTrans);
        if (t != null)
            d = t.distance(x, y);
        return d;
    }
*/
    public PNEdge getClosestEdge(int x, int y, PNEdge IgnoreEdge) {
        PNEdge    e, ce;
        double  d;
        double  cd = Double.MAX_VALUE;
        if (PNet.numberOfEdges() == 0)
            return null;
        else
            ce = PNet.getEdge(0);

        for (int i = 0; i < PNet.numberOfEdges(); i++) {
            e = PNet.getEdge(i);
            // Edge is ignored, if it has the same index ( => Edges
            // are the same)
            if (!PNet.equal (e, IgnoreEdge)) {
                d = e.distance((double) x, (double) y, PNet);
                if (d < cd) {
                    ce = e;
                    cd = d;
                }
            }
        }
        return ce;
    }

/*  public Object getClosestItem(int x, int y, Object IgnoreObject) {
        Object o;
        double distNode, distTrans;
        node closestNode;
        transition closestTransition;

        o = null;

        if (IgnoreObject == null) {
            o = (Object) getClosestNode(x, y, null);
            if (getDistanceToClosestNode(x, y, null) > getDistanceToClosestTransition(x, y, null))
                o = (Object) getClosestTransition(x, y, null);
            return o;
        }
        return o;
    }
*/
/*  public double getDistanceToClosestItem(int x, int y, Object IgnoreObject) {
        double distTrans, distNode, dist;
        distTrans = distNode = dist = Double.MAX_VALUE;
        if (IgnoreObject == null) {
            dist = distNode = getDistanceToClosestNode(x, y, null);
            distTrans = getDistanceToClosestTransition(x, y, null);
        }
        else {
            if (IgnoreObject instanceof node) {
                dist = distNode = getDistanceToClosestNode(x, y, (node) IgnoreObject);
                distTrans = getDistanceToClosestTransition(x, y, null);
            }
            else if (IgnoreObject instanceof transition) {
                dist = distNode = getDistanceToClosestNode(x, y, null);
                distTrans = getDistanceToClosestTransition(x, y, (transition) IgnoreObject);
            }
        }

        if (distTrans < distNode)
            dist = distTrans;
        return dist;
    }
*/
}