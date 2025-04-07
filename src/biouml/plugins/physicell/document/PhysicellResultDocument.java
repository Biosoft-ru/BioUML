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
    public PhysicellResultDocument(PhysicellSimulationResult result) throws Exception
    {
        super( result );
        viewPane = new ViewPane();
        RenderPanel renderPanel = new RenderPanel( 1500, 1500, result );
        JScrollPane scrollPane = new JScrollPane( renderPanel );
        scrollPane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS );
        scrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS );
        viewPane.add( scrollPane );

        if( result.getOptions().isCells() )
            renderPanel.readAgents( result.getPoint( result.getOptions().getTime() ) );
        if( result.getOptions().isDrawDensity() && result.hasDensity())
            renderPanel.readDensity( result.getDensity( result.getOptions().getTime(), result.getOptions().getSubstrate() ) );
        renderPanel.update();
    }

    @Override
    public String getDisplayName()
    {
        return "Simulation result";
    }
}