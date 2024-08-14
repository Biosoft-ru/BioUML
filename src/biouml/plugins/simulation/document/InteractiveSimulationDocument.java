package biouml.plugins.simulation.document;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JTabbedPane;

import biouml.model.dynamics.plot.PlotsInfo;
import biouml.model.dynamics.plot.Curve;
import biouml.model.dynamics.plot.PlotInfo;
import biouml.model.dynamics.plot.PlotVariable;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.gui.Document;

/**
 * @author axec
 *
 */
@ClassIcon ( "resources/simulationDocument.gif" )
public class InteractiveSimulationDocument extends Document implements PropertyChangeListener
{
    private final JTabbedPane tabbedPane;
    private InteractiveSimulation simulation;

    public InteractiveSimulationDocument(InteractiveSimulation simulation) throws IllegalArgumentException
    {
        super( simulation );
        viewPane = new ViewPane();
        
        this.simulation = simulation;
        simulation.doc = this;//TODO: use listener instead?
        tabbedPane = new JTabbedPane();
        updatePlots();
        
        simulation.addPropertyChangeListener( this );
        simulation.doSimulation();
    }
    
    public void updatePlots()
    {
        PlotsInfo plotsInfo = simulation.getPlots();
        if (plotsInfo == null)
            throw new IllegalArgumentException( "No plots specified for diagram. Can not create document!" );
        
        PlotInfo[] plotInfos = plotsInfo.getActivePlots();
        if (plotInfos.length == 0)
            throw new IllegalArgumentException( "No plots specified for diagram. Can not create document!" );

        Set<String> names = new HashSet<String>();
        
        tabbedPane.removeAll();
        for (PlotInfo plotInfo: plotInfos)  
        {
            tabbedPane.addTab( plotInfo.getTitle(), new SimplePlotPane( 700, 500, plotInfo, simulation.getEngine().getCompletionTime()) );  
            for( Curve c : plotInfo.getYVariables() )
                names.add( c.getCompleteName() );
            PlotVariable xVar = plotInfo.getXVariable();
            names.add( xVar.getCompleteName() );
        }
        simulation.setOutputNames( names );
        viewPane.add( tabbedPane );
        
        if( simulation.getResult() != null )
        {
            for( int i = 0; i < tabbedPane.getTabCount(); i++ )
            {
                ( (SimplePlotPane)tabbedPane.getComponentAt( i ) ).redrawChart( simulation.getResult() );
            }
        }
    }

    @Override
    public String getDisplayName()
    {
        return "Simulation: " + simulation.getDiagram().getName();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        for (int i=0; i< tabbedPane.getTabCount(); i++)
        {
            ((SimplePlotPane)tabbedPane.getComponentAt( i )).redrawChart( simulation.getResult() );
        }        
    }
}
