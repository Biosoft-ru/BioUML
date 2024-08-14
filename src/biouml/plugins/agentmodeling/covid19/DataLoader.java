package biouml.plugins.agentmodeling.covid19;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.model.dynamics.plot.PlotsInfo;
import biouml.model.dynamics.plot.Experiment;
import biouml.model.dynamics.plot.PlotInfo;
import biouml.standard.diagram.DiagramUtility;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.table.TableCSVImporter;
import ru.biosoft.table.TableCSVImporter.ImportProperties;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.TempFiles;

public class DataLoader
{
    public void loadOWID() throws Exception
    {
        CollectionFactory.createRepository( "../data_resources" );
        DataCollection collection = CollectionFactory.getDataCollection( "data/Collaboration/Ilya/Data/Diagrams/covid19/April 12" );
        int[] startingTime = new int[] {27, 31, 24};
        String[] countries = new String[] {"Germany", "Italy", "France"};
        loadOWID(collection, countries, Double.MAX_VALUE, new String[] {"time", "total_cases", ""});
    }

    public void loadOWID(DataCollection collection, String[] countries, double timeLimit, String[] columns) throws Exception
    {
        String urlString = "https://covid.ourworldindata.org/data/owid-covid-data.csv";
        File file = TempFiles.file( "statistics" );
        URL url = new URL( urlString );
        ReadableByteChannel readableByteChannel = Channels.newChannel( url.openStream() );
        FileOutputStream fileOutputStream = new FileOutputStream( file );
        fileOutputStream.getChannel().transferFrom( readableByteChannel, 0, Long.MAX_VALUE );
        fileOutputStream.close();

        File destination = TempFiles.dir( "OWID" );

        for( int j = 0; j < countries.length; j++ )
        {
            File result = writeCountry(file, countries[j], destination, timeLimit);
            TableCSVImporter importer = new TableCSVImporter();
            ImportProperties props = (ImportProperties)importer.getProperties( collection, result, countries[j] );
            props.setDataRow( 2 );
            props.setHeaderRow( 1 );
            props.setColumnForID( TableCSVImporter.GENERATE_UNIQUE_ID );
            importer.doImport(collection, result, result.getName(), null, null);
        }
    }

    public void removeColumn(TableDataCollection tdc, String column)
    {
        int index = tdc.getColumnModel().getColumnIndex(column);
        tdc.getColumnModel().removeColumn(index);
        CollectionFactoryUtils.save(tdc);
    }

    public void removeColumns(TableDataCollection tdc, String[] columns)
    {
        Set<String> columnSet = StreamEx.of(columns).toSet();
        List<String> toRemove = new ArrayList<>();
        int columnCount = tdc.getColumnModel().getColumnCount();
        for( int i = 0; i < columnCount; i++ )
        {
            String name = tdc.getColumnModel().getColumn(i).getName();
            if( !columnSet.contains(name) )
                toRemove.add(name);
        }

        for( String name : toRemove )
        {
            int index = tdc.getColumnModel().getColumnIndex(name);
            tdc.getColumnModel().removeColumn(index);
            CollectionFactoryUtils.save(tdc);
        }
    }

    private File writeCountry(File source, String country, File destination, double timeLimit) throws Exception
    {
        File file = new File( destination, country );
        file.createNewFile();
        boolean begin = false;
        try (BufferedReader br = ApplicationUtils.utfReader( source ); BufferedWriter bw = ApplicationUtils.utfWriter( file ))
        {
            String header = br.readLine();

            String[] headElements = header.split( "," );
            int dateIndex = 0;
            while( !headElements[dateIndex].equals( "date" ) )
                dateIndex++;

            header = "time," + header;
            bw.write( header );
            String[] titles = header.split(",");
            Object[] values = new Object[titles.length];
            for (int i=0; i<values.length; i++)
                values[i] = Double.valueOf( 0.0 );
            
//            int prevIndex = -1;
            
            int expectedIndex = 0;

            bw.newLine();
            while( true )
            {
                String line = br.readLine();
                if( line == null )
                    break;
                String[] lineArray = line.split(",", -1);
                String currentCountry = lineArray[2];
                if( !begin && currentCountry.equals( country ) )
                {
                    begin = true;
                    expectedIndex = dayIndex( lineArray[dateIndex] );
                    for( int i = 0; i < expectedIndex; i++ )
                    {
                        values[0] = String.valueOf((double)i);
                        values[dateIndex + 1] = i==0? LocalDate.ofYearDay( 2019, 365 ).toString(): LocalDate.ofYearDay( 2020, i ).toString();
                        bw.write( StreamEx.of( values ).joining( "," ) );
                        bw.newLine();
                    }
                }
                else if( begin && !currentCountry.equals( country ))
                    break;

                if( begin )
                {
                    int curIndex = getDayIndex( line, dateIndex );
                    if( curIndex > timeLimit )
                        break;
                    while (curIndex > expectedIndex) //means there are missed lines
                    {
                        insert(bw, titles.length, expectedIndex, dateIndex);
                        expectedIndex++; 
                    }
                    bw.write(String.valueOf((double) ( expectedIndex++ )));
                    bw.write( "," );
                    bw.write( line );
                    bw.write( "\n" );
                }
            }
        }
        return file;
    }  
        
    public int getDayIndex(String str, int dateIndex)
    {
        String[] lineArray = str.split( "," );
        return dayIndex( lineArray[dateIndex] );
    }
    
    public void insert(BufferedWriter bw, int length, int dayIndex, int dateIndex) throws IOException
    {
        Object[] values = new Object[length];
        for( int i = 0; i < values.length; i++ )
            values[i] = Double.valueOf( 0.0 );

        values[0] = String.valueOf( dayIndex );
        values[dateIndex + 1] = dayIndex == 0 ? LocalDate.ofYearDay( 2019, 365 ).toString() : LocalDate.ofYearDay( 2020, dayIndex ).toString();
        System.out.println( "Insert " + values[dateIndex + 1]);
        bw.write( StreamEx.of( values ).joining( "," ) );
        bw.newLine();
    }

    private static String getFirstDate(File f)
    {
        try (BufferedReader br = ApplicationUtils.utfReader( f ))
        {
            String[] header = br.readLine().split( "," );
            int i = 0;
            while( !header[i].equals( "date" ) )
                i++;

            String date = br.readLine().split( "," )[i];
            return date;

        }
        catch( Exception ex )
        {
            return null;
        }
    }

    public static int dayIndex(String dateString)
    {
        LocalDate d = LocalDate.parse( dateString );
        return d.getDayOfYear();
    }
    
    public void setExperimentTable(TableDataCollection tdc, Diagram d)
    {
        DataElementPath path = tdc.getCompletePath();
        PlotsInfo info = DiagramUtility.getPlotsInfo( d );
        for( PlotInfo plotInfo : info.getPlots() )
        {
            Experiment[] exps = plotInfo.getExperiments();
            if( exps != null )
            {
                for( Experiment exp : exps )
                {
                    exp.setPath( path );
                }
            }
        }
    }
}
