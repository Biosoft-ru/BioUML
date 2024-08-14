package ru.biosoft.table;

import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.sql.SqlConnectionHolder;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.util.ListUtil;

/**
 * TableDataCollection as the result of SQL query
 * @author lan
 */
public class SqlQueryTableDataCollection extends TableDataCollection implements SqlConnectionHolder
{
    /**
     * MySQL group_concat_max_len value which will be used in queries
     */
    private static final int GROUP_CONCAT_MAX_LEN = 32768;
    private static final Logger log = Logger.getLogger(SqlQueryTableDataCollection.class.getName());
    /**
     * Required property: query to fill the table
     */
    public static final String QUERY_PROPERTY = "query";
    /**
     * Optional property: query to get names. Must return the same number of rows in the same order as QUERY_PROPERTY.
     * If omitted, row names will be auto-generated numbers staring from 1.
     * If it equals to $1, $2 or ... then the corresponding column from query "query" will be taken as namelist (try to avoid this for huge queries)
     */
    public static final String NAMELIST_QUERY_PROPERTY = "nameListQuery";
    /**
     * Optional property: query to get table size. If NAMELIST_QUERY_PROPERTY specified, then it's ignored
     * If absent, it will be generated based on QUERY_PROPERTY value
     */
    public static final String COUNT_QUERY_PROPERTY = "countQuery";
    /**
     * SqlConnectionHolder object (or path to it in repository) to get the SqlConnection from.
     * If absent, then parent will be considered as SqlConnectionHolder.
     * If parent is not SqlConnectionHolder then exception will occur
     */
    public static final String SQL_HOLDER_PROPERTY = "sqlHolder";

    private SqlConnectionHolder connectionHolder;
    private String query;
    private int size;
    private int chunkSize;
    private boolean valid = true;
    private String fatalError;
    private Map<Integer, WeakReference<RowDataElement[]>> chunks = new HashMap<>();
    private RowDataElement[] lastChunk;
    private Map<String, Integer> nameMap;
    private List<String> nameList;

    public SqlQueryTableDataCollection(String name, SqlConnectionHolder holder, String query)
    {
        this(name, holder, query, null);
    }

    public SqlQueryTableDataCollection(String name, SqlConnectionHolder holder, String query, String nameListQuery)
    {
        this(null, makeProperties(name, holder, query, nameListQuery));
    }

    private static Properties makeProperties(String name, SqlConnectionHolder holder, String query, String nameListQuery)
    {
        Properties properties = new Properties();
        properties.put(DataCollectionConfigConstants.NAME_PROPERTY, name);
        properties.put(QUERY_PROPERTY, query);
        properties.put(SQL_HOLDER_PROPERTY, holder);
        if( nameListQuery != null )
            properties.put(NAMELIST_QUERY_PROPERTY, nameListQuery);
        return properties;
    }

