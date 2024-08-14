
package biouml.plugins.simulation.plot;

import java.awt.BorderLayout;

import ru.biosoft.gui.Document;
import ru.biosoft.gui.ViewPartSupport;
import biouml.standard.simulation.plot.Plot;

/**
 * @author anna
 *
 */
public class PlotEditorViewPart extends ViewPartSupport
{
    private final PlotEditorPane editorPane;

    public PlotEditorViewPart()
    {
        editorPane = new PlotEditorPane();
        add(editorPane, BorderLayout.CENTER);

    }

    @Override
    public boolean canExplore(Object model)
    {
        if( model instanceof Plot )
            return true;
        return false;
    }

    @Override
    public void explore(Object model, Document document)
    {
        this.model = model;
        this.document = document;
        if( model instanceof Plot )
        {
            PlotEx plotEx = new PlotEx((Plot)model);
            if( document instanceof PlotDocument && ( (PlotDocument)document ).getDefaultSimulationResult() != null )
                plotEx.setDefaultSimulationResult( ( (PlotDocument)document ).getDefaultSimulationResult());
            editorPane.setPlot(plotEx);
        }
    }
}
