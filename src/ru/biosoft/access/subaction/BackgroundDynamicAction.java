package ru.biosoft.access.subaction;

import java.util.List;
import java.util.logging.Level;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.task.JobControlTask;
import ru.biosoft.access.task.TaskPool;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.DocumentManager;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationFrame;
import com.developmentontheedge.application.ApplicationUtils;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListenerAdapter;

public abstract class BackgroundDynamicAction extends DynamicAction
{
    public abstract JobControl getJobControl(Object model, List<DataElement> selectedItems, Object properties) throws Exception;

    @Override
    public void performAction(final Object model, List<DataElement> selectedItems, Object properties) throws Exception
    {
        final JobControl jc = getJobControl(model, selectedItems, properties);
        jc.addListener(new JobControlListenerAdapter() {
            @Override
            public void resultsReady(JobControlEvent event)
            {
                ApplicationFrame frame = Application.getApplicationFrame();
                if(frame != null && event.getResults() != null)
                {
                    for(Object result: event.getResults())
                    {
                        if(!(result instanceof DataElement) || ((DataElement)result).getOrigin() == null) continue;
                        DocumentManager.getDocumentManager().openDocument((DataElement)result);
                    }
                }
            }
        });
        final ApplicationFrame frame = Application.getApplicationFrame();
        if(frame != null)
        {
            frame.getStatusBar().startProgressBar();
            jc.addListener(frame.getStatusBar());
        }
        TaskPool.getInstance().submit(new JobControlTask("Action: "+getValue(NAME), jc)
        {
            @Override
            public void doRun()
            {
                try
                {
                    Document document = Document.getActiveDocument();
                    if(document != null && document.getModel() == model)
                    {
                        document.startTransaction( getValue( NAME ).toString() );
                    }
                    try
                    {
                        jc.run();
                    }
                    finally
                    {
                        if(document != null && document.getModel() == model)
                        {
                            document.completeTransaction();
                        }
                    }
                }
                catch( Exception e )
                {
                    if(frame != null)
                        ApplicationUtils.errorBox(e);
                    log.log( Level.SEVERE, e.getMessage(), e );
                }
            }
        });
    }
}
