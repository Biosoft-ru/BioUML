package biouml.plugins.physicell.document;

import javax.swing.JScrollPane;

import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.gui.Document;

/**
 * @author axec
 *
 */
//@ClassIcon ( "resources/simulationDocument.gif" )
public class PhysicellResultDocument extends Document
{
    public PhysicellResultDocument(PhysicellSimulationResult result) throws IllegalArgumentException
    {
        super( result );
        viewPane = new ViewPane();
        Render2DPanel renderPanel = new Render2DPanel( 1500, 1500, result );
        JScrollPane scrollPane = new JScrollPane( renderPanel );
        scrollPane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS );
        scrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS );
        viewPane.add( scrollPane );
        result.init();
        renderPanel.read( result.getPoint( result.getOptions().getTime() ).getContent() );
        renderPanel.update();
    }

    @Override
    public String getDisplayName()
    {
        return "Simulation result";
    }
}