package ru.biosoft.table;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;

public class TableImporter implements DataElementImporter
{
    private static final String FORMAT = "Routput";
    private static final Pattern PATTERN = Pattern.compile( ".+\\." + FORMAT, Pattern.CASE_INSENSITIVE );

    @Override
    public int accept(DataCollection parent, File file)
    {
        if( parent == null || !parent.isMutable() || !DataCollectionUtils.isAcceptable( parent, getResultType() ) )
            return ACCEPT_UNSUPPORTED;
        return ( file == null || PATTERN.matcher( file.getName() ).matches() )
                ? ACCEPT_HIGH_PRIORITY : ACCEPT_UNSUPPORTED;
    }

    @Override
    public DataElement doImport(@Nonnull DataCollection parent, @Nonnull File file, String elementName, FunctionJobControl jobControl,
            Logger log) throws Exception
    {
        if( jobControl != null )
        {
            jobControl.functionStarted();
        }

        String baseName = elementName == null || elementName.equals( "" ) ? file.getName().replaceFirst( "\\.[^\\.]+$", "" ) : elementName;
        String name = baseName;
        while( parent.get( name ) != null )
        {
            log.warning( "Duplicate item " + name + ": old one removed" );
            parent.remove( name );
        }
        int j = 0;
        int i;
        int colCount;
        try (FileInputStream is = new FileInputStream( file );
                BufferedReader input = new BufferedReader( new InputStreamReader( is, StandardCharsets.UTF_8 ) ))
        {
            FileChannel ch = is.getChannel();
            TableDataCollection table = TableDataCollectionUtils.createTableDataCollection( parent, name );
            String line = input.readLine();
            if( line == null )
                throw new Exception( "Header line expected" );
            String[] colnames = line.split( "\t" );
            if( colnames.length <= 1 )
                throw new Exception( "Header line invalid" );
            for( i = 1; i < colnames.length; i++ )
            {
                String colname = colnames[i].replaceFirst( "^.+[\\\\\\/]([^\\\\\\/]+)$", "$1" );
                table.getColumnModel().addColumn( colname, Double.class );
            }
            colCount = table.getColumnModel().getColumnCount();
            while( ( line = input.readLine() ) != null )
            {
                j++;
                String[] fields = line.split( "\t" );
                Float[] values = new Float[colCount];
                for( i = 0; i < colCount; i++ )
                    values[i] = 0f;
                for( i = 0; i < Math.min( fields.length - 1, colCount ); i++ )
                {
                    try
                    {
                        values[i] = Float.parseFloat( fields[i + 1] );
                    }
                    catch( NumberFormatException e )
                    {
                    }
                }
                TableDataCollectionUtils.addRow( table, fields[0], values, true );
                if( jobControl != null && j % 100 == 0 )
                {
                    jobControl.setPreparedness( (int) ( 100 * ch.position() / ch.size() ) );
                    if( jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST )
                    {
                        input.close();
                        parent.remove( name );
                        return null;
                    }
                }
            }
            table.finalizeAddition();
            parent.put( table );
        }
        catch( Exception e )
        {
            parent.remove( name );
            if( jobControl != null )
            {
                jobControl.functionTerminatedByError( e );
                return null;
            }
            throw e;
        }
        if( jobControl != null && jobControl.getStatus() != JobControl.TERMINATED_BY_REQUEST
                && jobControl.getStatus() != JobControl.TERMINATED_BY_ERROR )
        {
            jobControl.setPreparedness( 100 );
            jobControl.functionFinished();
        }
        return parent.get( name );
    }

    @Override
    public boolean init(Properties properties)
    {
        return FORMAT.equalsIgnoreCase( properties.getProperty( SUFFIX ) );
    }

    @Override
    public Object getProperties(DataCollection<?> parent, File file, String elementName)
    {
        return null;
    }

    @Override
    public Class<? extends DataElement> getResultType()
    {
        return TableDataCollection.class;
    }
}
