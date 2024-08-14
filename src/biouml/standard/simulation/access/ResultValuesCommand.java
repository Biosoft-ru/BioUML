package biouml.standard.simulation.access;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.logging.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import one.util.streamex.DoubleStreamEx;

import java.util.logging.Logger;

import ru.biosoft.access.support.TagCommandSupport;
import ru.biosoft.access.support.TagEntryTransformer;
import biouml.standard.simulation.SimulationResult;

public class ResultValuesCommand extends TagCommandSupport<SimulationResult>
{
    protected static final Logger log = Logger.getLogger(VariablesCommand.class.getName());

    protected VariablesCommand varCommand;

    public ResultValuesCommand(TagEntryTransformer<SimulationResult> transformer, VariablesCommand varCommand)
    {
        super("VL", transformer);
        this.varCommand = varCommand;
    }

    @Override
    public String getTaggedValue()
    {
        String endl = System.getProperty("line.separator");
        SimulationResult sr = transformer.getProcessedObject();

        StringBuffer result = new StringBuffer();
        double[] times = sr.getTimes();
        double[][] values = sr.getValues();

        if( values == null )
            return "";

        int varCount = values.length > 0 ? values[0].length : 0;
        if( varCount == 0 )
            return result.toString();

        for( int timeSliceNumber = 0; timeSliceNumber < times.length; timeSliceNumber++ )
        {
            // Write time
            result.append("VL    " + times[timeSliceNumber] + "\t");

            // Write variable values
            result.append( DoubleStreamEx.of( values[timeSliceNumber] ).joining( "\t", "", endl ) );
        }
        return result.toString();
    }
    
    @Override
    public void start( String tag )
    {
        times = new TDoubleArrayList();
        values = new ArrayList<>();
    }

    private TDoubleList times;
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
                times.add(Double.parseDouble(currentToken));
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
        double[] timeArray = times.toArray();
        double[][] valueArray = values.toArray(new double[values.size()][]);
        SimulationResult sr = transformer.getProcessedObject();
        sr.setTimes(timeArray);
        sr.setValues(valueArray);
    }
}
