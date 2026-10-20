package ru.biosoft.table.access;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.application.ApplicationUtils;

import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.PriorityTransformer;
import ru.biosoft.access.file.AbstractFileTransformer;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.TextUtil2;

/**
 * TableDataCollection to FileDataElement transformer (version 2)
 * Compatible with R simple table format.
 */
public class TableDataFileTransformer2 extends AbstractFileTransformer<StandardTableDataCollection> implements PriorityTransformer
{
    protected Logger log = Logger.getLogger(TableDataFileTransformer2.class.getName());

    public static final String ID_TAG = "ID";
    public static final String DESCRIPTION_TAG = "DESCRIPTION";
    public static final String COLUMNS_TAG = "COLUMNS";
    public static final String ID_PRESENT_PROPERTY = "isIDColumnPresent";

    @Override
    public Class<? extends StandardTableDataCollection> getOutputType()
    {
        return StandardTableDataCollection.class;
    }

    @Override
    public void save(File output, StandardTableDataCollection tdc) throws Exception
    {
        boolean isIdColumnPresent = Boolean.valueOf( tdc.getInfo().getProperty( ID_PRESENT_PROPERTY ) );
        try (Writer fw = new OutputStreamWriter( new FileOutputStream( output ), StandardCharsets.UTF_8 ))
        {
            if( isIdColumnPresent )
                fw.write( "ID\t" );
            fw.write( tdc.columns().map( TableColumn::getName ).joining( "\t" ) );
            fw.write( "\n" );
            for( RowDataElement row : tdc )
            {
                if( isIdColumnPresent )
                    fw.write( row.getName() + "\t" );
                fw.write( StreamEx.of( row.getValues() ).prepend( row.getName() ).joining( "\t" ) );
                fw.write( "\n" );
            }
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Can not transform Entry to FileDataElement", t);
        }
    }

    @Override
    public StandardTableDataCollection load(File input, String name, DataCollection<StandardTableDataCollection> origin) throws Exception
    {
        StandardTableDataCollection table = new StandardTableDataCollection( getTransformedCollection(), name );
        int idColumnIndex = -1;
        try (BufferedReader br = ApplicationUtils.utfReader( input ))
        {
            String line;
            idColumnIndex = parseInfoFile( input, table );
            boolean columnCreated = idColumnIndex > -2;
            int uniqueID = 0;
            DataType[] columnTypes = null;
            while( ( line = br.readLine() ) != null )
            {
                if( line.trim().startsWith("#") )
                {
                    //skip comments
                    continue;
                }
                if( columnCreated )
                {
                    if( columnTypes == null )
                    {
                        columnTypes = new DataType[table.getColumnModel().getColumnCount()];
                    }
                    parseValuesLine( line, table, uniqueID, idColumnIndex, columnTypes );
                    uniqueID++;
                }
                else
                {
                    idColumnIndex = parseColumns( line, table );
                    columnCreated = true;
                }
            }
            postProcess(table, columnTypes);
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Can not transform FileDataElement to TableDataCollection", t);
        }
        if( idColumnIndex > -1 )
        {
            table.getInfo().getProperties().setProperty( ID_PRESENT_PROPERTY, "true" );
        }
        return table;
    }

    protected int parseColumns(String line, TableDataCollection table) throws Exception
    {
        StringTokenizer tokenizer = new StringTokenizer(line, "\t");
        int i = 0;
        int idColumnIndex = -1;
        while( tokenizer.hasMoreTokens() )
        {
            String columnName = tokenizer.nextToken().trim();
            //Do not add first ID column as regular column of the table. It will be automatically added to table. 
            //Values parsing in parseValuesLine will take first row value as ID if number of values is less than number of columns.
            //If column with ID is not the first, rename it since ID is reserved name. 
            if( columnName.equals( "ID" ) )
            {
                idColumnIndex = i;
            }
            else
                table.getColumnModel().addColumn( columnName, String.class );
            i++;
        }
        return idColumnIndex;
    }

    protected void parseValuesLine(String line, TableDataCollection table, int uniqueIndex, int idColumnIndex, DataType[] columnTypes)
    {
        String key = null;
        StringTokenizer tokenizer = new StringTokenizer(line, "\t");
        //int tokenCount = tokenizer.countTokens();
        int delta = 0;
        if( idColumnIndex < 0 )
        {
            key = String.valueOf(uniqueIndex);
        }
        Object[] values = new Object[table.getColumnModel().getColumnCount()];
        int i = 0;
        while( tokenizer.hasMoreTokens() )
        {
            if( idColumnIndex > -1 && i == idColumnIndex )
            {
                key = tokenizer.nextToken().trim();
                delta = 1;
                i++;
                continue;
            }
            String columnStrValue = tokenizer.nextToken().trim();
            if( !columnStrValue.equals("NA") )
            {
                Object columnValue = null;
                TableColumn col = table.getColumnModel().getColumn(i - delta);
                if( col.getType() == null )
                {
                    columnValue = columnStrValue;
                    DataType possibleType = findType(columnStrValue);
                    if( columnTypes[i - delta] == null )
                    {
                        columnTypes[i - delta] = possibleType;
                    }
                    else if( columnTypes[i - delta] == DataType.Integer && possibleType != DataType.Integer )
                    {
                        columnTypes[i - delta] = possibleType;
                    }
                    else if( columnTypes[i - delta] == DataType.Float && possibleType == DataType.Text )
                    {
                        columnTypes[i - delta] = possibleType;
                    }
                }
                else if( col.getType() == DataType.Float )
                {
                    try
                    {
                        columnValue = Double.parseDouble(columnStrValue);
                    }
                    catch( NumberFormatException e )
                    {
                    }
                }
                else if( col.getType() == DataType.Integer )
                {
                    try
                    {
                        columnValue = Integer.parseInt(columnStrValue);
                    }
                    catch( NumberFormatException e )
                    {
                    }
                }
                else
                {
                    columnValue = columnStrValue;
                }
                values[i - delta] = columnValue;
                i++;
            }
        }
        TableDataCollectionUtils.addRow(table, key, values);
    }

