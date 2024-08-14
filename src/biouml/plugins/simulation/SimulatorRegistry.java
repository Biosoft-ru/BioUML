package biouml.plugins.simulation;

import one.util.streamex.EntryStream;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.IConfigurationElement;

import ru.biosoft.util.ExtensionRegistrySupport;

public class SimulatorRegistry extends ExtensionRegistrySupport<SimulatorRegistry.SolverInfo>
{
    private static final SimulatorRegistry instance = new SimulatorRegistry();
    private static Logger log = Logger.getLogger(SimulatorRegistry.class.getName());

    public static final String SIMULATION_TYPE = "type";
    public static final String NAME = "displayName";
    public static final String CLASS = "class";

    private Map<String, SolverInfo> solvers = new HashMap<>();

    private SimulatorRegistry()
    {
        super("biouml.plugins.simulation.solver", NAME);
    }

    public static EntryStream<String, String> registry(String type)
    {
        return instance.entries().filterValues( info -> type.equals( info.type ) ).mapValues( info -> info.className );
    }

    public static Simulator getSimulator(String simulatorName)
    {
        SolverInfo simulatorInfo = instance.getExtension(simulatorName);
        if( simulatorInfo != null )
        {
            try
            {
                Simulator solver = simulatorInfo.solverClass.newInstance();
                return solver;
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Can not get solver by name, error: " + e, e);
                return null;
            }
        }
        return null;
    }
    
    public static String getSolverName(Class clazz)
    {
        instance.init();
        SolverInfo info = instance.solvers.get( clazz.getName() );
        return info == null ? "Undefined" : info.name;
    }

    @Override
    protected SolverInfo loadElement(IConfigurationElement element, String name) throws Exception
    {
        SolverInfo newInfo = new SolverInfo();
        newInfo.type = getStringAttribute(element, SIMULATION_TYPE);
        newInfo.solverClass = getClassAttribute(element, CLASS, Simulator.class);
        newInfo.className = newInfo.solverClass.getName();
        newInfo.name = name;
        this.solvers.put( newInfo.className, newInfo );
        return newInfo;
    }

    static class SolverInfo
    {
        public String type;
        public Class<? extends Simulator> solverClass;
        public String className;
        public String name;
    }
}
