package biouml.workbench.diagram.action;

import java.util.ArrayList;
import java.util.List;

import biouml.model.Diagram;
import biouml.model.Node;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.subaction.BackgroundDynamicAction;
import ru.biosoft.jobcontrol.AbstractJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlException;

public abstract class ProcessNodesAction extends BackgroundDynamicAction
{
    @Override
    public boolean isApplicable(Object model) //TODO: find more clever way to disable action for certain diagrams
    {
        return model instanceof Diagram && !((Diagram)model).getType().getClass().toString().endsWith("AnnotationDiagramType");
    }

    @Override
    public JobControl getJobControl(final Object model, final List<DataElement> selectedItems, Object properties) throws Exception
    {
        return new AbstractJobControl(log)
        {
            @Override
            protected void doRun() throws JobControlException
            {
                Diagram diagram = (Diagram)model;
                List<Node> nodes = new ArrayList<>();
                for(DataElement de : selectedItems)
                {
                    if(de instanceof Node)
                    {
                        nodes.add( (Node)de );
                    }
                }
                if(nodes.isEmpty())
                    return;
                processNodes(diagram, nodes);
                setPreparedness( 100 );
                resultsAreReady(new Object[]{model});
            }
        };
    }
    
    abstract void processNodes(Diagram diagram, List<Node> nodes); 
}
