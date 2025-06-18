package ru.biosoft.table.csv;

import java.io.BufferedReader;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.TextUtil2;

/**
 * Utility class for reading TableDataCollection from CSV file
 */
public class TableCSVReader
{
    protected static final Logger log = Logger.getLogger(TableCSVReader.class.getName());

    /**
     * Read table from CSV file
     */
    public TableDataCollection readTable(DataCollection parent, String name, File file)
    {
        TableDataCollection tableDataCollection = new StandardTableDataCollection(parent, name);

        // TODO: support other encodings if necessary
        try(BufferedReader reader = ApplicationUtils.utfReader( file ))
        {
            int columnCount = readHead(tableDataCollection, reader);
            readValues(tableDataCollection, reader, columnCount);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can nor read TableDataCollection from CSV file \"" + file.getAbsolutePath() + "\"", e);
        }

        return tableDataCollection;
    }

    /**
     * Read first line of CSV file and create columns
     * @return column count
     */
    protected int readHead(TableDataCollection tdc, BufferedReader reader) throws Exception
    {
        String line = reader.readLine();
        int columnCount = 0;
        if( line == null || line.length() == 0 )
        {
            log.log(Level.SEVERE, "No data in the file");
        }
        else
        {
            String[] columnNames = TextUtil2.split( line, ' ' );
            for( String columnName : columnNames )
            {
                tdc.getColumnModel().addColumn(columnName, String.class);
                columnCount++;
            }
        }
        return columnCount;
    }

    /**
     * Read CSV file to the end and add new rows to table
     */
    protected void readValues(TableDataCollection tdc, BufferedReader reader, int columnCount) throws Exception
    {
        String line;
        while( ( line = reader.readLine() ) != null )
        {
            String[] values = TextUtil2.split( line, ' ' );
            if( values.length != columnCount )
            {
                log.log(Level.SEVERE, "Incollect line in CSV file, parsing stopped");
                break;
            }
            else
            {
                TableDataCollectionUtils.addRow(tdc, values[0], values);
            }
        }
    }
}
