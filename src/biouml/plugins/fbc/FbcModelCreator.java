package biouml.plugins.fbc;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.plugins.fbc.SbmlModelFBCReader.FluxBounds;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.SimulationEngineRegistry;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import one.util.streamex.EntryStream;
import ru.biosoft.table.TableDataCollection;

public abstract class FbcModelCreator implements FbcConstant
{
    protected Diagram prepareDiagram(Diagram diagram)
    {
        EModel emodel = diagram.getRole( EModel.class );
        if( !requireInitialValues( diagram ) )
            return diagram;
        try
        {
            SimulationEngine engine = SimulationEngineRegistry.getSimulationEngine( diagram );
            engine.setDiagram( diagram );
            Model model = engine.createModel();
            model.init();
            double[] values = model.getCurrentValues();
            for( Map.Entry<String, Double> e : EntryStream.of( engine.getVarIndexMapping() ).mapToValue( (k, v) -> values[v] ) )
            {
                String name = e.getKey();
                Variable var = emodel.getVariable( name );
                if( var != null )
                    var.setInitialValue( e.getValue() );
            }
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
        return diagram;
    }

    protected boolean requireInitialValues(Diagram diagram)
    {
        List<Node> reactionNodes = DiagramUtility.getReactionNodes( diagram );
        EModel emodel = diagram.getRole( EModel.class );
        Set<String> equationVariables = emodel.getEquations().map( e -> e.getVariable() ).toSet();
        for( int i = 0; i < reactionNodes.size(); i++ )
        {
            Node node = reactionNodes.get( i );
            DynamicPropertySet dps = node.getAttributes();
            DynamicProperty dp = dps.getProperty( FBC_BOUNDS );
            if( dp == null )
                continue;
            FluxBounds fluxBounds = (FluxBounds)dps.getValue( FBC_BOUNDS );
            for( int j = 0; j < fluxBounds.sign.size(); j++ )
            {
                String val = fluxBounds.value.get( j );
                if( emodel.containsVariable( val ) && equationVariables.contains( val ) )
                    return true;
            }
            
            //check if change stoichiometry in initial assignments
            Reaction r = (Reaction)node.getKernel();
            for (SpecieReference sr: r)
            {
                String stoichiometry = sr.getStoichiometry();
                if( emodel.containsVariable( stoichiometry ) && equationVariables.contains( stoichiometry ) )
                    return true;
            }
        }
        return false;
    }

    public abstract FbcModel createModel(Diagram diagram, TableDataCollection fbcData, String typeObjectiveFunction);

    public abstract FbcModel createModel(Diagram diagram);

    public abstract FbcModel getUpdatedModel(Map<String, Double> values);
}
