package biouml.workbench.diagram;

import java.util.List;
import java.util.logging.Level;

import biouml.model.Diagram;
import biouml.model.DiagramElement;

import ru.biosoft.jobcontrol.AbstractJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlException;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.subaction.BackgroundDynamicAction;
import ru.biosoft.graphics.editor.ViewEditorHelper;

/**
 * @author lan
 *
 */
public class DiagramRemoveElementsAction extends BackgroundDynamicAction
{
    DiagramDynamicActionProperties properties = new DiagramDynamicActionProperties();

    @Override
    public JobControl getJobControl(final Object model, final List<DataElement> selectedItems, final Object properties) throws Exception
    {
        return new AbstractJobControl(log)
        {
            @Override
            protected void doRun() throws JobControlException
            {
                try
                {
                    ViewEditorHelper helper = ((DiagramDynamicActionProperties)properties).getHelper();
                    for(int i=0; i<selectedItems.size(); i++)
                    {
                        setPreparedness(i*100/selectedItems.size());
                        try
                        {
                            DataElement de = selectedItems.get(i);
                            if(de instanceof DiagramElement)
                            {
                                helper.removeView(((DiagramElement)de).getView());
                            }
                        }
                        catch( Exception e )
                        {
                            log.log( Level.SEVERE, e.getMessage(), e );
                        }
                    }
                    setPreparedness(100);
                    resultsAreReady(new Object[]{model});
                }
                catch( Exception e )
                {
                    throw new JobControlException(e);
                }
            }
        };
    }

    @Override
    public Object getProperties(Object model, List<DataElement> selectedItems)
    {
        return properties;
    }

    @Override
    public boolean isApplicable(Object model)
    {
        return model instanceof Diagram;
    }
}
