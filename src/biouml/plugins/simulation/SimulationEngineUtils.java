package biouml.plugins.simulation;

import java.io.File;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;

import ru.biosoft.util.ApplicationUtils;

import biouml.model.Module;
import biouml.model.dynamics.EModel;
import biouml.standard.simulation.SimulationResult;

public class SimulationEngineUtils
{
    protected static final Logger log = Logger.getLogger(SimulationEngineUtils.class.getName());

    /** TODO - generate log message about code generation. */
    public static File[] generateCode(EModel executableModel, SimulationEngine simulationEngine)
    {
        try
        {
            return simulationEngine.generateModel(true);
        }
        catch( Throwable t )
        {
            String st = ApplicationUtils.trimStackAsString( t, 10 );
            simulationEngine.getLogger().error("ERROR_CODE_GENERATION", new String[] {simulationEngine.getDiagram().getName(), st/*t.toString()*/});
            log.log( Level.SEVERE, "ERROR_CODE_GENERATION", t );
            t.printStackTrace();
        }

        return null;
    }

    public static DataCollection getSimulationResultDC(Module module, SimulationEngine simulationEngine)
    {
        DataCollection resultDC = null;

        // try to get data collection to save results by two different ways:
        try
        {
            if( module == null )
            {
                simulationEngine.getLogger().error("ERROR_GET_DATABASE", new String[] {simulationEngine.getDiagram().getCompletePath().toString()});
                return null;
            }

            // try to get data collection to save results by two different ways:
            if( module.getType().isCategorySupported() )
            {
                try
                {
                    resultDC = module.getCategory(SimulationResult.class);
                }
                catch( Throwable t )
                {
                }
            }

            if( resultDC == null )
            {
                DataCollection simulationDC = (DataCollection)module.get(Module.SIMULATION);
                if( simulationDC != null )
                    resultDC = (DataCollection)simulationDC.get("result");
            }
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "Can not find simulation result data collection: ", ex);
        }

        return resultDC;
    }

}
