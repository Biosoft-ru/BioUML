package biouml.plugins.physicell;

import biouml.model.Diagram;
import biouml.plugins.simulation.SimulationEngine;
import biouml.standard.diagram.DiagramUtility;

public class SimulationEngineHelper
{
    public String generateReport(Diagram d)
    {
        try
        {
            SimulationEngine engine = DiagramUtility.getPreferredEngine( d );
            if( ! ( engine instanceof PhysicellSimulationEngine ) )
            {
                engine = new PhysicellSimulationEngine();
            }
            engine.setDiagram( d );
            PhysicellModel model = ( (PhysicellSimulationEngine)engine ).createModel();

            return model.display();
        }
        catch( Exception e )
        {
            return "Could not generate Summary: " + e.getMessage();
        }
    }
}
