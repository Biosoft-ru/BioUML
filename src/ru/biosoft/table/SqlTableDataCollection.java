package ru.biosoft.table;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.json.JSONObject;

import com.developmentontheedge.beans.annot.PropertyName;

import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.SqlDataInfo;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementReadException;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.security.Permission;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.sql.BulkInsert;
import ru.biosoft.access.sql.FastBulkInsert;
import ru.biosoft.access.sql.Query;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.access.sql.SqlDataElement;
import ru.biosoft.access.sql.SqlLazyCharSequence;
import ru.biosoft.access.sql.SqlList;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.table.TableColumn.Nature;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.ApplicationUtils;
import ru.biosoft.util.LazySubSequence;

@PropertyName ( "table" )
public class SqlTableDataCollection extends TableDataCollection implements SqlDataElement
{
    public static final String TABLE_PREFIX = "table";
    public static final String ROW_ID_COLUMN = "row_id";
    public static final String COLUMN_MODEL_PROPERTY = "ColumnModel";

    protected String id;
    protected String tableId;
    private boolean initialized = false;
    protected int size = -1;
    protected List<String> names = null;
    protected Properties initProperties;
    protected int queryLength;
    private final Map<String, String> sqlColumnNameMap = new HashMap<>();
    private final Map<String, Integer> columnLengths = new HashMap<>();

    protected BulkInsert inserter = null;

    public SqlTableDataCollection(DataCollection<?> origin, Properties properties, TableDataCollection from) throws Exception
    {
        super(origin, properties);

        boolean shouldCreate = false;
        if( properties.getProperty( SqlDataInfo.ID_PROPERTY ) == null ) // Create new SqlTableDataCollection
        {
            this.id = SqlDataInfo.initDataElement( getConnection(), origin, properties, TABLE_PREFIX );
            getInfo().getProperties().setProperty( SqlDataInfo.ID_PROPERTY, this.id );
            shouldCreate = true;
        }
        else
        {
            this.id = properties.getProperty( SqlDataInfo.ID_PROPERTY );
            shouldCreate = !SqlUtil.hasTable( getConnection(), TABLE_PREFIX + "_" + this.id );
        }
        initProperties = (Properties)properties.clone();
        tableId = TABLE_PREFIX + "_" + this.id;
        if( shouldCreate || from != null )
        {
            createSQLTable();
        }
        if( from != null )
        {
            SqlDataInfo.storeProperties( getConnection(), id, properties );
            for( TableColumn col : from.getColumnModel() )
            {
                getColumnModel().addColumn( col );
            }
            for( RowDataElement row : from )
            {
                addRow( row );
            }
            finalizeAddition();
        }
        //initNameMap();
        addPropertyChangeListener(new ColumnPropertyChangeListener());
    }

    public SqlTableDataCollection(DataCollection<?> origin, Properties properties) throws Exception
    {
        this(origin, properties, null);
    }

    protected boolean isInitialized()
    {
        return initialized;
    }

    protected String getSQLColumnName(String name)
    {
        String result = sqlColumnNameMap.get(name);
        if( result == null )
        {
            result = name;
            result = result.replaceAll("[\\.\\-\\ ]", "_");
            result = result.replaceAll("\\W", "");
            result = result.replaceAll("_+", "_");
            if( Pattern.matches("\\d.*", result) )
                result = "col" + result;
            if( result.equals(ROW_ID_COLUMN) )
                result = "col" + result;
            if( result.length() > 52 )
                result = result.substring(0, 52);
            if(result.isEmpty())
                result = "col";
            String baseResult = result;
            int i = 0;
            while( sqlColumnNameMap.containsValue(result) )
            {
                result = baseResult + "_" + ( ++i );
            }
            sqlColumnNameMap.put(name, result);
        }
        return result;
    }

    protected String getSQLColumnName(int index)
    {
        return ( index >= 0 && index < getColumnModel().getColumnCount() ) ? getSQLColumnName(getColumnModel().getColumn(index).getName())
                : ROW_ID_COLUMN;
    }

    protected Query query(String template)
    {
        return new Query(template).name("table", tableId).name("idColumn", ROW_ID_COLUMN);
    }

    @Override
    public SqlColumnModel getColumnModel()
    {
        if( !isInitialized() )
        {
            synchronized( this )
            {
                if( !isInitialized() )
                {
                    try
                    {
                        initColumnModel();
                    }
                    catch( SQLException e )
                    {
                        log.log(Level.SEVERE, this + ": unable to initialize column model", e);
                    }
                }
            }
        }
        return (SqlColumnModel)columnModel;
    }

