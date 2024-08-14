package biouml.plugins.agentmodeling.simulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.model.SubDiagram;
import biouml.model.dynamics.EModel;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.Util;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.SimulationEngineRegistry;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import one.util.streamex.StreamEx;

@PropertyName ( "Module group" )
@PropertyDescription ( "Module group." )
public class ModuleGroup
{
    public static final String TOP_LEVEL = "<<TOP LEVEL>>";
    private String name;
    private SimulationEngine engine = new JavaSimulationEngine();
    private String engineName = "ODE Simulation Engine";
    private boolean simulateSeparately = false;
    private List<String> appropriateEngineNames = null;

    /**
     * Base diagram
     */
    private Diagram diagram;

    /**
     * Subdiagrams from base diagTram which should be simulated with given engine
     */
    private String[] subdiagrams = new String[0];

    public ModuleGroup()
    {
        //empty constructor for desearilzation purposes
    }

    public void initDiagram(Diagram diagram)
    {
        this.diagram = diagram;
        this.appropriateEngineNames = null;
        if( engine != null )
            engine.setDiagram( diagram );
    }

    public ModuleGroup(String name, String[] subdiagrams)
    {
        this.name = name;
        this.setEngineName( "ODE Simulation Engine" ); //default
        this.setSubdiagrams( subdiagrams );
    }

    @PropertyName ( "Group name" )
    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        this.name = name;
    }

    @PropertyName ( "Simulation engine name" )
    public String getEngineName()
    {
        return engineName;
    }

    public void setEngineName(String engineName)
    {
        this.engineName = engineName;
        setEngine( SimulationEngineRegistry.getSimulationEngine( engineName ) );
    }

    @PropertyName ( "Simulation engine" )
    public SimulationEngine getEngine()
    {
        return engine;
    }

    public void setEngine(SimulationEngine engine)
    {
        this.engine = engine;
        if( diagram != null )
            this.engine.setDiagram( diagram );
    }

    @PropertyName ( "Simulate separately" )
    @PropertyDescription ( "If true then each subdiagram will be simulated as separate agent using its own copy of simulation engine." )
    public boolean isSimulateSeparately()
    {
        return simulateSeparately;
    }

    public void setSimulateSeparately(boolean simulateSeparately)
    {
        this.simulateSeparately = simulateSeparately;
    }

    @PropertyName ( "Modules" )
    public String[] getSubdiagrams()
    {
        return subdiagrams;
    }
    public void setSubdiagrams(String[] subdiagrams)
    {
        this.subdiagrams = subdiagrams;
        this.appropriateEngineNames = null;     
    }

    private void updateAppropriateEngineNames()
    {
        List<String> result = new ArrayList<>();

        if( subdiagrams == null )
        {
            appropriateEngineNames = result;
            return;
        }

        StreamEx<Diagram> subDiagramElements = StreamEx.of( subdiagrams )
                .map( s -> s.equals( TOP_LEVEL ) ? diagram : ( (SubDiagram)diagram.findNode( s ) ).getDiagram() );
        List<EModel> emodels = subDiagramElements.map( d -> d.getRole() ).select( EModel.class ).toList();

        result = StreamEx.of( SimulationEngineRegistry.getSimulationEngineNames( emodels.get( 0 ) ) ).toList();
        for( int i = 1; i < emodels.size(); i++ )
            result.retainAll( StreamEx.of( SimulationEngineRegistry.getSimulationEngineNames( emodels.get( i ) ) ).toSet() );
        appropriateEngineNames = result;

        for( String name : appropriateEngineNames )
        {
            if( name.equals( engineName ) )
                return;
        }

        if( appropriateEngineNames.size() > 0 )
            setEngineName( appropriateEngineNames.get( 0 ) );
    }

    public Stream<String> getAppropriateEngineNames()
    {
        if( appropriateEngineNames == null )
            updateAppropriateEngineNames();
        return StreamEx.of( appropriateEngineNames );
    }

    public String[] getAvailableSubDiagrams()
    {
        if( diagram == null )
            return new String[] {};
        return StreamEx.of( Util.getSubDiagrams( diagram ) ).map( s -> s.getName() ).prepend( TOP_LEVEL ).toArray( String[]::new );
    }

    /**
     * 
     * @param diagram
     * @return
     */
    public static ModuleGroup[] generateModules(Diagram diagram)
    {
        if( !DiagramUtility.containModules( diagram ) ) //diagram does not contain modules - only one module group with top level can be
            return new ModuleGroup[] {new ModuleGroup( "Simulation Options", new String[] {TOP_LEVEL} )};

        List<SubDiagram> subdiagrams = Util.getSubDiagrams( diagram );
        Map<String, String[]> subDiagramToEngines = StreamEx.of( subdiagrams ).toMap( s -> s.getName(),
                s -> SimulationEngineRegistry.getSimulationEngineNames( s.getDiagram().getRole( EModel.class ) ) );

        subDiagramToEngines.put( TOP_LEVEL, SimulationEngineRegistry.getSimulationEngineNames( diagram.getRole( EModel.class ) ) );

        //Order engines according to number of subdiagrams, they are applicable for
        Map<String, Integer> engineToCount = new HashMap<>();
        for( Entry<String, String[]> entry : subDiagramToEngines.entrySet() )
            for( String engine : entry.getValue() )
                engineToCount.compute( engine, (k, v) -> v == null ? 1 : v + 1 );

        Map<String, Set<String>> subDiagramToEngine = new HashMap<>();
        Set<String> subDiagramNames = StreamEx.of( subdiagrams ).map( s -> s.getName() ).prepend( TOP_LEVEL ).toSet();
        for( String engine : StreamEx.of( engineToCount.entrySet() ).sortedByInt( e -> e.getValue() ).map( e -> e.getKey() ) )
        {
            for( Entry<String, String[]> entry : subDiagramToEngines.entrySet() )
            {
                String subDiagram = entry.getKey();
                if( !subDiagramNames.contains( subDiagram ) ) //this subDiagram is already associated with engine
                    continue;

                if( StreamEx.of( entry.getValue() ).toSet().contains( engine ) )//this engine is applicable to this module - assign it
                {
                    subDiagramToEngine.computeIfAbsent( engine, k -> new HashSet<String>() ).add( subDiagram );
                    subDiagramNames.remove( entry.getKey() ); //remove from free subdiagrams
                }
            }
        }

        int i = 0;
        ModuleGroup[] result = new ModuleGroup[subDiagramToEngine.size()];

        for( Entry<String, Set<String>> entry : subDiagramToEngine.entrySet() )
        {
            String name = result.length == 1 ? "Simulation Options" : "Simulation options " + i + 1;
            result[i] = new ModuleGroup( name, StreamEx.of( entry.getValue() ).toArray( String[]::new ) );
            i++;
        }
        return result;
    }
}
