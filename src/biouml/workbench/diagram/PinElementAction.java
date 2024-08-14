package biouml.workbench.diagram;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import biouml.model.DiagramElement;


@SuppressWarnings ( "serial" )
public class PinElementAction extends AbstractAction
{
    public static final String KEY = "Pin";
    public static final String DIAGRAM_ELEMENT = "diagram element";

    public PinElementAction()
    {
        this( true );
    }

    public PinElementAction(boolean enabled)
    {
        super( KEY );
        setEnabled( enabled );
    }

    @Override
    public void actionPerformed(ActionEvent evt)
    {
        DiagramElement de = (DiagramElement)getValue( DIAGRAM_ELEMENT );
        de.setFixed(true);
    }
}