    /**
     * @param parent
     * @param properties
     */
    public SqlQueryTableDataCollection(DataCollection<?> parent, Properties properties)
    {
        super(parent, properties);
        Object holder = properties.get(SQL_HOLDER_PROPERTY);
        if( holder instanceof SqlConnectionHolder )
        {
            connectionHolder = (SqlConnectionHolder)holder;
        }
        else if( holder != null )
        {
            try
            {
                connectionHolder = (SqlConnectionHolder)CollectionFactory.getDataElement(holder.toString());
            }
            catch( Exception e )
            {
            }
            if( connectionHolder == null )
            {
                fatal("Cannot fetch SqlConnectionHolder from properties: " + holder);
                return;
            }
        }
        else if( parent instanceof SqlConnectionHolder )
        {
            connectionHolder = (SqlConnectionHolder)parent;
        }
        else
        {
            fatal("Cannot fetch SqlConnectionHolder from parent: " + ( parent == null ? null : parent.getCompletePath() ));
            return;
        }
        query = properties.getProperty(QUERY_PROPERTY);
        if( query == null )
        {
            fatal("Cannot fetch query from properties");
            return;
        }
        String nameListQuery = properties.getProperty(NAMELIST_QUERY_PROPERTY);
        int nameColumn = -1;
        if( nameListQuery != null )
        {
            if(nameListQuery.startsWith("$"))
            {
                try
                {
                    nameColumn = Integer.parseInt(nameListQuery.substring(1));
                }
                catch( NumberFormatException e )
                {
                    fatal("Invalid syntax for nameListQuery: "+nameListQuery);
                    return;
                }
                nameListQuery = query;
            }
            initNameList(nameListQuery, nameColumn == -1 ? 1 : nameColumn);
            if( !valid )
                return;
        }
        else
        {
            String countQuery = properties.getProperty(COUNT_QUERY_PROPERTY);
            if( countQuery == null )
            {
                countQuery = makeCountQuery(query);
            }
            initSize(countQuery);
            if( !valid )
                return;
        }
        try
        {
            this.columnModel = new SqlQueryTableColumnModel(this, nameColumn);
        }
        catch( SQLException e )
        {
            fatal(e.getMessage());
        }
        if( this.columnModel.getColumnCount() == 0 )
        {
            fatal("No columns found");
            return;
        }
        chunkSize = 10000 / getColumnModel().getColumnCount();
        v_cache = null;
    }

    public String getFatalError()
    {
        return fatalError;
    }

    private void fatal(String message)
    {
        log.log(Level.SEVERE, getCompletePath() + ": " + message);
        valid = false;
        this.fatalError = message;
    }

    /**
     * Initializes collection name list
     * @param nameListQuery query to get name list
     * @param nameColumn
     */
    private void initNameList(String nameListQuery, int nameColumn)
    {
        ResultSet rs = null;
        Statement st = null;
        nameMap = new HashMap<>();
        nameList = new ArrayList<>();
        try
        {
            st = getConnection().createStatement();
            rs = st.executeQuery(nameListQuery);
            while( rs.next() )
            {
                String baseName = rs.getString(nameColumn);
                String name = baseName;
                int i=0;
                while(nameMap.containsKey(name))
                {
                    name = baseName+"_"+(++i);
                }
                nameMap.put(name, nameList.size());
                nameList.add(name);
            }
            size = nameList.size();
        }
        catch( SQLException e )
        {
            fatal(e.getMessage());
        }
        finally
        {
            SqlUtil.close(st, rs);
        }
    }

    /**
     * Initializes collection size
     * @param countQuery
     */
    private void initSize(String countQuery)
    {
        try
        {
            size = SqlUtil.queryInt(getConnection(), countQuery, -1);
            if( size == -1 )
            {
                fatal("Unable to fetch size with query " + countQuery);
            }
        }
        catch( BiosoftSQLException e )
        {
            fatal(e.getMessage());
        }
    }

    // TODO: make more robust
    private static Pattern countPattern = Pattern.compile("^SELECT\\s(.+)\\sFROM\\s", Pattern.CASE_INSENSITIVE);
    private static String makeCountQuery(String query)
    {
        Matcher matcher = countPattern.matcher(query);
        if( matcher.find() )
        {
            return matcher.replaceFirst("SELECT COUNT(*) FROM ");
        }
        return "SELECT COUNT(*) FROM (" + query + ") __original__query";
    }

    @Override
    public @Nonnull Iterator<RowDataElement> iterator()
    {
        if( !valid )
            return ListUtil.emptyIterator();
        return new Iterator<RowDataElement>()
        {
            int pos = 0;

            @Override
            public boolean hasNext()
            {
                return pos < size;
            }

            @Override
            public RowDataElement next()
            {
                if( !hasNext() )
                    throw new NoSuchElementException();
                return getAt(pos++);
            }
        };
    }

    @Override
    public void sortTable(int columnNumber, boolean dir)
    {
        // Not implemented
    }

