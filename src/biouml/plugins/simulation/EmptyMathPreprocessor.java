package biouml.plugins.simulation;

import java.util.HashSet;
import java.util.Set;

import biouml.model.Diagram;
import biouml.model.dynamics.Assignment;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.ExpressionOwner;
import one.util.streamex.StreamEx;

/**
 * Objects with empty math should be ignored (these are allowed in sbml l3v2)
 * @author Ilya
 */
public class EmptyMathPreprocessor extends Preprocessor
{

    @Override
    public boolean accept(Diagram diagram)
    {
        return diagram.getRole() instanceof EModel;
    }

    @Override
    public Diagram preprocess(Diagram diagram) throws Exception
    {
        for( ExpressionOwner owner : diagram.recursiveStream().map(de -> de.getRole()).select(ExpressionOwner.class) )
        {
            String[] expressions = owner.getExpressions();
            if( owner.getRole() instanceof Event )
            {
                Event event = (Event)owner.getRole();
                if (event.getTrigger().isEmpty())
                {
                    diagram.remove(event.getDiagramElement().getName());
                    continue;
                }
                Set<Assignment> toRemove = new HashSet<>();
                Assignment[] assignments = event.getEventAssignment();
                for( int i = 0; i < assignments.length; i++ )
                {
                    if( assignments[i].getMath().isEmpty() )
                        toRemove.add(assignments[i]);
                }
                removeAssignments(event, toRemove);
            }
            else
            {
                for( int i = 0; i < expressions.length; i++ )
                {
                    if( expressions[i] != null && expressions[i].isEmpty())
                            diagram.remove(owner.getRole().getDiagramElement().getName());
                }
                owner.setExpressions(expressions);
            }
        }
        return diagram;
    }

    /**Remove selected assignments from event*/
    private static void removeAssignments(Event event, Set<Assignment> toRemove)
    {
        event.setEventAssignment(StreamEx.of(event.getEventAssignment()).remove(a -> toRemove.contains(a)).toArray(Assignment[]::new));
    }
}