    protected DataType findType(String strObject)
    {
        try
        {
            Integer.parseInt(strObject);
            return DataType.Integer;
        }
        catch( NumberFormatException e )
        {
        }
        try
        {
            Double.parseDouble(strObject);
            return DataType.Float;
        }
        catch( NumberFormatException e )
        {
        }
        return DataType.Text;
    }

    /**
     *  Return -2 if no file found
     *  Return -1 if file found, parsed, but ID line was not present
     *  Return 0 if ID line is present in file. Then it's considered that ID is first column. 
     */
    protected int parseInfoFile(File file, TableDataCollection table) throws Exception
    {
        boolean columnCreated = false;
        int idColIndex = -1;
        File infoFile = new File( file.getAbsolutePath() + ".info" );
        if( infoFile.exists() )
        {
            try (BufferedReader br = ApplicationUtils.utfReader( infoFile ))
            {
                br.read(new char[1]); //read first symbol in UTF-8 file
                String line = null;
                String currentTag = null;

                StringBuffer currentValue = new StringBuffer();
                while( ( line = br.readLine() ) != null )
                {
                    String possibleTag = null;
                    int tagOffset = line.indexOf('\t');
                    if( tagOffset > 0 )
                    {
                        possibleTag = line.substring(0, tagOffset);
                    }
                    else
                    {
                        possibleTag = line.trim();
                        tagOffset = line.length();
                    }

                    if( ID_TAG.equals(possibleTag) || DESCRIPTION_TAG.equals(possibleTag) || COLUMNS_TAG.equals(possibleTag) )
                    {
                        if( COLUMNS_TAG.equals(possibleTag) )
                        {
                            columnCreated = true;
                        }
                        if( ID_TAG.equals( possibleTag ) )
                        {
                            idColIndex = 0;
                        }
                        if( currentTag != null )
                        {
                            parseTagValue(currentTag, currentValue, table);
                        }
                        currentTag = possibleTag;
                        if( tagOffset >= line.length() )
                        {
                            currentValue = new StringBuffer();
                        }
                        else
                        {
                            currentValue = new StringBuffer(line.substring(tagOffset + 1));
                        }
                    }
                    else
                    {
                        currentValue.append('\n');
                        currentValue.append(line.trim());
                    }
                }
                if( currentTag != null )
                {
                    parseTagValue(currentTag, currentValue, table);
                }
            }
        }
        return columnCreated ? idColIndex : -2;
    }

    protected void parseTagValue(String tag, StringBuffer value, TableDataCollection table) throws Exception
    {
        if( DESCRIPTION_TAG.equals(tag) )
        {
            table.getInfo().setDescription(value.toString());
        }
        else if( COLUMNS_TAG.equals(tag) )
        {
            String paramsArray[] = value.toString().split("\n");
            if( paramsArray != null && paramsArray.length > 0 )
            {
                for( String param : paramsArray )
                {
                    String sample[] = TextUtil2.split( param, ';' );
                    if( sample.length > 2 )
                    {
                        String columnName = sample[0].trim();
                        DataType type = DataType.fromString(sample[1].trim());
                        String columnDisplayName = sample[2].trim();

                        table.getColumnModel().addColumn(columnName, columnDisplayName, columnDisplayName, type.getType(), "");
                    }
                }
            }
        }
    }

    protected void postProcess(TableDataCollection table, DataType[] columnTypes)
    {
        Iterator<RowDataElement> rowIterator = table.iterator();
        while( rowIterator.hasNext() )
        {
            Object[] rowValues = rowIterator.next().getValues();
            for( int i = 0; i < columnTypes.length; i++ )
            {
                if( table.getColumnModel().getColumn(i).getType() == null && columnTypes[i] != null && rowValues[i] != null )
                {
                    if( columnTypes[i] == DataType.Integer )
                    {
                        rowValues[i] = Integer.parseInt(rowValues[i].toString());
                    }
                    else if( columnTypes[i] == DataType.Float )
                    {
                        rowValues[i] = Double.parseDouble(rowValues[i].toString());
                    }
                    else if( columnTypes[i] == DataType.Text )
                    {
                        rowValues[i] = rowValues[i].toString();
                    }
                }
            }
        }
        for( int i = 0; i < columnTypes.length; i++ )
        {
            if( table.getColumnModel().getColumn(i).getType() == null && columnTypes[i] != null )
            {
                table.getColumnModel().getColumn(i).setType(columnTypes[i]);
            }
        }
    }

    @Override
    public int getInputPriority(Class<? extends DataElement> inputClass, DataElement output)
    {
        return 0;
    }

    @Override
    public int getOutputPriority(String name)
    {
        return name.endsWith( ".tsv" )?2:0;
    }
}
