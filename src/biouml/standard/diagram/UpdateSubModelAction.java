package biouml.standard.diagram;

import java.util.List;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.subaction.BackgroundDynamicAction;
import biouml.model.Diagram;
import biouml.model.SubDiagram;

import ru.biosoft.jobcontrol.AbstractJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlException;

@SuppressWarnings ( "serial" )
public class UpdateSubModelAction extends BackgroundDynamicAction
{
    @Override
    public boolean isApplicable(Object object)
    {
        return object instanceof Diagram && DiagramUtility.isComposite(( (Diagram)object ));
    }

    @Override
    public Object getProperties(Object model, List<DataElement> selectedItems)
    {
        return null;
    }

    boolean isApplicable(List<DataElement> selectedItems)
    {
        return selectedItems.size() >= 1 && selectedItems.get(0) instanceof SubDiagram;
    }

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
                    if( !isApplicable(selectedItems) )
                        return;

                    SubDiagram container = (SubDiagram)selectedItems.get(0);
                    updateSubmodel(container);
                }
                catch( Exception e )
                {
                    throw new JobControlException(e);
                }
            }
        };
    }

    private void updateSubmodel(SubDiagram subDiagram) throws Exception
    {
        DataElementPath path = subDiagram.getDiagram().getCompletePath();
        Diagram diagram = path.getDataElement(Diagram.class);

        for( SubDiagram innerSubDiagram : Util.getSubDiagrams(diagram) )
            updateSubmodel(innerSubDiagram);
        subDiagram.setDiagram(diagram);
        subDiagram.setView(null);
    }
}
