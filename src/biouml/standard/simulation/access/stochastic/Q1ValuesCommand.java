package biouml.standard.simulation.access.stochastic;

import java.util.logging.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import one.util.streamex.DoubleStreamEx;
import ru.biosoft.access.support.TagCommandSupport;
import ru.biosoft.access.support.TagEntryTransformer;
import biouml.standard.simulation.SimulationResult;
import biouml.standard.simulation.StochasticSimulationResult;
import biouml.standard.simulation.access.VariablesCommand;

public class Q1ValuesCommand extends TagCommandSupport<SimulationResult>
{
    protected static final Logger log = Logger.getLogger(VariablesCommand.class.getName());

    protected VariablesCommand varCommand;

    public Q1ValuesCommand(TagEntryTransformer<SimulationResult> transformer, VariablesCommand varCommand)
    {
        super("Q1", transformer);
        this.varCommand = varCommand;
    }

    @Override
    public String getTaggedValue()
    {
        String endl = System.getProperty("line.separator");
        StochasticSimulationResult sr = (StochasticSimulationResult)transformer.getProcessedObject();

        StringBuffer result = new StringBuffer();
        double[] times = sr.getTimes();
        double[][] values = sr.getQ1( );
        
        if( values == null )
            return "";

        int varCount = values.length > 0 ? values[0].length : 0;
        if( varCount == 0 )
            return result.toString();

        for( int timeSliceNumber = 0; timeSliceNumber < times.length; timeSliceNumber++ )
        {
            // Write time
            result.append("Q1    " + times[timeSliceNumber] + "\t");

            // Write variable values
            result.append( DoubleStreamEx.of( values[timeSliceNumber] ).joining( "\t", "", endl ) );
        }
        return result.toString();
    }
    
    @Override
    public void start( String tag )
    {
        values = new ArrayList<>();
    }

    private List<double[]> values;
    
    @Override
    public void addValue(String string)
    {
        StringTokenizer strtok = new StringTokenizer(string, "\t" );

        if( varCommand.getVarCount() != strtok.countTokens() - 1 )
        {
            log.log(Level.SEVERE, "Error: Bad variable number in line " + string + "variable count: " + varCommand.getVarCount());
            return;
        }

        if( strtok.hasMoreTokens() )
        {
            String currentToken = strtok.nextToken();
            try
            {
                double[] val = new double[varCommand.getVarCount()];
                int counter = 0;
                while( strtok.hasMoreTokens() )
                {
                    currentToken = strtok.nextToken();
                    val[counter++] = Double.parseDouble(currentToken);
                }
                values.add(val);
            }
            catch( Exception ex )
            {
                log.log(Level.SEVERE, "Could not parse value " + currentToken);
            }
        }
    }

    /** Complete reading values. */
    @Override
    public void complete(String str)
    {
        double[][] valueArray = values.toArray(new double[values.size()][]);
        StochasticSimulationResult sr = (StochasticSimulationResult)transformer.getProcessedObject();
        sr.setQ1( valueArray);
    }
}
