package ru.biosoft.table.access;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.Iterator;
import java.util.StringTokenizer;

import one.util.streamex.StreamEx;

import java.util.logging.Logger;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.core.AbstractTransformer;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.file.FileBasedCollection;
import ru.biosoft.access.generic.PriorityTransformer;
import ru.biosoft.access.FileDataElement;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.TextUtil;

/**
 * TableDataCollection to FileDataElement transformer (version 2)
 * Compatible with R simple table format.
 */
public class TableDataFileTransformer2 extends AbstractTransformer<FileDataElement, StandardTableDataCollection> implements PriorityTransformer
{
    protected Logger log = Logger.getLogger(TableDataFileTransformer2.class.getName());

    public static final String ID_TAG = "ID";
    public static final String DESCRIPTION_TAG = "DESCRIPTION";
    public static final String COLUMNS_TAG = "COLUMNS";

    @Override
    public Class<FileDataElement> getInputType()
    {
        return FileDataElement.class;
    }

    @Override
    public Class<? extends StandardTableDataCollection> getOutputType()
    {
        return StandardTableDataCollection.class;
    }

    @Override
    public FileDataElement transformOutput(StandardTableDataCollection tdc) throws Exception
    {
        FileDataElement fde = new FileDataElement(tdc.getName(), getPrimaryCollection().cast( FileBasedCollection.class ));
        try(Writer fw = new OutputStreamWriter(new FileOutputStream(fde.getFile()), StandardCharsets.UTF_8))
        {
            fw.write( tdc.columns().map( TableColumn::getName ).joining( "\t" ) );
            fw.write( "\n" );
            for( RowDataElement row : tdc )
            {
                fw.write( StreamEx.of( row.getValues() ).prepend( row.getName() ).joining( "\t" ) );
                fw.write( "\n" );
            }
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Can not transform Entry to FileDataElement", t);
        }

        return fde;
    }

    @Override
    public StandardTableDataCollection transformInput(FileDataElement fileElement) throws Exception
    {
        StandardTableDataCollection table = new StandardTableDataCollection(getTransformedCollection(), fileElement.getName());
        try(BufferedReader br = ApplicationUtils.utfReader( fileElement.getFile() ))
        {
            String line;
            boolean columnCreated = parseInfoFile(fileElement, table);
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
                    parseValuesLine(line, table, uniqueID, columnTypes);
                    uniqueID++;
                }
                else
                {
                    parseColumns(line, table);
                    columnCreated = true;
                }
            }
            postProcess(table, columnTypes);
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Can not transform FileDataElement to TableDataCollection", t);
        }
        return table;
    }

    protected void parseColumns(String line, TableDataCollection table) throws Exception
    {
        StringTokenizer tokenizer = new StringTokenizer(line, "\t");
        while( tokenizer.hasMoreTokens() )
        {
            String columnName = tokenizer.nextToken().trim();
            table.getColumnModel().addColumn(columnName, String.class);
        }
    }

    protected void parseValuesLine(String line, TableDataCollection table, int uniqueIndex, DataType[] columnTypes)
    {
        String key = null;
        String[] tokens = line.split("\\t");
        int delta = 0;
        if( tokens.length == table.getColumnModel().getColumnCount() )
        {
            key = String.valueOf(uniqueIndex);
        }
        else
        {
            key = tokens[0];
            delta = 1;
        }
        Object[] values = new Object[table.getColumnModel().getColumnCount()];
        for( int i = delta; i < tokens.length; i++ )
        {
            String columnStrValue = tokens[i].trim();
            if( !columnStrValue.trim().equals("NA") )
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

    protected boolean parseInfoFile(FileDataElement fde, TableDataCollection table) throws Exception
    {
        boolean columnCreated = false;
        File infoFile = new File(fde.getFile().getAbsolutePath() + ".info");
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
        return columnCreated;
    }

    protected void parseTagValue(String tag, StringBuffer value, TableDataCollection table) throws Exception
    {
        if( DESCRIPTION_TAG.equals(tag) )
        {
            table.getInfo().setDescription(value.toString());
        }
        else if( COLUMNS_TAG.equals(tag) )
        {
            String paramsArray[] = value.toString().split("\\n");
            if( paramsArray != null && paramsArray.length > 0 )
            {
                for( String param : paramsArray )
                {
                    String sample[] = TextUtil.split( param, ';' );
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
