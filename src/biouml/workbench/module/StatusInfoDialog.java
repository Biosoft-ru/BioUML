package biouml.workbench.module;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import java.util.logging.Level;

import java.util.logging.Logger;
import biouml.workbench.BioUMLApplication;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.application.dialog.OkCancelDialog;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListener;

import com.developmentontheedge.log.PatternFormatter;
import com.developmentontheedge.log.TextPaneAppender;

/**
 * Dialog for status info
 */
public class StatusInfoDialog extends OkCancelDialog implements JobControlListener
{
    protected TextPaneAppender appender = null;

    //private JPanel mPanel = null;
    //private InfoFrame infoFrame = null;
    private JLabel infoLabel = null;
    private JProgressBar progressBar = null;

    private JobControl jobControl = null;

    /**
    * A constructor
    * @param frame a frame
    */
    public StatusInfoDialog(JFrame frame, String title, Logger cat, JobControl jobControl)
    {
        super(frame, title);
        appender = new TextPaneAppender( new PatternFormatter( "[%4$-7s] :  %5$s%n" ), "logTextPanel" );
        JPanel appenderPanel = appender.getLogTextPanel();
        appenderPanel.setBorder(new EmptyBorder(new Insets(10, 0, 0, 0)));
        appenderPanel.setPreferredSize(new Dimension(400, 300));

        cat.setLevel(Level.INFO);
        cat.addHandler( appender );

        this.jobControl = jobControl;
        jobControl.addListener(this);

        infoLabel = new JLabel();
        infoLabel.setBorder(new EmptyBorder(new Insets(0, 0, 10, 0)));
        progressBar = new JProgressBar(0, 100);
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(infoLabel, BorderLayout.CENTER);
        topPanel.add(progressBar, BorderLayout.SOUTH);

        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(new EmptyBorder(new Insets(10, 10, 10, 10)));

        content.add(topPanel, BorderLayout.NORTH);
        content.add(appenderPanel, BorderLayout.CENTER);

        setContent(content);

        cancelButton.setVisible(false);
        transferFocus();
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    }

    /**
    */
    public void setInfo(String info)
    {
        infoLabel.setText(info);
    }

    public void setMinMax(int min, int max)
    {
        progressBar.setMinimum(min);
        progressBar.setMaximum(max);
    }

    public void setPreparedness(int value)
    {
        progressBar.setValue(value);
    }

    public void startProcess(Thread thread)
    {
        okButton.setEnabled(false);
        pack();
        ApplicationUtils.moveToCenter(this);
        thread.start();
        setVisible(true);
    }

    /**
     * Calls when ok button is pressed.
     * @todo HIGH Generate name.
     */
    @Override
    protected void okPressed()
    {
        setVisible(false);
        dispose();
    }

    @Override
    protected void cancelPressed()
    {
        jobControl.terminate();
    }

    public void success()
    {
        jobControl.terminate();
        okButton.setEnabled(true);
        progressBar.setValue(0);
        infoLabel.setText(BioUMLApplication.getMessageBundle().getResourceString("FINISHED_SUCCESS"));
    }

    public void fails()
    {
        jobControl.terminate();
        okButton.setEnabled(true);
        progressBar.setValue(0);
        infoLabel.setText(BioUMLApplication.getMessageBundle().getResourceString("FINISHED_ERROR"));
    }

    //////////////////////////////////////////////
    // JobControlListener
    //
    @Override
    public void valueChanged(JobControlEvent event)
    {
        progressBar.setValue(event.getPreparedness());
    }
    @Override
    public void jobStarted(JobControlEvent event)
    {
    }
    @Override
    public void jobTerminated(JobControlEvent event)
    {
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
}
