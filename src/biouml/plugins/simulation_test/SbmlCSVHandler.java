package biouml.plugins.simulation_test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import one.util.streamex.DoubleStreamEx;
import one.util.streamex.StreamEx;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.plugins.sbml.SbmlModelReader;

public class SbmlCSVHandler
{
    private final String[] timeVariables = new String[] {"time", "Time"};

    private boolean timeCourse = true;
    
    public boolean isTimeCourse()
    {
        return timeCourse;
    }
    
    List<String> varNames = new ArrayList<>();
    public List<String> getVariableNames()
    {
        return varNames;
    }
    public void setVariableNames(List<String> varNames)
    {
        this.varNames = varNames;
    }

    List<double[]> varValues = new ArrayList<>();
    public List<double[]> getVariableValues()
    {
        return varValues;
    }
    public void setVariableValues(List<double[]> varValues)
    {
        this.varValues = varValues;
    }

    double[] times = null;

    public double[] getTimes()
    {
        return times;
    }

    public void setTimes(double[] times)
    {
        this.times = times;
    }

    public SbmlCSVHandler()
    {

    }

    public SbmlCSVHandler(File csvFile, boolean timeCourse) throws Exception
    {
        this.timeCourse = timeCourse;
        try(BufferedReader br = ApplicationUtils.utfReader( csvFile ))
        {
            String line = br.readLine();
            if( line == null ) throw new IllegalArgumentException("CSV file '" + csvFile.getName() + "' is empty");
            StringTokenizer firstLine = new StringTokenizer(line, ",");
            while( firstLine.hasMoreTokens() )
            {
                String varName = firstLine.nextToken().trim();
                if( varName.contains("`") )
                {
                    varName = varName.substring(varName.indexOf("`") + 1);
                }
                varNames.add(varName);
            }
            int varCount = varNames.size();
            
            // read data
            String dataLine;
            while( ( dataLine = br.readLine() ) != null )
            {
                StringTokenizer strtok = new StringTokenizer(dataLine, ",");
                if( strtok.countTokens() < varCount )
                {
                    //logger.error(Status.CSV_ERROR, "Bad line: " + dataLine);
                    continue;
                }
                
                double[] data = new double[varCount];
                for( int i = 0; i < varCount; i++ )
                {
                    data[i] = SbmlModelReader.parseSBMLDoubleValue(strtok.nextToken().trim());
                }
                varValues.add(data);
            }
        }

        if( this.timeCourse )
        {
            int timeIndex = StreamEx.of(this.timeVariables).mapToInt( varNames::indexOf ).without( -1 ).findFirst()
                    .orElseThrow( () -> new Exception("Can not find time variable") );
            times = StreamEx.of(varValues).mapToDouble(varValue -> varValue[timeIndex]).toArray();
        }
    }

    public SbmlCSVHandler(File csvFile) throws Exception
    {
        this(csvFile, true);
    }

    public void writeCSVFile(File csvFile)
    {
        writeCSVFile( csvFile, false );
    }

    public void writeCSVFile(File csvFile, boolean addTime)
    {
        try(PrintWriter pw = new PrintWriter(csvFile, "UTF-8"))
        {
            if( addTime )
                pw.println( StreamEx.of( varNames ).prepend( "time" ).joining( "," ) );
            else
                pw.println( StreamEx.of( varNames ).joining( "," ) );
            for( int i = 0; i < varValues.size(); i++ )
            {
                double[] values = varValues.get( i );
                if( addTime )
                    pw.println( DoubleStreamEx.of( values ).prepend( times[i] ).joining( "," ) );
                else
                    pw.println( StreamEx.of( values ).joining( "," ) );
            }
        }
        catch( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    /**
     * Find index corresponding to given timePoint (with given accuracy)
     * @param time - given time point
     * @param accuracy
     * @return -1 if time time not found
     */
    public int findIndexByTime(double time, double accuracy)
    {
        for( int point = 0; point < times.length; point++ )
        {
            if( Math.abs(time - times[point]) <= accuracy * ( Math.abs(times[point]) + Math.abs(time) ) )
                return point;
        }
        return -1;
    }

    public double[] getValues(double time, double accuracy)
    {
        int index = findIndexByTime(time, accuracy);
        if( index == -1 )
            return null;
        return varValues.get(index);
    }
}