    protected void initColumnModel() throws SQLException, BiosoftSQLException
    {
        columnModel = new SqlColumnModel(this);
        int id = getId();
        ResultSet rs = null;
        Statement st = null;
        Map<String, TableColumn> sqlColumnNames = new HashMap<>();
        if( id > 0 )
        {
            try
            {
                st = getConnection().createStatement();
                rs = SqlUtil.executeQuery(st, new Query("SELECT * FROM column_info WHERE data_element_id=$id$ ORDER BY position").num(id));
                while( rs.next() )
                {
                    String type = rs.getString("type");
                    DataType typeObj = DataType.fromString(type);
                    String name = rs.getString("name");
                    // displayName temporary unused as it produces problems (for some reason name=displayName in TableColumn)
                    //String displayName = rs.getString("display_name");
                    String displayName = name;
                    if( displayName.equals("") )
                        displayName = name;
                    String description = rs.getString("description");
                    if( description.equals("") )
                        description = name;

                    TableColumn col = new TableColumn(name, displayName, description, typeObj, rs.getString("expression"));

                    String nature = rs.getString("nature");
                    Nature natureObj = nature.equals("DBREF") ? Nature.DBREF : nature.equals("SAMPLE") ? Nature.SAMPLE : Nature.NONE;
                    col.setNature(natureObj);
                    sqlColumnNames.put(getSQLColumnName(col.getName()), col);
                }
            }
            finally
            {
                SqlUtil.close(st, rs);
            }
        }
        else
        {
            SqlColumnModel model2 = new SqlColumnModel(this);
            try
            {
                String json = getInfo().getProperty(COLUMN_MODEL_PROPERTY);
                if(json != null)
                    model2.fromJSON(new JSONObject(json));
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, getCompletePath()+": Unable to init column model: "+ExceptionRegistry.log(e)+"; using fallback");
            }
            for( TableColumn col : model2 )
            {
                sqlColumnNames.put(getSQLColumnName(col.getName()), col);
            }
        }
        PreparedStatement updateColumnStatement = null, insertColumnStatement = null;
        try
        {
            if( id > 0 )
            {
                updateColumnStatement = getConnection().prepareStatement(
                        "UPDATE column_info SET type=?,position=? WHERE data_element_id=? AND name=?");
                insertColumnStatement = getConnection()
                        .prepareStatement(
                                "INSERT INTO column_info(data_element_id,name,display_name,description,type,expression,nature,position) VALUES(?,?,?,?,?,?,?,?)");
            }
            st = getConnection().createStatement();
            rs = SqlUtil.executeQuery(st, Query.describe(tableId));
            int position = 0;
            while( rs.next() )
            {
                try
                {
                    String columnName = rs.getString("Field");
                    if( columnName.equals("row_id") )
                        continue;
                    String decoratedColumnName = sqlColumnNames.containsKey(columnName) ? columnName : ( "`" + columnName + "`" );
                    position++;
                    if( sqlColumnNames.containsKey(decoratedColumnName) ) // already existing column: update its type and position
                    {
                        TableColumn col = sqlColumnNames.get(decoratedColumnName);
                        columnLengths.put(col.getName(), SqlDataInfo.getColumnContentLength(rs.getString("Type")));
                        columnModel.addColumn(columnModel.cloneTableColumn(col));
                        if( id > 0 )
                        {
                            updateColumnStatement.setString(1, col.getType().toString());
                            updateColumnStatement.setInt(2, position);
                            updateColumnStatement.setInt(3, id);
                            updateColumnStatement.setString(4, col.getName());
                            updateColumnStatement.execute();
                        }
                        sqlColumnNames.remove(decoratedColumnName);
                    }
                    else
                    // new column
                    {
                        Class<?> javaColumnType = SqlDataInfo.getJavaColumnType(rs.getString("Type"));
                        if( javaColumnType == null )
                            javaColumnType = String.class;
                        TableColumn col = new TableColumn(columnName, javaColumnType);
                        columnLengths.put(columnName, SqlDataInfo.getColumnContentLength(rs.getString("Type")));
                        columnModel.addColumn(columnModel.cloneTableColumn(col));
                        if( id > 0 )
                        {
                            insertColumnStatement.setInt(1, id);
                            insertColumnStatement.setString(2, col.getName());
                            insertColumnStatement.setString(3, col.getDisplayName());
                            insertColumnStatement.setString(4, col.getShortDescription());
                            insertColumnStatement.setString(5, col.getType().toString());
                            insertColumnStatement.setString(6, col.getExpression());
                            insertColumnStatement.setString(7, col.getNature().name());
                            insertColumnStatement.setInt(8, position);
                            insertColumnStatement.execute();
                        }
                    }
                }
                catch( SQLException e )
                {
                    log.log(Level.SEVERE, "While doing DESCRIBE " + tableId + ": " + ExceptionRegistry.log(e));
                }
            }
        }
        finally
        {
            SqlUtil.close(st, rs);
            SqlUtil.close(insertColumnStatement, null);
            SqlUtil.close(updateColumnStatement, null);
        }
        // remove columns which absent in the table, but present in column_info
        if( id > 0 )
        {
            try (PreparedStatement removeColumnStatement = getConnection().prepareStatement(
                    "DELETE FROM column_info WHERE data_element_id=? AND name=?" ))
            {
                for( TableColumn col : sqlColumnNames.values() )
                {
                    removeColumnStatement.setInt( 1, id );
                    removeColumnStatement.setString( 2, col.getName() );
                    removeColumnStatement.execute();
                }
            }
        }
        sortOrder.set();
        initialized = true;
    }

    protected void initTableStatus()
    {
        if( queryLength > 0 )
            return;
        ResultSet rs = null;
        Statement st = null;
        queryLength = Math.max(200000 / ( getColumnModel().getColumnCount() + 1 ), 50);
        try
        {
            long avgRowLength = SqlUtil.getAvgRowLength( getConnection(), tableId );
            if( avgRowLength != 0 )
                 queryLength = Math.max((int) ( 10000000 / avgRowLength ), 50);
        }
        catch( Exception e )
        {
            ExceptionRegistry.log(e);
        }
        finally
        {
            SqlUtil.close(st, rs);
        }
    }

    protected void createSQLTable() throws BiosoftSQLException
    {
        Connection connection = getConnection();
        SqlUtil.dropTable(connection, tableId);
        int id = getId();
        if( id > 0 )
        {
            SqlUtil.execute(connection, new Query("DELETE FROM column_info WHERE data_element_id = $id$").num(id));
            SqlUtil.execute(connection, new Query("DELETE FROM de_info WHERE data_element_id = $id$").num(id));
        }
        SqlUtil.execute( connection,
                query( "CREATE TABLE $table$($idColumn$ VARCHAR(255) NOT NULL,PRIMARY KEY($idColumn$)) ENGINE=InnoDB CHARSET=utf8" ) );
        size = 0;
    }

    protected String getSQLType(String type)
    {
        return type.equals("Float") ? "DOUBLE" : type.equals("Integer") ? "INT" : "VARCHAR(" + SqlDataInfo.INITIAL_COLUMN_LENGTH + ")";
    }

    protected String getSQLType(int length)
    {
        int varCharLength = SqlDataInfo.INITIAL_COLUMN_LENGTH;
        int maxVarcharLength = SqlDataInfo.MAX_VARCHAR_LENGTH / 8;
        while( varCharLength < length && varCharLength <= maxVarcharLength )
            varCharLength *= 2;
        return ( varCharLength > maxVarcharLength ) ? "LONGTEXT" : "VARCHAR(" + varCharLength + ")";
    }

    protected void addColumnToDb(TableColumn col) throws Exception
    {
        if( !isInitialized() )
            return;
        finalizeAddition();
        String type = col.getType().name();
        SqlUtil.execute(
                getConnection(),
                query("ALTER TABLE $table$ ADD COLUMN $column$ $type$ DEFAULT NULL").name("column", getSQLColumnName(col.getName())).raw(
                        "type", getSQLType(type)));
        columnLengths.put(col.getName(), SqlDataInfo.INITIAL_COLUMN_LENGTH);
        int id = getId();
        if( id > 0 )
        {
            try (PreparedStatement ps = getConnection()
                    .prepareStatement(
                            "INSERT INTO column_info(data_element_id,name,display_name,description,type,expression,nature,position) VALUES(?,?,?,?,?,?,?,?)" ))
            {
                ps.setInt( 1, id );
                ps.setString( 2, col.getName() );
                ps.setString( 3, col.getDisplayName() );
                ps.setString( 4, col.getShortDescription() );
                ps.setString( 5, type );
                ps.setString( 6, col.getExpression() );
                ps.setString( 7, col.getNature().name() );
                ps.setInt( 8, getColumnModel().getColumnCount() );
                ps.execute();
            }
        }
        else
        {
            getInfo().getProperties().setProperty(COLUMN_MODEL_PROPERTY, getColumnModel().toJSON().toString());
            getCompletePath().save(this);
        }
        v_cache.clear();
        getIteratorRow = -1;
        getIteratorName = null;
        inserter = null;
        queryLength = 0;
    }

    protected void removeColumnFromDb(int columnPos) throws Exception
    {
        if( !isInitialized() )
            return;
        finalizeAddition();
        SqlUtil.execute(getConnection(), query("ALTER TABLE $table$ DROP COLUMN $column$").name("column", getSQLColumnName(columnPos)));
        String name = getColumnModel().getColumn(columnPos).getName();
        columnLengths.remove(name);
        int id = getId();
        if( id > 0 )
        {
            SqlUtil.execute(getConnection(), new Query("DELETE FROM column_info WHERE data_element_id=$id$ AND name=$name$").num("id", id)
                    .str("name", name));
        }
        else
        {
            getInfo().getProperties().setProperty(COLUMN_MODEL_PROPERTY, getColumnModel().toJSON().toString());
            getCompletePath().save(this);
        }
        v_cache.clear();
        getIteratorRow = -1;
        getIteratorName = null;
        inserter = null;
    }

    protected void changeDbColumn(String oldName, String oldType, TableColumn col) throws Exception
    {
        if( !isInitialized() )
            return;
        finalizeAddition();
        String type = col.getType().name();
        if( !oldName.equals(col.getName()) )
            columnLengths.remove(oldName);
        if( !oldName.equals(col.getName()) || !oldType.equals(type) )
        {
            String sqlMode = SqlUtil.queryString(getConnection(), "SELECT @@SESSION.sql_mode");
            SqlUtil.execute(getConnection(), "SET SESSION sql_mode=''");
            String sqlType = getSQLType(type);
            if( !type.equals("Float") && !type.equals("Integer") )
            {
                sqlType = getSQLType(SqlUtil.queryInt(getConnection(),
                        query("SELECT MAX(LENGTH($column$)) FROM $table$").name("column", getSQLColumnName(oldName))));
                columnLengths.put(col.getName(), SqlDataInfo.getColumnContentLength(sqlType));
            }
            SqlUtil.execute(getConnection(), query("ALTER TABLE $table$ CHANGE COLUMN $oldColumn$ $newColumn$ $type$ DEFAULT NULL")
                    .name("oldColumn", getSQLColumnName(oldName)).name("newColumn", getSQLColumnName(col.getName()))
                    .raw("type", sqlType));
            if( sqlMode != null && !sqlMode.equals("") )
                SqlUtil.execute(getConnection(), new Query("SET SESSION sql_mode=$mode$").str(sqlMode));
            v_cache.clear();
        }
        int id = getId();
        if( id > 0 )
        {
            try (PreparedStatement ps = getConnection()
                    .prepareStatement(
                            "UPDATE column_info SET name=?,display_name=?,description=?,type=?,expression=?,nature=? WHERE data_element_id=? AND name=?" ))
            {
                ps.setString( 1, col.getName() );
                ps.setString( 2, col.getDisplayName() );
                ps.setString( 3, col.getShortDescription() );
                ps.setString( 4, type );
                ps.setString( 5, col.getExpression() );
                ps.setString( 6, col.getNature().name() );
                ps.setInt( 7, id );
                ps.setString( 8, oldName );
                ps.execute();
            }
        }
        else
        {
            getInfo().getProperties().setProperty(COLUMN_MODEL_PROPERTY, getColumnModel().toJSON().toString());
            getCompletePath().save(this);
        }
        if( !oldType.equals(type) )
        {
            try (PreparedStatement ps = getConnection().prepareStatement(
                    query( "SELECT $column$ FROM $table$ WHERE $idColumn$=?" ).name( "column", getSQLColumnName( col.getName() ) ).get() ))
            {
                for( Object rdeObj : v_cache.values() )
                {
                    if( ! ( rdeObj instanceof RowDataElement ) )
                        continue;
                    RowDataElement rowDataElement = (RowDataElement)rdeObj;
                    ps.setString( 1, rowDataElement.getName() );
                    try (ResultSet rs = ps.executeQuery())
                    {
                        if( rs.next() )
                        {
                            rowDataElement.setValue( col.getName(), col.getType().convertValue( rs.getObject( 1 ) ) );
                        }
                    }
                }
            }
        }
        getIteratorRow = -1;
        getIteratorName = null;
        inserter = null;
    }

    public int getId()
    {
        try
        {
            return Integer.parseInt(id);
        }
        catch( Exception e )
        {
            return -1;
        }
    }

    @Override
    public String getTableId()
    {
        return tableId;
    }

    @Override
    public String[] getUsedTables()
    {
        String relatedTable = getInfo().getProperty(SqlDataElement.RELATED_TABLE_PROPERTY);
        if( relatedTable != null )
            return new String[] {tableId, relatedTable};
        return new String[] {tableId};
    }

    @Override
    public Connection getConnection() throws BiosoftSQLException
    {
        return SqlConnectionPool.getConnection(this);
    }

    Iterator<RowDataElement> getIterator = null;
    int getIteratorRow = -1;
    String getIteratorName = null;
    int lastGetRequest = 0;
    int incrementQueryScore = 0;
    @Override
    public synchronized RowDataElement getAt(int rowIdx)
    {
        if( getIteratorRow == rowIdx + 1 && getIteratorName != null )
        {
            try
            {
                return get(getIteratorName);
            }
            catch( Exception e )
            {
                throw ExceptionRegistry.translateException(e);
            }
        }
        // New element was requested too far away the old one
        if( getIteratorRow == -1 || getIteratorRow > rowIdx || getIteratorRow < rowIdx - 5 )
        {
            getIteratorRow = -1;
            getIterator = null;
            if( names != null )
            {
                // We can know the name of new element
                if( lastGetRequest == rowIdx - 1 )
                    incrementQueryScore++;
                else if( lastGetRequest != rowIdx )
                    incrementQueryScore = 0;
                lastGetRequest = rowIdx;
                String name = names.get(rowIdx);
                RowDataElement element = v_cache.get(name);
                // Return element if it's in the cache: no iterator required
                if( element != null )
                    return element;
                // Not in the cache: we were consequently asked for neighboring elements
                // consider this as the good idea to create an iterator
                if( incrementQueryScore > 5 )
                {
                    getIterator = new SQLIterator(rowIdx);
                    getIteratorRow = rowIdx;
                }
                else
                {
                    try
                    {
                        return get(name);
                    }
                    catch( Exception e )
                    {
                        throw ExceptionRegistry.translateException(e);
                    }
                }
            }
            else
            {
                // No names available: just create iterator
                getIterator = new SQLIterator(rowIdx);
                getIteratorRow = rowIdx;
            }
        }
        while( rowIdx > getIteratorRow )
        {
            getIterator.next();
            getIteratorRow++;
        }
        if( !getIterator.hasNext() )
            return null;
        RowDataElement de = getIterator.next();
        getIteratorName = de.getName();
        getIteratorRow++;
        return de;
    }

    @Override
    public int getSize()
    {
        if( size == -1 )
        {
            try
            {
                size = SqlUtil.getRowsCount(getConnection(), tableId);
            }
            catch( BiosoftSQLException e )
            {
                e.log();
                size = 0;
            }
        }
        return size;
    }

    @Override
    public @Nonnull List<String> getNameList()
    {
        if( names == null )
        {
            try
            {
                initNameMap();
            }
            catch( BiosoftSQLException e )
            {
                e.log();
                names = Collections.emptyList();
            }
        }
        return Collections.unmodifiableList(names);
    }

    @Override
    public String getName(int rowIdx)
    {
        if( names == null )
        {
            try
            {
                initNameMap();
            }
            catch( BiosoftSQLException e )
            {
                e.log();
                names = Collections.emptyList();
            }
        }
        return names.get(rowIdx);
    }

    @Override
    public Object getValueAt(int rowIdx, int columnIdx)
    {
        RowDataElement row = getAt(rowIdx);
        Object[] values = row.getValues();
        return values[columnIdx];
    }

    @Override
    public void setValueAt(int rowIdx, int columnIdx, Object value)
    {
        RowDataElement row = getAt(rowIdx);
        Object oldValue = row.getValues(false)[columnIdx];
        row.getValues()[columnIdx] = value;
        try
        {
            doPut(row, false);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, e.getMessage(), e);
            return;
        }
        firePropertyChange("values", oldValue, value);
    }

    protected void initNameMap()
    {
        if( getSize() > ApplicationUtils.getMaxSortingSize() )
        {
            names = new SqlList(this, query("SELECT $idColumn$ FROM $table$"), getSize());
        }
        else
        {
            List<String> newNames = SqlUtil.queryStrings( getConnection(), query( "SELECT $idColumn$ FROM $table$ " ) + getOrderByClause() );
            size = newNames.size();
            names = newNames;
            if( !sqlSort )
            {
                final Map<String, Object> values = new HashMap<>();
                Iterator<RowDataElement> iterator = iterator();
                while( iterator.hasNext() )
                {
                    RowDataElement element = iterator.next();
                    values.put(element.getName(), element.getValues()[sortOrder.getColumnNumber()]);
                }
                Collections.sort(newNames, (name1, name2) -> {
                    Object value1 = values.get(name1);
                    Object value2 = values.get(name2);
                    int priority1 = ( value1 instanceof Comparable<?> ) ? 1 : 0;
                    int priority2 = ( value2 instanceof Comparable<?> ) ? 1 : 0;
                    int result = 0;
                    try
                    {
                        result = ( priority1 & priority2 ) > 0 ? ( (Comparable)value1 ).compareTo(value2) : priority2 - priority1;
                    }
                    catch( Exception e )
                    {
                    }
                    return sortOrder.getDirection() ? result : -result;
                });
            }
        }
    }

    protected boolean sqlSort = true;
    @Override
    public void sortTable(int columnNumber, boolean dir)
    {
        SortOrder oldSortOrder = sortOrder;
        sortOrder = new SortOrder(columnNumber, dir);
        if( sortOrder.equals(oldSortOrder) )
            return;
        getIteratorRow = -1;
        getIteratorName = null;
        try
        {
            sqlSort = sortOrder.getColumnNumber() < 0 || getColumnModel().getColumn(sortOrder.getColumnNumber()).isExpressionEmpty();
            initNameMap();
            getInfo().getProperties().setProperty(SORT_ORDER_PROPERTY, sortOrder.toString());
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
        firePropertyChange("sortOrder", oldSortOrder, sortOrder);
    }

    @Override
    public boolean isSortingSupported()
    {
        return getSize() <= ApplicationUtils.getMaxSortingSize();
    }

    @Override
    public Iterator<RowDataElement> getSortedIterator(String field, boolean direction, int from, int to)
    {
        if( field.equals(DataCollectionConfigConstants.NAME_PROPERTY) )
            sortTable( -1, direction);
        else
            sortTable(getColumnModel().optColumnIndex(field), direction);
        return new SQLIterator(from, to);
    }

    // Though name is present in rs (rs.getString(1)) we pass it to reuse already created String object
    protected RowDataElement createRow(String name, ResultSet rs) throws BiosoftSQLException
    {
        try
        {
            RowDataElement element = new RowDataElement(name, SqlTableDataCollection.this);
            List<Object> rowData = new ArrayList<>();
            int count = getColumnModel().getColumnCount();
            TableColumn[] columns = getColumnModel().getColumns();
            for( int i = 0; i < count; i++ )
            {
                Object value = rs.getObject(i + 2);
                if( columns[i].getType().supportsLazyCharSequenceInit() && value instanceof String ) // Lazy initialization
                {
                    if( ( (String)value ).length() > 500 )
                        value = new SqlLazyCharSequence(this, query("SELECT $column$ FROM $table$ WHERE $idColumn$=$value$").name("column",
                                getSQLColumnName(columns[i].getName())).str("value", name));
                    else
                        value = new LazySubSequence((String)value, 0, ( (String)value ).length());
                }
                rowData.add(columns[i].getType().convertValue(value));
            }
            element.setValues(rowData.toArray());
            return element;
        }
        catch( SQLException e )
        {
            throw new BiosoftSQLException(this, e);
        }
    }

    @Override
    public boolean contains(String name) throws BiosoftSQLException
    {
        if( v_cache != null && v_cache.get(name) != null )
            return true;
        return SqlUtil.hasResult(getConnection(), Query.byCondition(tableId, ROW_ID_COLUMN, name));
    }

    @Override
    public synchronized RowDataElement doGet(String name) throws BiosoftSQLException
    {
        if( v_cache.get(name) != null )
            return v_cache.get(name);
        ResultSet rs = null;
        RowDataElement de = null;
        Statement st = null;
        try
        {
            st = SqlUtil.createStatement(getConnection());
            rs = SqlUtil.executeAndAdvance(st, Query.byCondition(tableId, ROW_ID_COLUMN, name));
            if( rs != null )
                de = createRow(name, rs);
        }
        finally
        {
            SqlUtil.close(st, rs);
        }
        if( de != null )
            v_cache.put(name, de);
        return de;
    }

    @Override
    public void doPut(RowDataElement de, boolean isNew) throws Exception
    {
        List<String> oldNames = names;
        int curSize1 = size;
        if( !isNew )
        {
            try
            {
                doRemove(de.getName());
            }
            catch( Exception e )
            {
                size = curSize1;
                throw e;
            }
        }
        int curSize2 = size;
        try
        {
            addRow(de);
            finalizeAddition();
        }
        catch( Exception e )
        {
            size = curSize2;
            throw e;
        }
        if( isNew || curSize1 != size )
            names = null;
        else
            names = oldNames;
    }

    @Override
    public void doRemove(String name) throws Exception
    {
        int removed = SqlUtil.executeUpdate(getConnection(), query("DELETE FROM $table$ WHERE $idColumn$ = $name$").str("name", name));
        if(size != -1)
            size -= removed;
        names = null;
    }

    @Override
    public @Nonnull
    Iterator<RowDataElement> iterator()
    {
        getColumnModel(); // initialize
        return new SQLIterator();
    }

    @Override
    public StreamEx<RowDataElement> stream()
    {
        return StreamEx.of( spliterator() );
    }

    protected void prepareAddStatement() throws BiosoftSQLException
    {
        finalizeAddition();
        inserter = new FastBulkInsert(this, tableId);
    }

    /**
     * adds a row
     * This can be used for mass addition
     * Call finalizeAddition after you finished adding!
     * @param row row to add
     * @throws SQLException if DB error occurs
     * Duplicate row key will produce SQLException also
     */
    @Override
    public void addRow(RowDataElement row) throws BiosoftSQLException
    {
        if( inserter == null )
        {
            prepareAddStatement();
        }
        Object[] values = row.getValues(false);
        checkValues(values);
        Object[] parameters = new Object[values.length + 1];
        parameters[0] = row.getName();
        for( int i = 0; i < values.length; i++ )
        {
            if( getColumnModel().getColumn(i).getValueClass() != String.class && values[i] != null && values[i].equals("null") )
                parameters[i + 1] = null;
            else
            {
                parameters[i + 1] = values[i];
            }
        }
        inserter.insert(parameters);
        size++;
    }

    private void checkValues(Object[] values) throws BiosoftSQLException
    {
        for( int i = 0; i < values.length; i++ )
        {
            if( values[i] == null )
                continue;
            TableColumn col = getColumnModel().getColumn(i);
            DataType type = col.getType();
            if( type.isNumeric() )
                continue;
            int length = values[i].toString().length();
            if( length > columnLengths.get(col.getName()) )
            {
                finalizeAddition();
                String sqlType = getSQLType(length);
                columnLengths.put(col.getName(), SqlDataInfo.getColumnContentLength(sqlType));
                SqlUtil.execute(
                        getConnection(),
                        query("ALTER TABLE $table$ MODIFY COLUMN $column$ $type$ DEFAULT NULL").name("column",
                                getSQLColumnName(col.getName())).raw("type", sqlType));
            }
        }
    }

    @Override
    public void finalizeAddition() throws BiosoftSQLException
    {
        if( inserter != null )
        {
            inserter.flush();
            size = -1;
            names = null;
        }
    }

    protected String getOrderByClause()
    {
        return getSize() > ApplicationUtils.getMaxSortingSize() || !sqlSort ? "" : "ORDER BY "
                + ( sortOrder.getColumnNumber() == -1 && Boolean.valueOf(getInfo().getProperty(INTEGER_IDS)) ? "0+" : "" )
                + SqlUtil.quoteIdentifier(getSQLColumnName(sortOrder.getColumnNumber())) + " " + ( sortOrder.getDirection() ? "ASC" : "DESC" )
                + ( sortOrder.getColumnNumber() == -1 ? "" : ",1" );
    }

    /**
     * Reports a bound property update to any of the registered listeners. No
     * event is fired if old and new values are equal and non-null.
     */
    @Override
    public void firePropertyChange(PropertyChangeEvent evt)
    {
        columnEventSupport.firePropertyChange(evt);
    }

    @Override
    public boolean isMutable()
    {
        if( getOrigin() == null
                || ( SecurityManager.getPermissions(getOrigin().getCompletePath()).getPermissions() & Permission.WRITE ) != 0 )
            return super.isMutable();
        return false;
    }

    ////////////////////////////////////////////////////////////////////////////
    //  Inner classes
    //
    /**
     * Implement Iterator for iterate SqlDataCollection elements.
     * @see SQLDataCollection
     */
    protected class SQLIterator implements Iterator<RowDataElement>
    {
        /** Current result set for iterate through. */
        private ResultSet resultSet = null;

        private String name = null;

        private Statement statement = null;
        private int rowNumber = -1;

        private final int size = getSize();
        private final int queryLength;

        /**
         * Create iterator.
         * Ask DBMS for all DataElements and fill resultSet.
         */
        public SQLIterator()
        {
            this(0, getSize());
        }

        public SQLIterator(int from)
        {
            this(from, getSize());
        }

        public SQLIterator(int from, int to)
        {
            initTableStatus();
            queryLength = Math.min(SqlTableDataCollection.this.queryLength, to - from);
            init(from);
        }

        private void init(int skip)
        {
            try
            {
                if( sqlSort || getSize() > ApplicationUtils.getMaxSortingSize() )
                {
                    try
                    {
                        if( statement != null )
                            statement.close();
                        statement = SqlUtil.createStatement(getConnection());
                        resultSet = SqlUtil.executeQuery(statement, Query.all(tableId) + " " + getOrderByClause() + " LIMIT " + skip + ","
                                + queryLength);
                        if( !resultSet.next() )
                            resultSet = null;
                    }
                    catch( SQLException exc )
                    {
                        throw new BiosoftSQLException(getConnection(), null, exc);
                    }
                }
                rowNumber = skip;
            }
            catch( BiosoftSQLException e )
            {
                throw new DataElementReadException(e, getCompletePath(), "row#" + skip);
            }
        }

        @Override
        protected void finalize() throws Throwable
        {
            SqlUtil.close(statement, resultSet);
        }

        /**
         * Implement Iterator.hasNext().
         * @see java.util.Iterator#hasNext()
         */
        @Override
        public boolean hasNext()
        {
            return rowNumber < size && ( !sqlSort || resultSet != null );
        }

        /**
         * Implement Iterator.next().
         * @see java.util.Iterator#next()
         */
        @Override
        public RowDataElement next()
        {
            RowDataElement element = null;
            if( !hasNext() )
            {
                throw new NoSuchElementException("SqlTableDataCollection.iterator() has no more elements.");
            }
            if( !sqlSort )
            {
                try
                {
                    name = getName(rowNumber);
                    if( name == null )
                    {
                        rowNumber++;
                        return null;
                    }
                    element = doGet(name);
                    rowNumber++;
                }
                catch( Exception exc )
                {
                    throw new DataElementReadException(exc, getCompletePath(), "row#" + rowNumber);
                }
            }
            else
            {
                int curRow = rowNumber;
                try
                {
                    int row = resultSet.getRow();

                    name = resultSet.getString(1);
                    if( name != null )
                    {
                        element = v_cache.get(name);
                        if( element == null )
                        {
                            element = createRow(name, resultSet);
                            v_cache.put(name, element);
                        }
                        rowNumber++;
                    }

                    if( row == resultSet.getRow() )
                    {
                        if( !resultSet.next() )
                        {
                            resultSet.close();
                            init(rowNumber);
                        }
                    }
                    else
                    {
                        if( resultSet.isAfterLast() )
                        {
                            resultSet.close();
                            init(rowNumber);
                        }
                    }
                }
                catch( Exception exc )
                {
                    init(curRow);
                    return next();
                }
            }
            return element;
        }

        @Override
        public void remove()
        {
            if( name != null )
            {
                try
                {
                    resultSet.deleteRow();
                    SqlTableDataCollection.this.remove(name);
                }
                catch( Exception e )
                {
                    throw new UnsupportedOperationException("This method can be used only once after method next().");
                }
                name = null;
            }
        }
    }// end of SQLIterator

    private class ColumnPropertyChangeListener implements PropertyChangeListener
    {
        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            try
            {
                if( evt.getPropertyName().equals("columnInfo.name") )
                {
                    TableColumn oldCol = (TableColumn)evt.getSource();
                    oldCol.setDisplayName(oldCol.getName());
                    TableColumn col = getColumnModel().cloneTableColumn(oldCol);
                    getColumnModel().getColumns()[getColumnModel().getColumnIndex(col.getName())] = col;
                    changeDbColumn(evt.getOldValue().toString(), col.getType().name(), col);
                }
                else if( evt.getPropertyName().equals("columnInfo.type") )
                {
                    TableColumn col = (TableColumn)evt.getSource();
                    changeDbColumn(col.getName(), evt.getOldValue().toString(), col);
                }
                else if( evt.getPropertyName().startsWith("columnInfo.") )
                {
                    TableColumn col = (TableColumn)evt.getSource();
                    changeDbColumn(col.getName(), col.getType().name(), col);
                }
            }
            catch( Exception e )
            {
                ExceptionRegistry.log(e);
            }
        }
    }

    public Map<String, String> getColumnMatchingCopy()
    {
        if( !isInitialized() )
            getColumnModel();
        return Collections.unmodifiableMap( sqlColumnNameMap );
    }

    public static @Nonnull SqlTableDataCollection createEmptyCopyWithSameColumns(SqlTableDataCollection sourceTable,
            DataElementPath newElementPath)
    {
        SqlTableDataCollection resultTable = (SqlTableDataCollection)TableDataCollectionUtils.createTableDataCollection( newElementPath );

        ColumnModel oldCm = sourceTable.getColumnModel();
        ColumnModel newCm = resultTable.getColumnModel();
        for( TableColumn tc : oldCm )
        {
            newCm.addColumn( newCm.cloneTableColumn( tc ) );
        }
        CollectionFactoryUtils.save( resultTable );

        for( TableColumn col : newCm )
        {
            String columnName = col.getName();
            int length = sourceTable.columnLengths.getOrDefault( columnName, -1 );
            if( length == -1 )
                continue;

            String sqlType = resultTable.getSQLType( length );
            resultTable.columnLengths.put( columnName, SqlDataInfo.getColumnContentLength( sqlType ) );
            SqlUtil.execute( resultTable.getConnection(),
                    resultTable.query( "ALTER TABLE $table$ MODIFY COLUMN $column$ $type$ DEFAULT NULL" )
                            .name( "column", resultTable.getSQLColumnName( columnName ) )
                            .raw( "type", sqlType )
            );
        }

        return resultTable;
    }
}
