
package biouml.plugins.simulation.plot;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.repository.DataElementPathDialog;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.SaveDocumentAction;
import biouml.standard.simulation.SimulationResult;
import biouml.standard.simulation.plot.Plot;

import com.developmentontheedge.beans.swing.table.RowModelEvent;
import com.developmentontheedge.beans.swing.table.RowModelListener;
import com.developmentontheedge.application.Application;

/**
 * @author anna
 *
 */
@ClassIcon ( "resources/plotDocument.gif" )
public class PlotDocument extends Document
{
    private final PlotPane plotPane;
    private boolean isChanged;
    private final PlotChangeListener listener;
    private SimulationResult defaultSimulationResult;
    /**
     * @param plot
     */
    public PlotDocument(Plot plot)
    {
        super(plot);
        if( plot.getOrigin() == null )
            isChanged = true;
        viewPane = new ViewPane();
        plotPane = new PlotPane(700, 500);
        viewPane.add(plotPane);
        plotPane.setPlot(plot);
        plotPane.redrawChart();
        listener = new PlotChangeListener();
        plot.addPropertyChangeListener(listener);
        plot.getRowModel().addRowModelListener(listener);
    }

    @Override
    public String getDisplayName()
    {
        Plot plot = getPlot();
        if( plot.getOrigin() != null )
            return plot.getOrigin().getName() + " : " + plot.getName();
        return plot.getName();
    }

    @Override
    protected void doUpdate()
    {
        plotPane.scheduleRedraw();
    }

    @Override
    public boolean isChanged()
    {
        return isChanged;
    }

    @Override
    public boolean isMutable()
    {
        Plot plot = getPlot();
        if( plot != null && ( plot.getOrigin() == null || ( plot.getOrigin() != null && plot.getOrigin().isMutable() ) ) )
            return true;
        return false;
    }

    private Plot getPlot()
    {
        return (Plot)getModel();
    }

    @Override
    public void save()
    {
        Plot plot = getPlot();
        if( plot.getOrigin() != null )
        {
            DataElementPath path = DataElementPath.create(plot);
            try
            {
                path.save(plot);
                isChanged = false;
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Saving error", e);
            }
        }
        else
        {
            DataElementPathDialog dialog = new DataElementPathDialog("Save plot document as");
            DataElementPath plotPath = null;
            if( defaultSimulationResult != null && defaultSimulationResult.getOrigin() != null )
                plotPath = DataElementPath.create(defaultSimulationResult.getOrigin(), plot.getName());
            dialog.setValue(plotPath);
            dialog.setElementClass(Plot.class);
            dialog.setPromptOverwrite(true);
            if( dialog.doModal() )
            {
                try
                {
                    DataElementPath path = dialog.getValue();
                    PlotEx.savePlot(plot, path);
                    isChanged = false;
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, "Saving error", e);
                }
            }
        }
    }

    @Override
    public void updateActionsState()
    {
        if( isChanged )
            Application.getActionManager().enableActions( true, SaveDocumentAction.KEY );
    }

    private class PlotChangeListener implements PropertyChangeListener, RowModelListener
    {
        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            update();
            isChanged = true;
            updateActionsState();
        }

        @Override
        public void tableChanged(RowModelEvent e)
        {
            update();
            isChanged = true;
            updateActionsState();
        }

    }

    public SimulationResult getDefaultSimulationResult()
    {
        return defaultSimulationResult;
    }

    public void setDefaultSimulationResult(SimulationResult defaultSimulationResult)
    {
        this.defaultSimulationResult = defaultSimulationResult;
    }

    @Override
    public void close()
    {
        Plot plot = getPlot();
        plot.removePropertyChangeListener(listener);
        if( plot.getOrigin() != null )
        {
            plot.getOrigin().release(plot.getName());
        }
        super.close();
    }



}
