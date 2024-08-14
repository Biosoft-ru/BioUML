package biouml.plugins.sbml.extensions;

import java.io.File;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;

import java.util.logging.Logger;

import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.Index;
import ru.biosoft.gui.Document;
import biouml.model.Diagram;
import biouml.model.Module;
import biouml.plugins.sbml.extensions.SimulationExtension.Simulation;
import biouml.standard.simulation.SimulationResult;
import biouml.workbench.diagram.DiagramDocument;

import com.developmentontheedge.beans.DynamicProperty;

public class ResultDataCollection extends AbstractDataCollection<SimulationResult>
{
    protected static final Logger log = Logger.getLogger(ResultDataCollection.class.getName());

    protected Index namesMap = null;

    public ResultDataCollection(DataCollection<?> parent, Properties properties)
    {
        super(parent, properties);
        try
        {
            namesMap = new DiagramIndex(new File(properties.getProperty("configPath")), "result.index");
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
                for( Diagram diagram : Module.getModule(this).getDiagrams() )
                {
                    Object object = diagram.getAttributes().getValue(SimulationExtension.DIAGRAM_SIMULATIONS_PROPERTY);
                    if( object instanceof SimulationExtension.Simulation[] )
                    {
                        for( SimulationExtension.Simulation simulation : (SimulationExtension.Simulation[])object )
                        {
                            namesMap.put(simulation.getName(), diagram.getName());
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
        initNameList();
        return new ArrayList<>( namesMap.keySet() );
    }

    @Override
    protected SimulationResult doGet(String name)
    {
        if( namesMap.containsKey(name) )
        {
            try
            {
                Diagram diagram = Module.getModule(this).getDiagram((String)namesMap.get(name));
                Object object = diagram.getAttributes().getValue(SimulationExtension.DIAGRAM_SIMULATIONS_PROPERTY);
                if( object instanceof SimulationExtension.Simulation[] )
                {
                    for( SimulationExtension.Simulation simulation : (SimulationExtension.Simulation[])object )
                    {
                        if( simulation.getName().equals(name) )
                        {
                            return simulation.getResult();
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
    protected void doPut(SimulationResult simulationResult, boolean isNew) throws Exception
    {
        SimulationExtension.Simulation simulation = new SimulationExtension.Simulation(null, simulationResult.getName());
        simulation.setTitle(simulationResult.getTitle());
        simulation.setResult(simulationResult);
        
        Document document = Document.getCurrentDocument();
        if( document instanceof DiagramDocument )
        {
            Diagram diagram = ( (DiagramDocument)document ).getDiagram();
            Object object = diagram.getAttributes().getValue(SimulationExtension.DIAGRAM_SIMULATIONS_PROPERTY);
            Simulation[] newSimulations = null;
            if( object != null && ( object instanceof Simulation[] ) )
            {
                //add experiment to current list
                Simulation[] oldSimulations = (Simulation[])object;
                newSimulations = new Simulation[oldSimulations.length + 1];
                System.arraycopy(oldSimulations, 0, newSimulations, 0, oldSimulations.length);
                newSimulations[oldSimulations.length] = simulation;
                diagram.getAttributes().getProperty(SimulationExtension.DIAGRAM_SIMULATIONS_PROPERTY).setValue(newSimulations);
            }
            else
            {
                //create new experiments list
                newSimulations = new Simulation[1];
                newSimulations[0] = simulation;
                try
                {
                    diagram.getAttributes().add(
                            new DynamicProperty(SimulationExtension.DIAGRAM_SIMULATIONS_PROPERTY, Simulation[].class, newSimulations));
                }
                catch( Exception e )
                {
                }
            }
            namesMap.put(simulationResult.getName(), diagram.getName());
        }
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
                Object object = diagram.getAttributes().getValue(SimulationExtension.DIAGRAM_SIMULATIONS_PROPERTY);
                if( object != null && ( object instanceof SimulationExtension.Simulation[] ) )
                {
                    int pos = 0;
                    Simulation[] newSimulations = new Simulation[ ( (SimulationExtension.Simulation[])object ).length - 1];
                    for( SimulationExtension.Simulation simulation : (SimulationExtension.Simulation[])object )
                    {
                        if( !simulation.getName().equals(name) )
                        {
                            newSimulations[pos++] = simulation;
                        }
                    }
                    diagram.getAttributes().getProperty(SimulationExtension.DIAGRAM_SIMULATIONS_PROPERTY).setValue(newSimulations);
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

    public String getDiagramNameByExperiment(String experimentId)
    {
        return (String)namesMap.get(experimentId);
    }
}