    private synchronized RowDataElement[] getChunk(int chunkNum)
    {
        WeakReference<RowDataElement[]> ref = chunks.get(chunkNum);
        lastChunk = ref == null ? null : ref.get();
        if( lastChunk == null )
        {
            ResultSet resultSet = null;
            Statement st = null;
            try
            {
                st = connectionHolder.getConnection().createStatement();
                st.execute("SET SESSION group_concat_max_len="+GROUP_CONCAT_MAX_LEN);
                resultSet = st.executeQuery(query + " LIMIT " + ( chunkNum * chunkSize ) + "," + chunkSize);
                lastChunk = new RowDataElement[chunkSize];
                int i = 0;
                while( resultSet.next() && i < chunkSize )
                {
                    lastChunk[i] = createRow( ( chunkNum * chunkSize + i ), resultSet);
                    i++;
                }
            }
            catch( SQLException e )
            {
                log.log(Level.SEVERE, "Cannot execute " + query, e);
                return null;
            }
            finally
            {
                SqlUtil.close(st, resultSet);
            }
            chunks.put( chunkNum, new WeakReference<>( lastChunk ) );
        }
        return lastChunk;
    }

    private RowDataElement createRow(int n, ResultSet resultSet)
    {
        RowDataElement row = new RowDataElement(getName(n), this);
        Object[] values = new Object[getColumnModel().getColumnCount()];
        for( int i = 0; i < values.length; i++ )
        {
            try
            {
                TableColumn column = getColumnModel().getColumn(i);
                int index = ((SqlQueryTableColumnModel)getColumnModel()).getColumnQueryIndex(i);
                if(index > -1)
                    values[i] = column.getType().convertValue(resultSet.getObject(index));
            }
            catch( SQLException e )
            {
                log.log(Level.SEVERE, e.getMessage());
            }
        }
        row.setValues(values);
        return row;
    }

    private RowDataElement getRow(int rowIdx)
    {
        if( !valid || rowIdx < 0 || rowIdx >= size )
            return null;
        return getChunk(rowIdx / chunkSize)[rowIdx % chunkSize];
    }

    @Override
    public RowDataElement getAt(int rowIdx)
    {
        return getRow(rowIdx);
    }

    @Override
    public Object getValueAt(int rowIdx, int columnIdx)
    {
        if( !valid )
            return null;
        return getRow(rowIdx).getValues()[columnIdx];
    }

    @Override
    public void setValueAt(int rowIdx, int columnIdx, Object value)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName(int rowIdx)
    {
        if( !valid )
            return null;
        return getNameList().get(rowIdx);
    }

    @Override
    public Connection getConnection() throws BiosoftSQLException
    {
        return connectionHolder.getConnection();
    }

    @Override
    public boolean isMutable()
    {
        return false;
    }

    @Override
    public int getSize()
    {
        return valid?size:0;
    }

    @Override
    public @Nonnull List<String> getNameList()
    {
        if( !valid )
            return Collections.<String> emptyList();
        if( nameList != null )
            return Collections.unmodifiableList(nameList);
        return new AbstractList<String>()
        {
            @Override
            public String get(int index)
            {
                if( index < 0 || index >= size )
                    throw new IndexOutOfBoundsException();
                return String.valueOf(index + 1);
            }

            @Override
            public int size()
            {
                return size;
            }
        };
    }

    @Override
    public RowDataElement get(String name) throws Exception
    {
        try
        {
            if( nameMap != null )
                return getAt(nameMap.get(name));
            return getAt(Integer.parseInt(name) - 1);
        }
        catch( Exception e )
        {
            return null;
        }
    }

    public String getQuery()
    {
        return query;
    }

    public synchronized void clearCache()
    {
        chunks = new HashMap<>();
        lastChunk = null;
    }
    
    @Override
    public void recalculateTable(FunctionJobControl jobControl)
    {
        if( jobControl != null )
        {
            jobControl.functionStarted();
            jobControl.setPreparedness(0);
        }
        clearCache();
        firePropertyChange("*", null, null);
        if( jobControl != null )
        {
            jobControl.functionFinished();
        }
    }
}
