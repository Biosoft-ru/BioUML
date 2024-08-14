
package com.developmentontheedge.application;

//import java.util.EventListener;
//import javax.swing.Timer;
//import javax.swing.border.BevelBorder;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListener;




// Status bar for the console.
@SuppressWarnings ( "serial" )
public class ApplicationStatusBar extends JPanel implements  ActionListener, JobControlListener
{

    private static final int PROGRESS_MAX = 100;
    private static final int PROGRESS_MIN = 0;

    private JLabel label;
    private Dimension preferredSize;

    private JProgressBar progressBar;
    private Timer timer=null;
    private int target = 0;

    public ApplicationStatusBar()
    {
        this.setLayout(new FlowLayout(FlowLayout.LEFT));
        this.setBorder(BorderFactory.createEtchedBorder());
        //this.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));

        progressBar = new JProgressBar(JProgressBar.HORIZONTAL,PROGRESS_MIN, PROGRESS_MAX);
        progressBar.setPreferredSize(new Dimension(100, progressBar.getPreferredSize().height + 2));
        progressBar.setVisible(false);

        label = new JLabel();
        preferredSize = new Dimension(getWidth(label.getText()), 2 * getFontHeight());
        Font font = new Font("Arial",Font.PLAIN,12);
        label.setFont(font);

        
        
        this.add(label);
        this.add(progressBar);

    }
    protected int getWidth(String s)
    {
        if (this.getFont() == null)
        {
            return 0;
        }
        return this.getFontMetrics(this.getFont()).stringWidth(s);
    }
    protected int getFontHeight()
    {
        if (this.getFont() == null)
        {
            return 0;
        }
        return this.getFontMetrics(this.getFont()).getHeight();
    }

    @Override
    public Dimension getPreferredSize()
    {
        return preferredSize;
    }

    public void setMessage(String message)
    {
        label.setText(message);
        label.repaint();
    }

    public void startProgressBar(String msg)
    {
        setMessage(msg);
        setValue(0);
        startProgressBar();
    }

    public void startProgressBar()
    {
        //System.out.println("startProgressBar");
        progressBar.setValue(0);
        //CDE.getCDE().getFrame().getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        progressBar.setVisible(true);
        progressBar.repaint();
        //timer = new Timer(15, this);
        timer = new Timer();
        timer.schedule(
                      new TimerTask()
                      {
                          @Override
                        public void run()
                          {
                              actionPerformed(null) ;
                          }
                      }, 0,15);

        //EventListener[] l= timer.getListeners(getClass());
        //System.out.println("0 l="+l.length);
        //timer.start();
        //timer.setLogTimers(true);
    }

    public void setValue(int n)
    {
        if(!progressBar.isVisible())
            startProgressBar();
        target = n;
    }

    public void stopImmediately()
    {
        if (timer==null) return;
        timer.cancel();
        timer = null;
        progressBar.setVisible(false);
    }

    public void stopImmediately(String msg)
    {
        stopImmediately();
        setMessage(msg);
    }

    public void stopProgressBar(String msg)
    {
        stopProgressBar();
        setMessage(msg);
    }

    public void stopProgressBar()
    {
        if (timer==null) return;

        target = PROGRESS_MAX;

        timer.cancel();
        timer = null;
        setMessage("");
        progressBar.setVisible(false);
    }

    @Override
    public void actionPerformed(ActionEvent evt)
    {
        int value = progressBar.getValue();

        if (value >= target)
            return;

        if (value < PROGRESS_MAX)
        {
            progressBar.setValue(value + 1);
            progressBar.repaint();
            //validate();
            //repaint();
            //System.out.println(""+this);
            //System.out.println("progressBar="+progressBar);
        }
    }
    //public void run()
    //{
    //}

    @Override
    public void valueChanged(JobControlEvent event)
    {
        setValue(event.getPreparedness());
    }
    @Override
    public void jobStarted(JobControlEvent event)
    {
        startProgressBar(event.getMessage());
    }
    @Override
    public void jobTerminated(JobControlEvent event)
    {
        stopImmediately(event.getMessage());
    }
    @Override
    public void jobPaused(JobControlEvent event)
    {
    }
    @Override
    public void jobResumed(JobControlEvent event)
    {
    }
    @Override
    public void resultsReady(JobControlEvent event)
    {
    }

} // end class StatusBar
