package biouml.plugins.sbml;

import java.util.logging.Logger;
import org.w3c.dom.Element;

import biouml.model.DiagramType;
import biouml.model.Node;
import biouml.model.dynamics.Event;

public class SbmlModelReader_24 extends SbmlModelReader_23
{
    public SbmlModelReader_24()
    {
        log = Logger.getLogger(SbmlModelReader_24.class.getName());
    }

    @Override
    protected DiagramType getDiagramType(Element modelElement)
    {
        return new SbmlDiagramType_L2v4();
    }

    @Override
    public Node readEvent(Element element, int i)
    {
        Node node = super.readEvent( element, i );
        if( node != null )
        {
            Event event = node.getRole( Event.class );
            event.setUseValuesFromTriggerTime( readOptionalBoolean( element, EVENT_USE_VALUES_FROM_TRIGGER_TIME_ATTR, true) );
        }
        return node;
    }
}
