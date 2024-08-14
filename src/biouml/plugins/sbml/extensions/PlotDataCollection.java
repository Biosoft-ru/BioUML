package biouml.plugins.sbml.extensions;

import java.io.File;
import java.util.logging.Level;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;

import one.util.streamex.StreamEx;

import java.util.logging.Logger;

import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.Index;
import ru.biosoft.gui.Document;
import biouml.model.Diagram;
import biouml.model.Module;
import biouml.standard.simulation.plot.Plot;
import biouml.standard.simulation.plot.Series;
import biouml.workbench.diagram.DiagramDocument;

import com.developmentontheedge.beans.DynamicProperty;

public class PlotDataCollection extends AbstractDataCollection<Plot>
{
    protected static final Logger log = Logger.getLogger(PlotDataCollection.class.getName());

    protected Index namesMap = null;

    public PlotDataCollection(DataCollection<?> parent, Properties properties)
    {
        super(parent, properties);
        try
        {
            namesMap = new DiagramIndex(new File(properties.getProperty("configPath")), "plot.index");
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "can't create index", t);
        }
    }

    protected boolean isInitNameList = false;

    protected void initNameList()
    {
        if( isInitNameList )
            return;

        isInitNameList = true;

        if( !namesMap.isValid() )
        {
            try
            {
                DataCollection<Diagram> diagrams = Module.getModule(this).getDiagrams();
                for( Diagram diagram : diagrams )
                {
                    Object object = diagram.getAttributes().getValue(PlotExtension.DIAGRAM_PLOTS_PROPERTY);
                    if( object != null && ( object instanceof Plot[] ) )
                    {
                        for( Plot plot : (Plot[])object )
                        {
                            namesMap.put(plot.getName(), diagram.getName());
                        }
                    }
                }
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "can't initialize collection " + getName(), t);
            }
        }
    }

    @Override
    public @Nonnull List<String> getNameList()
    {
        return names().toList();
    }
    
    @Override
    public StreamEx<String> names()
    {
        initNameList();
        return StreamEx.ofKeys( namesMap );
    }

    @Override
    protected Plot doGet(String name)
    {
        if( namesMap.containsKey(name) )
        {
            try
            {
                Diagram diagram = Module.getModule(this).getDiagram((String)namesMap.get(name));
                Object object = diagram.getAttributes().getValue(PlotExtension.DIAGRAM_PLOTS_PROPERTY);
                if( object != null && ( object instanceof Plot[] ) )
                {
                    for( Plot plot : (Plot[])object )
                    {
                        if( plot.getName().equals(name) )
                        {
                            return plot;
                        }
                    }
                }
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "can't get element " + name + " from " + getName(), t);
            }
        }
        return null;
    }

    @Override
    protected void doPut(Plot plot, boolean isNew) throws Exception
    {
        Document document = Document.getCurrentDocument();

        Diagram diagram = null;
        if( document != null && ( document instanceof DiagramDocument ) )
        {
            // by default use current diagram
            diagram = ( (DiagramDocument)document ).getDiagram();
        }
        else
        {
            // if current diagram is null try to find diagram with experiment from first series of plot
            List<Series> series = plot.getSeries();
            if( series != null && series.size() > 0 )
            {
                DataCollection resultsDC = Module.getModule(this);
                try
                {
                    resultsDC = (DataCollection)resultsDC.get(Module.SIMULATION);
                    resultsDC = (DataCollection)resultsDC.get(Module.RESULT);
                }
                catch( Exception e )
                {
                    return;
                }
                if( resultsDC instanceof ResultDataCollection
                        && series.get(0).getSourceNature().equals(Series.SourceNature.SIMULATION_RESULT) )
                {
                    String diagramName = ( (ResultDataCollection)resultsDC ).getDiagramNameByExperiment(series.get(0).getSource());
                    diagram = Module.getModule(this).getDiagram(diagramName);
                }
            }
        }
        if(diagram == null) return;

        Object object = diagram.getAttributes().getValue(PlotExtension.DIAGRAM_PLOTS_PROPERTY);
        Plot[] newPlots = null;
        if( object != null && ( object instanceof Plot[] ) )
        {
            //add plot to current list
            Plot[] oldSimulations = (Plot[])object;
            newPlots = new Plot[oldSimulations.length + 1];
            System.arraycopy(oldSimulations, 0, newPlots, 0, oldSimulations.length);
            newPlots[oldSimulations.length] = plot;
            diagram.getAttributes().getProperty(PlotExtension.DIAGRAM_PLOTS_PROPERTY).setValue(newPlots);
        }
        else
        {
            //create new plots list
            newPlots = new Plot[1];
            newPlots[0] = plot;
            try
            {
                diagram.getAttributes().add(new DynamicProperty(PlotExtension.DIAGRAM_PLOTS_PROPERTY, Plot[].class, newPlots));
            }
            catch( Exception e )
            {
            }
        }
        namesMap.put(plot.getName(), diagram.getName());
    }

    @Override
    protected void doRemove(String name) throws Exception
    {
        if( namesMap.containsKey(name) )
        {
            try
            {
                DataCollection<Diagram> diagramsDC = Module.getModule(this).getDiagrams();
                Diagram diagram = diagramsDC.get((String)namesMap.get(name));
                Object object = diagram.getAttributes().getValue(PlotExtension.DIAGRAM_PLOTS_PROPERTY);
                if( object != null && ( object instanceof Plot[] ) )
                {
                    int pos = 0;
                    Plot[] newSimulations = new Plot[ ( (Plot[])object ).length - 1];
                    for( Plot plot : (Plot[])object )
                    {
                        if( !plot.getName().equals(name) )
                        {
                            newSimulations[pos++] = plot;
                        }
                    }
                    diagram.getAttributes().getProperty(PlotExtension.DIAGRAM_PLOTS_PROPERTY).setValue(newSimulations);
                    namesMap.remove(name);
                    diagramsDC.put(diagram);
                }
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "can't remove element " + name + " from " + getName(), t);
            }
        }
    }

    @Override
    public int getSize()
    {
        initNameList();
        return namesMap.size();
    }

    @Override
    public boolean contains(String name)
    {
        return namesMap.containsKey(name);
    }

    @Override
    public void close() throws Exception
    {
        super.close();

        if( namesMap != null )
        {
            namesMap.close();
            namesMap = null;
        }
    }
}
