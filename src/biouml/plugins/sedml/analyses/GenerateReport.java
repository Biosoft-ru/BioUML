package biouml.plugins.sedml.analyses;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import one.util.streamex.StreamEx;

import biouml.plugins.sedml.MathMLUtils;
import biouml.standard.simulation.SimulationResult;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.Utils;
import ru.biosoft.util.Maps;

/**
 * @author axec
 *
 */
@ClassIcon ( "resources/generate-report.gif" )
public class GenerateReport<T extends GenerateReportParameters> extends AnalysisMethodSupport<T>
{
    protected GenerateReport(DataCollection<?> origin, String name, T parameters)
    {
        super( origin, name, parameters );
    }

    protected Map<String, double[]> evaluateExpression(String expression)
    {
        AstStart ast = Utils.parseExpression( expression );
        List<String> variables = Utils.getVariables( ast );

        final Set<String> singleEmptyPrefix = Collections.singleton( "" );
        Set<String> prefixes = singleEmptyPrefix;

        for( String var : variables )
        {
            DataElementPathSet srps = parameters.getSimulationResultPath();

            DataElementPath srp = null;
            if( srps.size() == 1 )
                srp = srps.iterator().next();
            else
            {
                srp = srps.stream().filter( p -> var.startsWith( p.getName() + "." ) ).findAny()
                        .<IllegalArgumentException> orElseThrow( () -> {
                    throw new IllegalArgumentException( "Can not resolve variable " + var );
                } );
            }

            String justVarName = var.startsWith( srp.getName() + "." ) ? var.substring( srp.getName().length() + 1 ) : var;

            Set<String> varPrefixes = StreamEx.ofKeys( srp.getDataElement( SimulationResult.class ).getVariableMap() )
                    .filter( colName -> colName.endsWith( "/" + justVarName ) )
                    .map( colName -> colName.substring( 0, colName.length() - justVarName.length() ) ).toSet();
            if( varPrefixes.isEmpty() )
                continue;
            if( prefixes != singleEmptyPrefix && !prefixes.equals( varPrefixes ) )
                throw new IllegalArgumentException( "Incompatible variables in expression " + expression );
            prefixes = varPrefixes;
        }

        return StreamEx.of( prefixes ).toSortedMap( prefix -> {
            Map<String, double[]> variableValues = getVariableValues( prefix );
            if( variableValues.isEmpty() )
                throw new IllegalArgumentException( "No variables" );

            int pointCount = variableValues.values().iterator().next().length;
            if( StreamEx.ofValues( variableValues ).anyMatch( v -> v.length != pointCount ) )
                throw new IllegalArgumentException( "Incompatible variables in expression " + expression );

            double[] values = new double[pointCount];
            for( int i = 0; i < pointCount; i++ )
            {
                Map<String, Object> scope = new HashMap<>();
                final int ii = i;
                scope.putAll( Maps.transformValues( variableValues, x -> x[ii] ) );
                AstStart st = Utils.parseExpression( expression );
                MathMLUtils.evaluateVectorFunctions( st, variableValues );
                Object evalResult = Utils.evaluateExpression( st, scope );
                if( ! ( evalResult instanceof Number ) )
                    throw new IllegalArgumentException(
                            "Expression '" + expression + "' evaluates to '" + evalResult + "' and this is not a number" );
                values[i] = ( (Number)evalResult ).doubleValue();
            }
            return values;
        } );
    }

    private Map<String, double[]> getVariableValues(String prefix)
    {
        Map<String, double[]> result = new HashMap<>();
        boolean singleSimulationResult = parameters.getSimulationResultPath().size() == 1;
        for( DataElementPath path : parameters.getSimulationResultPath() )
        {
            SimulationResult simulationResult = path.getDataElement( SimulationResult.class );
            String[] varNames = StreamEx.ofKeys( simulationResult.getVariableMap() )
                    .filter( s -> s.startsWith( prefix ) || s.equals( "time" ) ).toArray( String[]::new );
            double[][] values = simulationResult.getValues( varNames );
            for( int i = 0; i < varNames.length; i++ )
            {
                String withoutPrefix = varNames[i].equals( "time" ) ? "time" : varNames[i].substring( prefix.length() );
                result.put( path.getName() + "." + withoutPrefix, values[i] );
                if( singleSimulationResult )
                    result.put( withoutPrefix, values[i] );
            }
        }
        return result;
    }
}
