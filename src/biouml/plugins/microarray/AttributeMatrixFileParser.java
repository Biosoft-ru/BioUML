package biouml.plugins.microarray;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import one.util.streamex.StreamEx;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.TextUtil;

public class AttributeMatrixFileParser
{
    private BufferedReader br;

    private DataType types[];

    private String headerName = null;

    private String[] dels = new String[] {"\\s"};

    public String getHeaderName()
    {
        return headerName;
    }

    private static final String ATT_MAT_EXTENSIONS[] = TextUtil.split( MessageBundle.getMessage("ATT_MAT_FILE_EXT"), ' ' );

    public AttributeMatrixFileParser()
    {
    }

    public void parseFiles(File directory) throws Exception
    {
        File files[] = directory.listFiles(f -> {
            if( f.isDirectory() )
            {
                return true;
            }
            else if( f.isFile() )
            {
                return StreamEx.of( ATT_MAT_EXTENSIONS ).anyMatch( f.getName().toLowerCase()::endsWith );
            }
            return false;
        });
        if(files == null)
            throw new IOException( "Unable to read directory "+directory );

        for( File file : files )
        {
            try
            {
                parseFile(file);
            }
            catch( Exception e )
            {
                e.printStackTrace();
            }
        }
    }

    public HashMap<String, List<Object[]>> parseFile(File file) throws IOException
    {
        HashMap<String, List<Object[]>> attributes = new HashMap<>();
        br = ApplicationUtils.utfReader( file );
        String values[] = null;
        DataType types0[];
        while( null == values )
            values = resolveLine();
        types = tryToResolveTypes(values);

        values = resolveLine();
        while( null == values )
            values = resolveLine();
        types0 = tryToResolveTypes(values);
        boolean header = !Arrays.equals(types, types0);
        types = types0;
        br.close();
        br = ApplicationUtils.utfReader( file );
        ArrayList<Object[]> ttypes = new ArrayList<>( 1 );
        ttypes.add(types);
        attributes.put("types", ttypes);
        if( header )
        {
            values = resolveLine();
            while( null == values )
                values = resolveLine();
            headerName = values[0];
            ArrayList<Object[]> vvalues = new ArrayList<>( 1 );
            vvalues.add(values);
            attributes.put(values[0], vvalues);
        }
        Object typedValues[];
        while( true )
        {
            try
            {
                values = resolveLine();
                while( null == values )
                    values = resolveLine();
            }
            catch( EOFException e )
            {
                break;
            }
            catch( IOException e )
            {
                throw e;
            }
            typedValues = typecastValues(values);
            List<Object[]> vvalues = attributes.get(values[0]);
            if( null == vvalues )
            {
                vvalues = new ArrayList<>();
                attributes.put(values[0], vvalues);
            }
            vvalues.add(typedValues);
        }
        br.close();
        return attributes;
    }

    public TableDataCollection parseFileToMicroarray(File file, DataCollection<?> parent, String[] dels) throws IOException
    {
        this.dels = dels;
        Map<String, List<Object[]>> result = parseFile(file);

        TableDataCollection tableDataCollection = new StandardTableDataCollection(parent, file.getName().substring(0, file.getName().indexOf('.')));
        
        Object types[] = result.get("types").get(0);
        Object columnNames[] = null;
        if( null != headerName )
            columnNames = result.get(headerName).get(0);
        else
        {
            columnNames = new Object[types.length - 1];
            Arrays.fill( columnNames, "" );
        }

        try
        {
            String columnName;
            for( int i = 0; i < columnNames.length; i++ )
            {
                columnName = columnNames[i].toString().trim();
                if( "".equals(columnName) )
                    columnName = "Column" + i;
                tableDataCollection.getColumnModel().addColumn(columnName, (DataType)types[i + 1]);
            }
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }

        for(Entry<String, List<Object[]>> entry : result.entrySet())
        {
            String key = entry.getKey();
            Object objects[] = entry.getValue().get( 0 );
            Object newObjects[] = new Object[objects.length - 1];
            System.arraycopy(objects, 1, newObjects, 0, newObjects.length);
            if( !key.equals("types") && !key.equals(headerName) )
            {
                TableDataCollectionUtils.addRow(tableDataCollection, key, newObjects);
            }
        }

        return tableDataCollection;
    }

    private Object[] typecastValues(String[] values)
    {
        Object ret[] = new Object[values.length];
        for( int i = 0; i < values.length; i++ )
        {
            try
            {
                if( types[i].equals(DataType.Integer) )
                    ret[i] = Integer.parseInt(values[i]);
                else if( types[i].equals(DataType.Float) )
                    ret[i] = Double.parseDouble(values[i]);
                // else if( types[i].equals(Long.class) )
                // ret[i] = Long.parseLong(values[i]);
                else
                    ret[i] = values[i];
            }
            catch( NumberFormatException e )
            {
                ret[i] = 0;
            }
        }
        return ret;
    }

    private String[] resolveLine() throws IOException
    {
        String line = null;
        String values[];
        line = br.readLine();
        if( null == line )
            throw new EOFException();
        ArrayList<String> list = new ArrayList<>();
        list.add(line);
        for( String delimiter : dels )
        {
            if( "".equals(delimiter) )
                continue;
            ArrayList<String> tmpList = new ArrayList<>();
            for( String string : list )
            {
                String tmp[] = string.split(delimiter);
                for( String str : tmp )
                {
                    tmpList.add(str);
                }
            }
            list = tmpList;
        }
        values = list.toArray(new String[list.size()]);
        if( 0 == values.length )
            return null;
        return values;
    }

    private DataType[] tryToResolveTypes(String values[])
    {
        return StreamEx.of(values).map( this::tryToResolveType ).toArray( DataType[]::new );
    }

    private DataType tryToResolveType(String string)
    {
        if( TextUtil.isIntegerNumber(string) )
        {
            return DataType.Integer;
        }
        else if( TextUtil.isFloatingPointNumber(string) )
        {
            return DataType.Float;
        }

        return DataType.Text;
    }
}