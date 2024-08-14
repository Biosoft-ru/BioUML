package biouml.plugins.simulation.plot;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.JOptionPane;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.table.TableDataCollection;
import biouml.plugins.simulation.resources.MessageBundle;
import biouml.standard.simulation.SimulationResult;
import biouml.standard.simulation.plot.DataGeneratorSeries;
import biouml.standard.simulation.plot.Plot;
import biouml.standard.simulation.plot.Series;

import com.developmentontheedge.beans.Option;

/**
 * It supports extended views of plot in property inspector.
 */
public class PlotEx extends Option
{
    protected static final Logger log = Logger.getLogger(PlotEx.class.getName());

    ////////////////////////////////////////////////////////////////////////////
    // Constructors
    //

    public PlotEx(Plot plot)
    {
        this.plot = plot;
        if( plot != null )
        {
            this.currentPlotPath = DataElementPath.create(plot);
            plot.setParent(this);
        }
    }

    public PlotEx(SimulationResult result)
    {
        this.plot = Plot.createDefaultPlot(null);
        this.currentPlotPath = DataElementPath.create(plot);
        this.defaultResult = result;

        plot.setParent(this);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Properties
    //

    protected Plot plot;
    public Plot getPlot()
    {
        return plot;
    }

    public void setPlot(Plot plot)
    {
        Plot oldPlot = this.plot;
        oldPlot.setParent(null);
        this.plot = plot;
        this.plot.setParent(this);

        firePropertyChange("plot", oldPlot, plot);
    }

    protected DataElementPath savePlotPath = null;
    public DataElementPath getSavePlotPath()
    {
        return savePlotPath;
    }
    public void setSavePlotPath(DataElementPath path)
    {
        savePlotPath = path;
    }

    protected DataElementPath currentPlotPath = null;
    public DataElementPath getCurrentPlotPath()
    {
        return currentPlotPath;
    }
    public void setCurrentPlotPath(DataElementPath path)
    {
        currentPlotPath = path;
        
        Plot oldPlot = plot;
        oldPlot.setParent(null);
        plot = path.getDataElement(Plot.class);
        plot.setParent(this);

        firePropertyChange("plot", oldPlot, plot);
    }

    protected SimulationResult defaultResult;
    public SimulationResult getDefaultSimulationResult()
    {
        return defaultResult;
    }

    public void setDefaultSimulationResult(SimulationResult defaultResult)
    {
        this.defaultResult = defaultResult;
    }

    public SimulationResult getSimulationResult(DataElementPath resultPath)
    {
        DataElement dc = resultPath.optDataElement();
        if( dc instanceof SimulationResult )
            return (SimulationResult)dc;
        return null;
    }

    public boolean hasDefaultResult()
    {
        return defaultResult != null;
    }

    public boolean isDefaultResult(String name)
    {
        return defaultResult.getName().equals(name);
    }

    //////////////////////////////////////////////////////////////////////////////
    // Actions
    //
    public boolean savePlot()
    {
        try
        {
            MessageBundle messageBundle = (MessageBundle)ResourceBundle.getBundle(MessageBundle.class.getName());

            if( savePlotPath == null )
            {
                String title = messageBundle.getResourceString("PLOT_PANE_PLOT_DC_ERROR_TITLE");
                String message = messageBundle.getResourceString("PLOT_PANE_PLOT_DC_ERROR_MESSAGE");
                JOptionPane.showMessageDialog(null, message, title, JOptionPane.YES_NO_OPTION);
                return false;
            }

            // check series for consistency
            List<Series> series = plot.getSeries();
            if( series != null )
            {
                for( Series s: series )
                {
                    if(s instanceof DataGeneratorSeries)
                    {
                        //TODO: check for consistency DataGeneratorSeries
                    }
                    else
                    {
                        String resultName = s.getSource();
                        if( s.getSourceNature().equals(Series.SourceNature.SIMULATION_RESULT) )
                        {
                            DataElement result = DataElementPath.create(s.getSource()).optDataElement();
                            if( result == null || ! ( result instanceof SimulationResult ) )
                            {
                                String title = messageBundle.getResourceString("PLOT_PANE_PLOT_INCONSISTENT_ERROR_TITLE");
                                String message = messageBundle.getResourceString("PLOT_PANE_PLOT_INCONSISTENT_SR_ERROR_MESSAGE");
                                message = MessageFormat.format(message, new Object[] {resultName});
                                JOptionPane.showMessageDialog(null, message, title, JOptionPane.YES_NO_OPTION);
                                return false;
                            }
                        }
                        else if( s.getSourceNature().equals(Series.SourceNature.EXPERIMENTAL_DATA) )
                        {
                            TableDataCollection experiment = DataElementPath.create(s.getSource()).optDataElement(TableDataCollection.class);
                            if( experiment == null )
                            {
                                String title = messageBundle.getResourceString("PLOT_PANE_PLOT_INCONSISTENT_ERROR_TITLE");
                                String message = messageBundle.getResourceString("PLOT_PANE_PLOT_INCONSISTENT_EDF_ERROR_MESSAGE");
                                message = MessageFormat.format(message, new Object[] {resultName});
                                JOptionPane.showMessageDialog(null, message, title, JOptionPane.YES_NO_OPTION);
                                return false;
                            }
                        }
                    }
                }
            }

            savePlot(plot, savePlotPath);
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "Error occured while saving plot in data collection: " + ex);
        }
        return true;
    }

    public static void savePlot(Plot plot, DataElementPath plotPath) throws Exception
    {
        // create new instance
        DataCollection parent = plotPath.getParentCollection();
        Plot plot_ = plot.clone(parent, plotPath.getName());
        parent.put(plot_);
        parent.release(plot_.getName());
    }

    public TableDataCollection getExperiment(DataElementPath experimentPath)
    {
        DataElement de = experimentPath.optDataElement();
        if( de instanceof TableDataCollection )
            return (TableDataCollection)de;
        return null;
    }
}
