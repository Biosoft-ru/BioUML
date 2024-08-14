package biouml.plugins.simulation_test;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import biouml.standard.simulation.SimulationResult;

public class DefaultTestLogger implements TestLogger
{
    protected String outputPath;
    protected String name;
    protected TestDescription statistics;
    protected SimulationResult simulationResult;
    protected double times[];
    protected String scriptName;
    protected int status;
    protected String messages;

    private static final String fileSuffix = ".BioUML";

    public DefaultTestLogger(String outputPath, String name)
    {
        this.outputPath = outputPath;
        this.name = name;
    }

    @Override
    public String getOutputPath()
    {
        return outputPath;
    }

    @Override
    public void testStarted(String testName)
    {
        status = Status.SUCCESSFULL;
        messages = "";
    }
    @Override
    public void testCompleted()
    {
        writeInfoFile();
        writeCSVFile( name + fileSuffix + ".csv", simulationResult );
    }

    @Override
    public int getStatus()
    {
        return status;
    }

    @Override
    public void warn(String message)
    {
        this.messages += "; " + message;
    }
    @Override
    public void error(int status, String message)
    {
        this.status = status;
        this.messages += message + "; ";
    }

    @Override
    public void simulationStarted()
    {
        simStarted = System.currentTimeMillis();
    }

    @Override
    public void simulationCompleted()
    {
        simCompleted = System.currentTimeMillis();
    }

    @Override
    public void complete()
    {

    }

    protected void writeCSVFile(String fileName, SimulationResult simulationResult)
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
                List<String> varList = Arrays
                        .asList( simulationResult.getVariableMap().keySet().stream().sorted().toArray( String[]::new ) );
                handler.setVariableNames( varList );
                List<double[]> values = new ArrayList<>();
                try
                {
                    List<double[]> tmpValues = statistics.getInterpolatedValues( simulationResult, times, varList );
                    int ind = 0;
                    for( double[] line : tmpValues )
                    {
                        double[] newLine = new double[line.length + 1];
                        newLine[0] = times[ind];
                        System.arraycopy( line, 0, newLine, 1, line.length );
                        values.add( newLine );
                        ind++;
                    }
                }
                catch( Exception e )
                {

                }
                handler.setVariableValues( values );
                handler.writeCSVFile( outputFile );
            }
        }
    }

    public static final String STATUS = "status";
    public static final String SIMULATIONTIME = "time";
    public static final String MESSAGES = "messages";
    public static final String SIMULATOR = "simulator";
    public static final String ZERO = "zero";
    public static final String ATOL = "atol";
    public static final String RTOL = "rtol";
    public static final String STEP = "step";
    public static final String SBML_LEVEL = "sbmllevel";
    public static final String SCRIPT_NAME = "scriptname";

    public static final String TIME_COURSE_TEST = "timecourse";

    protected void writeInfoFile()
    {
        File outDir = new File( outputPath );
        if( !outDir.exists() )
            outDir.mkdirs();
        name = name.replaceAll( "\\\\", "/" );
        new File( outDir, name ).getParentFile().mkdirs();

        try (PrintWriter pw = new PrintWriter( new File( outDir, name + ".info" ), "UTF-8" ))
        {
            pw.println( STATUS + "=" + status );
            pw.println( SIMULATIONTIME + "=" + this.getSimulationTime() );
            if( statistics != null )
            {
                pw.println( SIMULATOR + "=" + statistics.getSolverName() );
                pw.println( SBML_LEVEL + "=" + statistics.getSbmlLevel() );
                pw.println( ZERO + "=" + statistics.getZero() );
                pw.println( ATOL + "=" + statistics.getAtol() );
                pw.println( RTOL + "=" + statistics.getRtol() );
                pw.println( STEP + "=" + statistics.getStep() );
                pw.println( MESSAGES + "=" + ( ( messages != null ) ? messages : "" ) );
                pw.println( SCRIPT_NAME + "=" + scriptName );
                pw.println( TIME_COURSE_TEST + "= true" );
            }
        }
        catch( Exception e )
        {
            System.out.println( "*** " + e.getMessage() );
        }
    }

    protected long simCompleted;
    protected long simStarted;
    public long getSimulationTime()
    {
        if( simCompleted > simStarted )
            return simCompleted - simStarted;

        return 0;
    }

    @Override
    public void setStatistics(TestDescription statistics)
    {
        this.statistics = statistics;
    }

    @Override
    public void setSimulationResult(SimulationResult simulationResult)
    {
        this.simulationResult = simulationResult;
    }

    @Override
    public void setTimes(double times[])
    {
        this.times = times;
    }

    @Override
    public void setScriptName(String scriptName)
    {
        this.scriptName = scriptName;
    }
}
