package tclib.utils.petrinets.gui;

import java.util.*;

import tclib.utils.petrinets.PNTransition;
import tclib.utils.petrinets.PetriNet;


public class PNExecutor implements Runnable
{
    PetriNet PNet;
    PNEditor Ed;
    PNCanvas Vis;
    int StepCount;
    int mode;
    int vismode;
    int delay;
    boolean demo;
    boolean selected;
    boolean single;
    PNTransition selectedTransition;
    
    private boolean running;
    private Thread thread;

    static final int PARMAN = 0;
    static final int PARRAN = 1;
    static final int SEQMAN = 2;
    static final int SEQRAN = 3;
    static final int CODE = 99;

    public PNExecutor(PetriNet pnet, PNEditor ed, PNCanvas vi, int m, boolean sing) 
    {    
        PNet = pnet;
        Ed = ed;
        Vis = vi;
        StepCount = 0;
        mode = m;
        delay = 0;
        selected = false;
        single = sing;
        demo = false;
    }

    public void setPN(PetriNet pnet) {
        PNet = pnet;
    }

    public void setEditor(PNEditor ed) {
        Ed = ed;
    }

    public void setVis(PNCanvas vi) {
        Vis = vi;
    }

    public void setDemo(boolean d) {
        demo = d;
    }

    public void setMode(int m) {
        mode = m;
    }

    public void enableSingleStep() {
        single = true;
    }

    public void disableSingleStep() {
        single = false;
    }

    public void select(PNTransition t) {
        selected = true;
        selectedTransition = t;
	resume();
    }

    public void restoreVisMode() {
        if (mode == SEQMAN || mode == PARMAN) {
            Vis.setMode(vismode, CODE);
            PNet.unhighlightAllTransitions();
            Vis.repaint();
        } else {
	    PNet.unhighlightAllTransitions();
	    Vis.repaint();
        }
    }

    public void holdVisMode() {
        vismode = Vis.getMode();
        Vis.setMode(PNCanvas.MODE_SELECT, 0);
        Vis.repaint(1);
    }

    public void blink(PNTransition t) {
        for (int i = 0; i < 3; i++) {
            t.highlight = true;
            Vis.repaint();
            try {
                Thread.sleep(500);
            }
            catch (InterruptedException l) {}
            t.highlight = false;
            Vis.repaint();
            try {
                Thread.sleep(500);
            }
            catch (InterruptedException l) {}
         }
    }

