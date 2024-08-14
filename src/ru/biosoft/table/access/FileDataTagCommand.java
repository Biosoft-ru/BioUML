package ru.biosoft.table.access;

import java.util.StringTokenizer;

import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.FileTableDataCollection;
import ru.biosoft.table.RowDataElement;

/**
 * Optimized read data tag command for {@link FileTableDataCollection}
 */
public class FileDataTagCommand extends TableDataTagCommand
{
    protected FileTableDataCollection table;
    protected ColumnModel columnModel;

    public FileDataTagCommand(String tag, TableDataEntryTransformer transformer, FileTableDataCollection table)
    {
        super(tag, transformer);
        this.table = table;
        this.columnModel = table.getColumnModel();
    }

    @Override
    public void addValue(String value)
    {
        if( DATA_TAG.equals(tag) )
        {
            parseValuesLine(value);
        }
        else
        {
            super.addValue(value);
        }
    }

    protected void parseValuesLine(String line)
    {
        RowDataElement rde = parseLine(table, columnModel, line);
        if( rde != null )
        {
            table.addDataDirectly(rde);
        }
    }
    /**
     * Parse table values from line
     */
    public static RowDataElement parseLine(FileTableDataCollection table, ColumnModel columnModel, String line)
    {
        int size = columnModel.getColumnCount();
        Object[] values = new Object[size];
        String delimiters = table.getInfo().getProperty(FileTableDataCollection.VALUE_DELIMITERS_PROPERTY);
        if( delimiters == null )
        {
            delimiters = "\t";
            table.getInfo().getProperties().put(FileTableDataCollection.VALUE_DELIMITERS_PROPERTY, delimiters);
        }
        StringTokenizer st = new StringTokenizer( line, delimiters, true );
        int i = 0;
        if( st.hasMoreTokens() )
        {
            String key = st.nextToken();
            boolean prevDelimiter = false;

            while( ( st.hasMoreTokens() ) && ( i < size ) )
            {
                String token = st.nextToken();

                boolean isDelimiter = delimiters.contains( token );
                if( !isDelimiter || prevDelimiter )
                {
                    String curValue = isDelimiter ? "" : token;
                    try
                    {
                        values[i] = columnModel.getColumn( i ).getType().convertValue( curValue );
                    }
                    catch( Exception e )
                    {
                        values[i] = curValue; // just store it to give user possibility to fix it later
                    }
                    i++;
                }
                prevDelimiter = isDelimiter;
            }
            RowDataElement result = new RowDataElement(key, table);
            result.setValues(values);
            return result;
        }
        return null;
    }
}
