package biouml.plugins.simulation_test.dsmts;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import biouml.plugins.simulation_test.DefaultTestLogger;
import biouml.plugins.simulation_test.SbmlCSVHandler;
import biouml.plugins.simulation_test.Status;
import biouml.standard.simulation.StochasticSimulationResult;
import one.util.streamex.StreamEx;

public class DSMTSSimulationResultLogger extends DefaultTestLogger
{
    private static final String suffixMean = "-BioUML.mean";
    private static final String suffixSD = "-BioUML.sd";
    protected static final String INITIAL_TIME = "initial_time";
    protected static final String COMPLETION_TIME = "completion_time";
    protected static final String SIMULATION_NUMBER = "simulation_number";
    private int simulationNumber = 1;
    private Map<String, Integer> variables;

    public DSMTSSimulationResultLogger(String outputPath, String name)
    {
        super(outputPath, name);
    }

    public void setSimualtionNumber(int simulationNumber)
    {
        this.simulationNumber = simulationNumber;
    }

    public void setVarNames(Map<String, Integer> variables)
    {
        this.variables = variables;
    }

    @Override
    public void testCompleted()
    {
        File outDir = new File(outputPath + "/"+name.substring(0, name.lastIndexOf("/")));
        if( !outDir.exists() )
            outDir.mkdirs();
        
        writeInfoFile(new File(outputPath+"/"+name+".info"));
        writeCSVFile( name + suffixMean + ".csv", simulationResult.getValues() );
        writeCSVFile( name + suffixSD + ".csv", ( (StochasticSimulationResult)simulationResult ).getSD() );
    }

    private List<double[]> getValues(Map<String, Integer> mapping, List<String> varNames, double[][] values)
    {
        int[] indices = StreamEx.of( varNames ).mapToInt( n -> mapping.get( n ) ).toArray();
        List<double[]> result = new ArrayList<>();
        for( int i = 0; i < values.length; i++ )
        {
            double[] filtered = new double[indices.length];
            for( int j = 0; j < indices.length; j++ )
            {
                filtered[j] = values[i][indices[j]];
            }
            result.add( filtered );
        }
        return result;
    }

    protected void writeCSVFile(String fileName, double[][] values)
    {
        if( status == Status.SUCCESSFULL )
        {
            File outDir = new File( outputPath );
            if( !outDir.exists() )
                outDir.mkdirs();

            if( statistics != null && simulationResult != null )
            {
                File outputFile = new File( outDir, fileName );
                SbmlCSVHandler handler = new SbmlCSVHandler();
                List<String> names = Arrays.asList( variables.keySet().toArray( new String[variables.size()] ) );
                handler.setVariableNames( names );
                handler.setVariableValues( getValues( variables, names, values ) );
                handler.setTimes( times );
                handler.writeCSVFile( outputFile, true );
            }
        }
    }

    protected void writeInfoFile(File infoFile)
    {
        try (PrintWriter pw = new PrintWriter( infoFile ))
        {
            if( statistics != null )
                pw.println(SIMULATOR + "=" + statistics.getSolverName());
            
            pw.println(STATUS + "=" + status);
            pw.println(SIMULATIONTIME + "=" + getSimulationTime());
                      
            if( times != null )
            {
                pw.println(INITIAL_TIME + "=" + times[0]);
                pw.println(COMPLETION_TIME + "=" + times[times.length - 1]);
                pw.println(STEP + "=" + ( times[1] - times[0] ));
            }
            
            pw.println(SIMULATION_NUMBER + "=" + simulationNumber);
            pw.println(MESSAGES + "=" + ( ( messages != null ) ? messages : "" ));
        }
        catch( Exception e )
        {
            System.out.println("*** " + e.getMessage());
        }
    }
}