    public void blink(ArrayList<PNTransition> v) {           //new 18.5.97 jw
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < v.size(); j++) {
                v.get (j).highlight = true;
            }
            Vis.repaint();
            try {
                Thread.sleep(500);
            }
            catch (InterruptedException l) {}
            for (int j = 0; j < v.size(); j++) {
                v.get (j).highlight = false;
            }
            Vis.repaint();
            try {
                Thread.sleep(500);
            }
            catch (InterruptedException l) {}
        }
    }

    public boolean isAlive ()
    {
    		return running;
    }
    
    public void start ()
    {
        delay	= Ed.getDelay();
        demo		= Ed.getDemo();
        
        StepCount = 0;
        running	= true;
        thread	= new Thread (this);
        thread.start ();
    }
    
    public void stop ()
    {
    		running = false;
    		thread	= null;
    }
    
    public void suspend ()
    {
    		running	= false;
    		thread	= null;
    }
    
    public void resume ()
    {
    		running = true;
    		thread	= new Thread (this);
    		thread.start ();
    }
    
    public void run() 
    {                      //New 22.5.97 ah
        switch (mode) {
        case PNExecutor.SEQRAN :
            PNTransition t;
            boolean increaseCounter;
            ArrayList<PNTransition> seq = new ArrayList<PNTransition>();

            while (! PNet.isDead(true) && running) {
                for (int i = 0; i < PNet.numberOfTransitions(); i++){
                    t = PNet.getTransition(i);
                    t.randomize();
                    if (t.canFire(PNet,true)) seq.add (t);
                }
                PNTransition seqtemp = seq.get (0);
                for (int i = 1; i < seq.size()-1; i++) {
                    if (seqtemp.getRan() > seq.get (i).getRan())
                        seqtemp = (seq.get (i));
                }
                t = seqtemp;
                increaseCounter = t.canFire(PNet,true);
                if (increaseCounter) {
		    if (demo) blink(t);
		    t.fire(PNet,true);
                    StepCount++;
                    Vis.repaint();
                    Ed.setCount(StepCount);
                    if (single) {
                        Ed.runIsHolding();
                        suspend();
                    } else {
                        try {
                            Thread.sleep(delay);
                        }
                        catch (InterruptedException l) {}
                    }
                }
            }
            
	    Ed.runButtonsUnHighlight();
            Ed.setStatus("Petri Net is dead!");
            break;
        case PNExecutor.SEQMAN :
            PNTransition t1;
	   
            while (! PNet.isDead(false) && running) {
                for (int i = 0; i < PNet.numberOfTransitions(); i++) {
                    t1 = PNet.getTransition(i);
                    if (t1.canFire(PNet, false)) {
                        t1.highlight = true;
                    }
                }
                selected = false;
                holdVisMode();
                while (! selected) {
		  suspend();
		}
                selectedTransition.fire(PNet, false);
                StepCount++;
                Ed.setCount(StepCount);
                restoreVisMode();
                if (single) {
                    Ed.runIsHolding();
                    suspend();
                }
            }
            
	    Ed.runButtonsUnHighlight();
            Ed.setStatus("Petri Net is dead!");
            break;

        case PNExecutor.PARRAN :
            PNTransition t2;
            boolean changed;
            boolean increaseCounter1;
            PNTransition prio, nextprio, help;
            ArrayList<PNTransition> par = new ArrayList<PNTransition>();


            while (! PNet.isDead(false) && running){
                for (int i = 0; i < PNet.numberOfTransitions(); i++){
                    t2 = PNet.getTransition(i);
                    t2.randomize();
                    if (t2.canFire(PNet,false)) par.add (t2);
                }

                do {
                    changed = false;
                    int size = (par.size()-1);
                    for (int i = 0; i < size ; i++){
                        prio = par.get (i);
                        nextprio = par.get (i+1);
                        if (prio.getRan() < nextprio.getRan() ) {
                            help = nextprio;
                            par.remove (i+1);
                            par.add(i+1, prio);
                            par.remove (i);
                            par.add(i, help);
                            changed = true;
                        }
                    }
                } while (changed);

                par = PNet.multipleCanFire(par,false);

                if (demo) {
                    blink(par);
                }
                increaseCounter1 = false;
                for (int i = 0; i< par.size(); i++){
                    t2 = par.get (i);
                    increaseCounter1 = (t2.fire(PNet, false));
                  }
                Vis.repaint();
                par.clear ();
                if (increaseCounter1) {
                    StepCount++;
                    Vis.repaint();
                    Ed.setCount(StepCount);
                    if (single) {
                        Ed.runIsHolding();
                        suspend();
                    } else {
                        try {
                            Thread.sleep(delay);
                        }
                        catch (InterruptedException l) {}
                    }
                }
                increaseCounter1 = false;
            }
            
	    Ed.runButtonsUnHighlight();
            Ed.setStatus("Petri Net is dead!");
            break;

        case PNExecutor.PARMAN :
            PNTransition t3,t4;
            boolean changed2 = false;
            boolean moreThanOne;
             PNTransition prio2, nextprio2, help2;
            int i;


            ArrayList<PNTransition> par2 = new ArrayList<PNTransition>();
            ArrayList<PNTransition> fired = new ArrayList<PNTransition>();


            while (! PNet.isDead(false) && running){
              for (i = 0; i < PNet.numberOfTransitions(); i++){
                    t3 = PNet.getTransition(i);
                    t3.randomize();
                    if (t3.canFire(PNet,false))
                        par2.add (t3);
                }
                do {
                    changed2 = false;
                    int size = (par2.size()-1);
                    for (i = 0; i < size ; i++){
                        prio2 = par2.get (i);
                        nextprio2 = par2.get (i+1);
                        if (prio2.getRan() < nextprio2.getRan() ) {
                            help2 = nextprio2;
                            par2.remove (i+1);
                            par2.add(i+1, prio2);
                            par2.remove (i);
                            par2.add(i, help2);
                            changed2 = true;
                        }
                    }
                } while (changed2);

                moreThanOne = false;
                ArrayList<PNTransition> fireable = new ArrayList<PNTransition>();

                for (i = 0; i < par2.size(); i++){
                  PNTransition test = par2.get (i);
                  if ((PNet.getAllConnectedFireableTrans(test, false)).size() > 1) {
                    moreThanOne = true;
                    fireable.add (test);
                  }
                }
                if (moreThanOne){
                  PetriNet.subVectorOfVector(fireable, par2);
                  for (i=0; i < fireable.size(); i++){
                    t4 = fireable.get (i);
                    t4.highlight = true;
                  }
                  holdVisMode();
                  while (fireable.size() > 1) {
                    selected = false;

                     while (! selected) {
		      suspend();
		    }
 
                    fired = PNet.getAllConnectedTrans(selectedTransition);

                    for (int k = 0; k < fired.size(); k++) {
                      PNTransition unhigh = fired.get (k);
                      unhigh.highlight = false;
                    }
                    if (demo) blink(selectedTransition);
                    selectedTransition.fire(PNet, false);
                    Vis.repaint();

                    PetriNet.subVectorOfVector(fired, fireable);
                  }
                  restoreVisMode();
                  if (fireable.size() == 1) {
                    fireable.get (0).fire(PNet, false);
                    Vis.repaint();
                  }
                }

                par2 = PNet.multipleCanFire(par2, false);

                if (demo) {
                           blink(par2);
                          }

                for (i=0; i < par2.size(); i++){
                     PNTransition fire = par2.get (i);
                     fire.fire(PNet, false);
                }


                par2.clear ();

                StepCount++;              //zaehlt vielleicht nicht richtig
                Vis.repaint();
                Ed.setCount(StepCount);
                if (single) {
                   Ed.runIsHolding();
                   suspend();
                } else {
                   try {
                     Thread.sleep(delay);
                   }
                   catch (InterruptedException l) {}
                }
            }
	    Ed.runButtonsUnHighlight();
            Ed.setStatus("Petri Net is dead!");
            break;
        }
    }
}

