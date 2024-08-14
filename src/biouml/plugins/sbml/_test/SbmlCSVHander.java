package biouml.plugins.sbml._test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import one.util.streamex.DoubleCollector;
import one.util.streamex.DoubleStreamEx;

import com.developmentontheedge.application.ApplicationUtils;

public class SbmlCSVHander
{
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

    public SbmlCSVHander()
    {

    }

    public SbmlCSVHander(File csvFile, TestLogger logger)
    {
        try(BufferedReader br = ApplicationUtils.utfReader( csvFile ))
        {
            

            StringTokenizer firstLine = new StringTokenizer(br.readLine(), ",");
            while( firstLine.hasMoreTokens() )
            {
                varNames.add(firstLine.nextToken().trim());
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
                    data[i] = Double.parseDouble(strtok.nextToken().trim());
                }
                varValues.add(data);
            }
        }
        catch( Exception e )
        {
            //logger.error(Status.CSV_ERROR, "CSV file processing error:" + e);
        }
    }

    public void writeCSVFile(File csvFile)
    {
        try(PrintWriter pw = new PrintWriter(csvFile, "UTF-8"))
        {
            pw.println(String.join( ",", varNames ));

            for( double[] values : varValues )
            {
                pw.println( DoubleStreamEx.of( values ).collect( DoubleCollector.joining( "," ) ) );
            }
        }
        catch(IOException e)
        {
            throw new UncheckedIOException( e );
        }
    }
}
