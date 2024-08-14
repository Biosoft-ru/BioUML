package biouml.model;

import java.util.logging.Level;
import java.util.Iterator;

import biouml.standard.type.Base;

/**
 * Convert diagram adding EModel and necessary roles for simulation
 */
public class PathwaySimulationDiagramTypeConverter extends DiagramTypeConverterSupport
{
    @Override
    protected Diagram convert(DiagramType diagramType, Diagram diagram) throws Exception
    {
        Diagram newDiagram = diagramType.createDiagram(diagram.getOrigin(), diagram.getName(), null);
        processCompartment(newDiagram, newDiagram, diagram, Node.class);
        processCompartment(newDiagram, newDiagram, diagram, Edge.class);
        return newDiagram;
    }

    protected void processCompartment(Diagram diagram, Compartment newCompartment, Compartment oldCompartment, Class<? extends DiagramElement> deType)
    {
        Iterator<DiagramElement> iter = oldCompartment.iterator();
        while( iter.hasNext() )
        {
            DiagramElement de = iter.next();
            if( !deType.isInstance(de) )
                continue;
            try
            {
                DiagramElement[] newElements = convertDiagramElement(de, diagram);
                if( newElements != null )
                {
                    for( DiagramElement newElement : newElements )
                    {
                        newCompartment.put(newElement);
                        if( ( newElement instanceof Compartment ) && ( de instanceof Compartment ) )
                        {
                            processCompartment(diagram, (Compartment)newElement, (Compartment)de, deType);
                        }
                    }
                }
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Cannot convert element: " + de.getName(), e);
            }
        }
    }

    @Override
    public DiagramElement[] convertDiagramElement(DiagramElement de, Diagram diagram) throws Exception
    {
        Compartment parent;
        Compartment oldParent = (Compartment)de.getOrigin();
        if( oldParent instanceof Diagram )
        {
            parent = diagram;
        }
        else
        {
            DiagramElement parentDe = diagram.findDiagramElement(oldParent.getCompleteNameInDiagram());
            parent = ( parentDe == null ) ? null : ( (Compartment)parentDe );
        }
        if( de instanceof Node )
        {
            if( isElementAvailable(de, diagram.getType().getNodeTypes()) )
            {
                DiagramElement newNode = de.clone(parent, de.getName());
                newNode = diagram.getType().getSemanticController().validate(parent, newNode);
                return new DiagramElement[] {newNode};
            }
            else
            {
                log.log(Level.SEVERE, "Node type not supported in new diagram type: " + de.getName());
            }
        }
        else if( de instanceof Edge )
        {
            if( isElementAvailable(de, diagram.getType().getEdgeTypes()) )
            {
                DiagramElement newInput = diagram.findDiagramElement( ( (Edge)de ).getInput().getCompleteNameInDiagram());
                DiagramElement newOutput = diagram.findDiagramElement( ( (Edge)de ).getOutput().getCompleteNameInDiagram());
                if( ( newInput != null ) && ( newOutput != null ) )
                {
                    DiagramElement newEdge = new Edge(parent, ( (Edge)de ).getKernel(), (Node)newInput, (Node)newOutput);
                    newEdge = diagram.getType().getSemanticController().validate(parent, newEdge);
                    return new DiagramElement[] {newEdge};
                }
            }
            else
            {
                log.log(Level.SEVERE, "Edge type not supported in new diagram type: " + de.getName());
            }
        }
        return null;
    }

    protected boolean isElementAvailable(DiagramElement de, Object[] classes)
    {
        Class<? extends Base> type = de.getKernel().getClass();
        for( Object obj : classes )
        {
            if( obj instanceof Class && ( (Class<?>)obj ).isAssignableFrom(type) )
            {
                return true;
            }
        }
        return false;
    }
}
