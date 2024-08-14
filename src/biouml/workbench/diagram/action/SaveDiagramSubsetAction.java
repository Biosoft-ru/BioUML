package biouml.workbench.diagram.action;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.subaction.BackgroundDynamicAction;
import ru.biosoft.jobcontrol.AbstractJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlException;

public class SaveDiagramSubsetAction extends BackgroundDynamicAction
{
    @Override
    public boolean isApplicable(Object model) //TODO: find more clever way to disable action for certain diagrams
    {
        return model instanceof Diagram && !((Diagram)model).getType().getClass().toString().endsWith("AnnotationDiagramType");
    }

    @Override
    public JobControl getJobControl(final Object model, final List<DataElement> selectedItems, Object properties) throws Exception
    {
        final DataElementPath target = (DataElementPath)(((DynamicPropertySet)properties).getValue( "target" ));
        return new AbstractJobControl(log)
        {
            @Override
            protected void doRun() throws JobControlException
            {
                try
                {
                    Diagram diagram = (Diagram)model;
                    Diagram result = diagram.clone(target.getParentCollection(), target.getName() );
                    setPreparedness( 50 );
                    Set<String> names = new HashSet<>();
                    for(DataElement item : selectedItems)
                    {
                        names.add( item.cast( DiagramElement.class ).getCompleteNameInDiagram() );
                    }
                    setPreparedness( 52 );
                    retainElements(result, names);
                    setPreparedness( 80 );
                    CollectionFactoryUtils.save( result );
                    setPreparedness( 100 );
                    resultsAreReady(new Object[]{result});
                }
                catch( Exception e )
                {
                    throw new JobControlException( e );
                }
            }

            private void retainElements(Compartment compartment, Set<String> retain) throws Exception
            {
                for(String name : compartment.getNameList().toArray( new String[compartment.getSize()] ))
                {
                    DiagramElement de = compartment.get( name );
                    if( de == null || de instanceof Edge || retain.contains( de.getCompleteNameInDiagram() ))
                        continue;
                    if(de instanceof Compartment)
                    {
                        Compartment c = (Compartment)de;
                        if(c.getSize() > 0)
                        {
                            retainElements( c, retain );
                            if(c.getSize() > 0)
                                continue;
                        }
                    }
                    if(de instanceof Node)
                    {
                        Node n = (Node)de;
                        for(Edge e : n.getEdges())
                        {
                            e.getOrigin().remove( e.getName() );
                        }
                    }
                    compartment.remove( de.getName() );
                }
            }
        };
    }

    @Override
    public Object getProperties(Object model, List<DataElement> selectedItems)
    {
        return getTargetProperties( Diagram.class, DataElementPath.create( ( (Diagram)model ).getCompletePath() + " subset" ).uniq() );
    }
}
