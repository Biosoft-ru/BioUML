package biouml.workbench.diagram;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import biouml.model.Diagram;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.gui.Document;

public class FitToScreenAction extends AbstractAction
{
    public static final String KEY = "Fit to screen";

    public FitToScreenAction(boolean enabled)
    {
        super( KEY );
        setEnabled( enabled );
    }

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
        Document activeDocument = Document.getActiveDocument();
        if( activeDocument != null && activeDocument instanceof DiagramDocument )
        {
            ViewPane viewPane = ( (DiagramDocument)activeDocument ).getDiagramViewPane();
            Diagram diagram = ( (DiagramDocument)activeDocument ).getDiagram();
            Rectangle bounds = diagram.getView().getBounds();
            try
            {
                if( viewPane != null )
                {
                    double scale = Math.min( viewPane.getWidth() / bounds.getWidth(), viewPane.getHeight() / bounds.getHeight() ) * 0.95;
                    viewPane.scale( scale / viewPane.getScaleX(), scale / viewPane.getScaleY() );
                }
            }
            catch( Exception e )
            {
            }
        }

    }

}
