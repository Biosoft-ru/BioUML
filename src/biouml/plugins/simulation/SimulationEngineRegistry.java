package biouml.plugins.simulation;

import java.util.logging.Level;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import java.util.logging.Logger;
import org.eclipse.core.runtime.IConfigurationElement;
import ru.biosoft.util.ExtensionRegistrySupport;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;

public class SimulationEngineRegistry extends ExtensionRegistrySupport<SimulationEngineRegistry.EngineInfo>
{
    private static final SimulationEngineRegistry instance = new SimulationEngineRegistry();
    private static Logger log = Logger.getLogger(SimulationEngineRegistry.class.getName());

    private static final String NAME = "displayName";
    private static final String CLASS = "class";
    private static final String EMODEL_TYPE = "emodelType";
    private static final String PRIORITY = "priority";

    private SimulationEngineRegistry()
    {
        super("biouml.plugins.simulation.engine", NAME);
    }

    @Override
    protected EngineInfo loadElement(IConfigurationElement element, String name) throws Exception
    {
        Class<? extends SimulationEngine> engineClass = getClassAttribute(element, CLASS, SimulationEngine.class);
        String type = getStringAttribute(element, EMODEL_TYPE);
    
        String[] priority = getStringAttribute(element, PRIORITY).split(",");
        double[] prior = new double[priority.length];
        try
        {
            for( int j = 0; j < priority.length; j++ )
            {
                prior[j] = Double.parseDouble(priority[j]);
            }
        }
        catch( Exception ex )
        {
            throw new Exception("Priority absents or not a number");
        }
    
        return new EngineInfo(name, engineClass, type.split(","), prior);
    }

    public static String[] getSimulationEngineNames(EModel emodel)
    {
        List<EngineInfo> infos = getSimulationEngineInfos(emodel.getType());
        List<String> names = new ArrayList<>();
        for( EngineInfo info : infos )
            names.add(info.getName());
        return names.toArray(new String[names.size()]);
    }
    
    public static String[] getAllSimulationEngineNames()
    {
        return instance.stream().map( e->e.getName() ).toArray( String[]::new );
    }

    public static String getSimulationEngineName(SimulationEngine engine)
    {
        Class<? extends SimulationEngine> clazz = engine.getClass();
        for( EngineInfo info : instance )
        {
            if( info.getEngineClass().equals(clazz) )
                return info.getName();
        }
        return null;

    }
    /**
     * Return names of all simulation engines which are compatible with <b>model</b> type
     * @param model
     * @return
     */
    private static List<EngineInfo> getSimulationEngineInfos(String emodelType)
    {
        try
        {
            return instance.stream().filter( info -> info.accepts( emodelType ) ).sorted( new EngineComparator( emodelType ) ).toList();
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "Simulation engines list can not be created. " + ex);
        }
        return Collections.emptyList();
    }

    /**
     * 
     * @param name
     * @return created instance of SimulationEngine by its <b>name</b><br>
     * Note: name is not equal to output of SimulationeEngine.getEngineName()<br>
     * it is set in plugin.xml
     */
    public static SimulationEngine getSimulationEngine(String name)
    {
        try
        {
            return instance.getExtension(name).getEngineClass().newInstance();
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "Simulation engine " + name + " can not be created. " + ex);
        }
        return null;
    }

    public static SimulationEngine getSimulationEngine(Diagram diagram)
    {
        return getSimulationEngine(getSimulationEngineName((EModel)diagram.getRole()));
    }

    public static String getSimulationEngineName(EModel model)
    {
        try
        {
            String emodelType = model.getType();
            List<EngineInfo> engineInfos = getSimulationEngineInfos(emodelType);
            if( engineInfos.isEmpty() )
                throw new Exception("Simulation engine compatible with model type" + model.getType() + " not found");
            return engineInfos.get(0).getName();
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "Simulation engine can not be created. " + ex);
        }
        return null;
    }

    /**
     * Comparator for engine descending sort according to thir priority to given emodel type
     * @author axec
     *
     */
    public static class EngineComparator implements Comparator<EngineInfo>
    {
        private final String modelType;
    
        public EngineComparator(String modelType)
        {
            this.modelType = modelType;
        }
    
        @Override
        public int compare(EngineInfo o1, EngineInfo o2)
        {
            double p1 = o1.getPriority(modelType);
            double p2 = o2.getPriority(modelType);
            return (int)Math.signum(p2 - p1);
        }
    }

    protected static class EngineInfo
    {
        protected HashMap<String, Double> modeltypeToPriority;
        protected Class<? extends SimulationEngine> engineClass;
        protected String name;

        public EngineInfo(String name, Class<? extends SimulationEngine> engineClass, String[] modelType, double[] priority)
        {
            this.name = name;
            this.engineClass = engineClass;
            modeltypeToPriority = new HashMap<>();
            for( int i = 0; i < modelType.length; i++ )
            {
                String type = modelType[i];
                Double prior = ( priority.length > i ) ? priority[i] : -1;
                modeltypeToPriority.put(type, prior);
            }
        }
        public Set<String> getEModelTypes()
        {
            return modeltypeToPriority.keySet();
        }

        public boolean accepts(String modelType)
        {
            return modeltypeToPriority.containsKey(modelType);
        }

        public double getPriority(String modelType)
        {
            try
            {
                return modeltypeToPriority.get(modelType);
            }
            catch( Exception ex )
            {
                return -1;
            }
        }

        public Class<? extends SimulationEngine> getEngineClass()
        {
            return engineClass;
        }

        public String getName()
        {
            return name;
        }
    }
}
