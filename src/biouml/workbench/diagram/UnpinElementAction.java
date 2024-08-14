package biouml.workbench.diagram;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import biouml.model.DiagramElement;


@SuppressWarnings ( "serial" )
public class UnpinElementAction extends AbstractAction
{
    public static final String KEY = "Unpin";
    public static final String DIAGRAM_ELEMENT = "diagram element";
    public static final String VIEWPANE = "viewPane";

    public UnpinElementAction()
    {
        super( KEY );
        setEnabled( true );
    }

    @Override
    public void actionPerformed(ActionEvent evt)
    {
        DiagramElement de = (DiagramElement)getValue( DIAGRAM_ELEMENT );
        de.setFixed(false);
    }
}
