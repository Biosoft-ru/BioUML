package ru.biosoft.analysis.document;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.logging.Level;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.beans.swing.PropertyInspectorEx;

import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.exception.AnalysisFailException;
import ru.biosoft.access.repository.DataElementPathDialog;
import ru.biosoft.analysis.Util;
import ru.biosoft.analysis.gui.CancelAnalysisAction;
import ru.biosoft.analysis.gui.GenerateScriptAction;
import ru.biosoft.analysis.gui.RunAnalysisAction;
import ru.biosoft.analysiscore.AnalysisMethod;
import ru.biosoft.analysiscore.AnalysisMethodElement;
import ru.biosoft.analysiscore.AnalysisMethodInfo;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.DocumentManager;
import ru.biosoft.jobcontrol.ClassJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListener;
import ru.biosoft.jobcontrol.JobProgressBar;
import ru.biosoft.tasks.TaskManager;


public class AnalysisDocument extends Document implements JobControlListener
{
    private PropertyInspector pi;
    private JobProgressBar jpb;

    private AnalysisParameters parameters;

    public AnalysisDocument (AnalysisMethodInfo methodInfo, AnalysisParameters parameters)
    {
        super(methodInfo);
        this.parameters = parameters;
        init();
    }

    public AnalysisDocument(AnalysisMethodInfo methodInfo)
    {
        this(methodInfo, createParameters(methodInfo));
    }

    public AnalysisDocument(AnalysisMethodElement methodElement)
    {
        super(methodElement);
        this.parameters = methodElement.getAnalysisMethod().getParameters();
        init();
    }

    private static AnalysisParameters createParameters(AnalysisMethodInfo methodInfo)
    {
        AnalysisMethod method = methodInfo.createAnalysisMethod();
        if(method == null)
        {
            ApplicationUtils.errorBox("Unable to initialize analysis. See log for details.");
            throw new AnalysisFailException( null, methodInfo.getName() );
        }
        return method.getParameters();
    }

    private void init()
    {
        viewPane = new ViewPane();
        pi = new PropertyInspectorEx();

        jpb = new JobProgressBar();
        jpb.setStringPainted(true);

        initActions();

        if( parameters instanceof Option )
        {
            ((Option)parameters).addPropertyChangeListener(pi);
        }

        update();
        viewPane.add(createMainPanel());
    }

    private JButton runCancelButton;
    private JButton generateScriptButton;
    private JButton expertModeButton;

    private Action runAction;
    private Action cancelAction;

    private void initActions()
    {
        runCancelButton = new JButton();

        runAction = new RunAnalysisAction();
        runAction.putValue(RunAnalysisAction.DOCUMENT, this);

        cancelAction = new CancelAnalysisAction();
        cancelAction.putValue(CancelAnalysisAction.DOCUMENT, this);

        runCancelButton.setAction(runAction);

        generateScriptButton = new JButton();
        generateScriptButton.setText("Generate script");

        Action generateAction = new GenerateScriptAction();
        generateAction.putValue(GenerateScriptAction.DOCUMENT, this);
        generateScriptButton.setAction(generateAction);

        expertModeButton = new JButton();
        expertModeButton.setText("Expert mode");

        expertModeButton.addActionListener(e -> expertModeChanged());
    }

