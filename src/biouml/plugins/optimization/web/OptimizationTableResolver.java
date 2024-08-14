package biouml.plugins.optimization.web;

import java.util.HashMap;
import java.util.Map;

import biouml.model.Diagram;
import biouml.model.SubDiagram;
import biouml.model.web.DiagramModelTableResolver;
import biouml.plugins.optimization.Optimization;
import biouml.plugins.optimization.OptimizationConstraint;
import biouml.plugins.optimization.OptimizationExperiment;
import biouml.plugins.optimization.ParameterConnection;
import biouml.standard.diagram.Util;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.analysis.optimization.Parameter;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.table.access.TableResolver;

public class OptimizationTableResolver extends TableResolver
{
    protected String what;
    protected String expName = null;
    protected String subDiagram;
    
    public OptimizationTableResolver(BiosoftWebRequest arguments) throws WebException
    {
        this.what = arguments.getString(DiagramModelTableResolver.TABLE_TYPE_PARAMETER);
        if( what.equals("experiment") )
        {
            this.expName = arguments.getString("expname");
        }
        this.subDiagram = arguments.get( "subDiagram" );
    }

    @Override
    public DataCollection<?> getTable(DataElement de) throws Exception
    {
        Optimization optimization = de.cast( Optimization.class );
        switch(what)
        {
            case "fittingparams":
                VectorDataCollection<Parameter> fittingParams = new VectorDataCollection<>( "Parameters", Parameter.class, null );
                fittingParams.putAll(optimization.getParameters().getFittingParameters());
                return fittingParams;
            case "constraints":
                VectorDataCollection<OptimizationConstraint> constraints = new VectorDataCollection<>(
                        "Constraints", OptimizationConstraint.class, null);
                constraints.putAll(optimization.getParameters().getOptimizationConstraints());
                return constraints;
            case "experiment":
                OptimizationExperiment exp = optimization.getParameters().getOptimizationExperiment(expName);
                if( exp == null ) throw new Exception("No such experiment: "+expName);
                VectorDataCollection<ParameterConnection> parameterConnections = new VectorDataCollection<>(
                        "Connections", ParameterConnection.class, null);
                parameterConnections.putAll(exp.getParameterConnections());
                return parameterConnections;
            case "entities":
            case "variables":
            {
                Map<String, String> paramsMap = new HashMap<>();
                paramsMap.put( DiagramModelTableResolver.TABLE_TYPE_PARAMETER, what.equals( "variables" ) ? what : "variableroles" );
                if( subDiagram != null )
                {
                    paramsMap.put( "subDiagram", subDiagram );
                }

                return new DiagramModelTableResolver( new BiosoftWebRequest( paramsMap ) ).getTable( optimization.getDiagram() );
            }
            default:
                throw new WebException( "EX_QUERY_PARAM_INVALID_VALUE", "type" );
        }
    }
}