    private JPanel createMainPanel()
    {
        JPanel mainPanel = new JPanel(new GridBagLayout());

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.add(runCancelButton);
        buttonsPanel.add(generateScriptButton);
        buttonsPanel.add(expertModeButton);

        mainPanel.add(pi, new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));
        mainPanel.add(buttonsPanel, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(
                0, 0, 0, 0), 0, 0));
        mainPanel.add(jpb, new GridBagConstraints(0, 2, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0,
                0, 0, 0), 0, 0));

        return mainPanel;
    }

    protected void expertModeChanged()
    {
        if( pi.getPropertyShowMode() == PropertyInspector.SHOW_EXPERT )
        {
            pi.setPropertyShowMode(PropertyInspector.SHOW_USUAL);
            expertModeButton.setText("Expert mode");
            getAnalysisParameters().setExpertMode(false);
        }
        else
        {
            pi.setPropertyShowMode(PropertyInspector.SHOW_EXPERT);
            expertModeButton.setText("Simple mode");
            getAnalysisParameters().setExpertMode(true);
        }
        pi.explore(getAnalysisParameters());
    }

    public AnalysisMethod createAnalysisMethod()
    {
        if(getModel() instanceof AnalysisMethodInfo)
        {
            AnalysisMethod method = ((AnalysisMethodInfo)getModel()).createAnalysisMethod();
            method.setParameters( getAnalysisParameters() );
            return method;
        }

        return ((AnalysisMethodElement)getModel()).getAnalysisMethod();
    }

    public AnalysisParameters getAnalysisParameters()
    {
        applyEditorChanges();
        return parameters;
    }

    @Override
    public String getDisplayName()
    {
        return createAnalysisMethod().getName();
    }

    @Override
    public Action[] getActions(ActionType actionType)
    {
        return null;
    }

    private ClassJobControl jobControl;
    public ClassJobControl startAnalysis()
    {
        runCancelButton.setAction(cancelAction);

        AnalysisMethod method = createAnalysisMethod();
        method.validateParameters();
        method.setLogger(log);

        jobControl = method.getJobControl();
        if( jobControl != null )
        {
            jobControl.addListener(jpb);
            jobControl.addListener(this);
        }

        //analysis will be running by task manager
        TaskManager.getInstance().addAnalysisTask( method, true );
        return jobControl;
    }

    public void stopAnalysis()
    {
        runCancelButton.setAction(runAction);
        jpb.setValue(0);

        if( jobControl != null )
        {
            jobControl.terminate();
            jobControl.removeListener(jpb);
            jobControl.removeListener(this);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Update issues
    //
    @Override
    protected void doUpdate()
    {
        pi.explore(getAnalysisParameters());
    }

    @Override
    public void save()
    {
        DataElementPathDialog dialog = new DataElementPathDialog();
        dialog.setMultiSelect(false);
        dialog.setValue((DataElementPath)null);

        DataElementPath path = null;
        if(dialog.doModal())
            path = dialog.getValue();
        if(path != null)
        {
            try
            {
                AnalysisMethodElement me = new AnalysisMethodElement(path.getName(), path.getParentCollection());
                me.setAnalysisMethod(createAnalysisMethod());
                CollectionFactoryUtils.save(me);
            }
            catch(Exception e)
            {
                log.log(Level.SEVERE, "AnalysisDocument: error of the analysis saving.", e);
            }
        }
        log.info("The document is successfully saved.");
    }

    @Override
    public void close()
    {
        jpb.setValue(0);

        if( jobControl != null )
        {
            if( jobControl.getStatus() == JobControl.RUNNING )
            {
                int status = JOptionPane.showConfirmDialog(Application.getApplicationFrame(), "Run process in background?",
                        "Incomplete process", JOptionPane.YES_NO_OPTION);
                if( status == JOptionPane.NO_OPTION )
                {
                    jobControl.terminate();
                }
            }
            jobControl.removeListener(this);
            jobControl.removeListener(jpb);
        }
        super.close();
    }

    ////////////////////////////////////////////////////////////////////////////
    // Job control issues
    //

    @Override
    public void valueChanged(JobControlEvent event)
    {
    }

    @Override
    public void jobStarted(JobControlEvent event)
    {
        runCancelButton.setAction(cancelAction);
    }

    @Override
    public void jobTerminated(JobControlEvent event)
    {
        runCancelButton.setAction(runAction);
        jpb.setValue(0);

        if( event.getStatus() != JobControl.COMPLETED )
        {
            log.log( Level.SEVERE, event.getMessage() );
        }

        if( jobControl != null )
        {
            jobControl.terminate();
            jobControl.removeListener(this);
            jobControl.removeListener(jpb);
        }
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
        DocumentManager documentManager = DocumentManager.getDocumentManager();

        Object[] results = event.getResults();
        if( results != null )
        {
            StreamEx.of( results ).select( ru.biosoft.access.core.DataElement.class ).forEach( documentManager::openDocument );
        }
        if( jobControl != null )
        {
            double timeMS = jobControl.getElapsedTime();
            log.info("Elapsed time " + Util.getElapsedTime(timeMS));
        }

        log.info("Results are ready.");
    }
}
