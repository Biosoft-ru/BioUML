package ru.biosoft.access;

import java.lang.ref.SoftReference;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringJoiner;

import javax.annotation.Nonnull;

import ru.biosoft.exception.LoggedException;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementReadException;
import ru.biosoft.access.core.SortableDataCollection;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.sql.SqlConnectionHolder;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.access.sql.SqlList;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.util.ListUtil;


/**
 * DataCollection which store all information in SQL DBMS.
 *
 * @see ru.biosoft.access.core.DataCollection
 * @see ru.biosoft.access.SqlTransformer
 */
public class SqlDataCollection<T extends DataElement> extends AbstractDataCollection<T> implements SqlConnectionHolder, SortableDataCollection<T>
{
    ///////////////////////////////////////////////////////////////////////////
    // JDBC properties
    //

    /** Default JDBC driver */
    public static final String JDBC_DEFAULT_DRIVER = "com.mysql.jdbc.Driver";

    /** Property for storing class of jdbc driver. */
    public static final String JDBC_DRIVER_PROPERTY = "jdbcDriverClass";

    /** Property for storing driver specific URL for connecting to the DBMS. */
    public static final String JDBC_URL_PROPERTY = "jdbcURL";

    /** Property for storing user name for connecting to the DBMS. */
    public static final String JDBC_USER_PROPERTY = "jdbcUser";

    /** Property for storing user password for connecting to the DBMS. */
    public static final String JDBC_PASSWORD_PROPERTY = "jdbcPassword";

    /** Property for storing class of that should be used for converting DataElements to/from DBMS. */
    public static final String SQL_TRANSFORMER_CLASS = "transformerClass";

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Standart constructor for creating data collection.
     * Used by CollectionFactory.
     *
     * <ul>Required properties:
     * <li>{@link #JDBC_DRIVER_PROPERTY}</li>
     * <li>{@link #JDBC_URL_PROPERTY}</li>
     * <li>{@link #JDBC_USER_PROPERTY}</li>
     * <li>{@link #JDBC_PASSWORD_PROPERTY}</li>
     * <li>{@link #SQL_TRANSFORMER_CLASS}</li>
     * </ul>
     *
     * @param parent Parent collection.
     * @param properties Properties for creating collection (cannot be null).
     * @see #JDBC_DRIVER_PROPERTY
     * @see #JDBC_URL_PROPERTY
     * @see #JDBC_USER_PROPERTY
     * @see #JDBC_PASSWORD_PROPERTY
     * @see #SQL_TRANSFORMER_CLASS
     */
    public SqlDataCollection(DataCollection<?> parent, Properties properties) throws LoggedException
    {
        super(parent, properties);
        init();
    }

    protected void init() throws LoggedException
    {
        try
        {
            // Create transformer
            Class<? extends SqlTransformer<T>> transformerClass = (Class<? extends SqlTransformer<T>>)getInfo().getPropertyClass(
                    SQL_TRANSFORMER_CLASS, SqlTransformer.class );
            try
            {
                transformer = transformerClass.newInstance();
            }
            catch( Exception e )
            {
                throw new DataElementReadException(e, this, SQL_TRANSFORMER_CLASS);
            }
            transformer.init(this);
            checkUsedTables();
        }
        catch( Exception e )
        {
            valid = false;
            throw ExceptionRegistry.translateException(e);
        }
    }

    protected void checkUsedTables() throws BiosoftSQLException
    {
        String[] tables = transformer.getUsedTables();
        if( tables != null )
        {
            Connection connection = getConnection();
            for( String table : tables )
            {
                if( !SqlUtil.hasTable(connection, table) )
                {
                    String createQuery = transformer.getCreateTableQuery(table);
                    if( createQuery != null )
                    {
                        SqlUtil.execute(connection, createQuery);
                    }
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    //  Public methods
    //

    /** Connection to DBMS. */
    private final ThreadLocal<Connection> conn = new ThreadLocal<>();
    /**
     *  Return active connection.
     *  Connect if necessary.
     *  @return Active connection.
     *  @throws java.sql.SQLException If operation on DBMS failed.
     */
    @Override
    public synchronized Connection getConnection() throws BiosoftSQLException
    {
        if( conn.get() == null )
        {
            conn.set(SqlConnectionPool.getConnection(this));
        }
        return conn.get();
    }

    /** Transformer for converting DataElements to/from DBMS. */
    private SqlTransformer<T> transformer = null;
    public SqlTransformer<T> getTransformer()
    {
        return transformer;
    }

    /**
     * Return type of ru.biosoft.access.core.DataElement stored in this data collection.
     * Really ask transformer for extract template type.
     *
     * @see ru.biosoft.access.SqlTransformer
     * @return Type of ru.biosoft.access.core.DataElement stored in this data collection.
     */
    @Override
    public @Nonnull Class<T> getDataElementType()
    {
        return transformer.getTemplateClass();
    }


    /** Flag 'is mutable' */
    private Boolean sqlMutable = null;

    /**
     * Returns <code>true</code> if this data collection is mutable.
     * Check Connection is read only or not.
     * @return whether this collection is mutable.
     * @see java.sql.Connection
     */
    @Override
    public boolean isMutable()
    {
        if( !isValid() )
            return false;
        if( sqlMutable == null )
        {
            try
            {
                sqlMutable = !getConnection().isReadOnly();
            }
            catch( SQLException exc )
            {
                log.log(Level.SEVERE, "Cann't ask conn.isReadOnly()", exc);
                return false;
            }
        }

        return ( sqlMutable.booleanValue() && mutable );
    }

    protected BiosoftSQLException sqlError(SQLException e, String query)
    {
        return new BiosoftSQLException(this, query, e);
    }

    int count = -1;
    /**
     *  Returns size of data collection.
     *  This implementation execute SQL query for extract record count.
     *
     *  @return size of data collection.
     */
    @Override
    public int getSize()
    {
        if( !isValid() )
            return 0;
        if( this.count > -1 )
            return this.count;
        List<String> nameList = this.nameListRef == null ? null : this.nameListRef.get();
        if( nameList != null )
        {
            this.count = nameList.size();
            return this.count;
        }
        try
        {
            String countQuery = transformer.getCountQuery();
            this.count = SqlUtil.queryInt(getConnection(), countQuery);
        }
        catch( Exception e )
        {
            throw new DataElementReadException(e, this);
        }
        return count;
    }

    private SoftReference<List<String>> nameListRef = null;

    @Override
    public @Nonnull List<String> getNameList()
    {
        if( !isValid() )
            return ListUtil.emptyList();
        if( nameListRef != null )
        {
            List<String> cachedList = nameListRef.get();
            if( cachedList != null )
            {
                return cachedList;
            }
        }
        List<String> list;
        try
        {
            String nameListQuery = transformer.getNameListQuery();
            if( getInfo().getQuerySystem() == null || getInfo().getQuerySystem().getIndex("title") == null )
            {
                list = new SqlList( this, nameListQuery, getSize(), transformer.isNameListSorted() );
                nameListRef = new SoftReference<>(list);
                return list;
            }
            list = SqlUtil.queryStrings(getConnection(), nameListQuery);
            sortNameList(list);
            nameListRef = new SoftReference<>(list);
        }
        catch( BiosoftSQLException e )
        {
            throw new DataElementReadException(e, this);
        }
        return list;
    }

    /**
     * Returns true if data collection contain element with the specified name.
     */
    @Override
    public boolean contains(String name)
    {
        if( !isValid() )
            return false;
        String elementQuery = transformer.getElementExistsQuery(name);
        if( elementQuery == null )
            return false;
        if( v_cache.containsKey(name) )
            return true;
        return SqlUtil.hasResult(getConnection(), elementQuery);
    }

    /**
     *  Close connection to DBMS.
     *  Invalidates SQLDataCollection instance.
     *  @throws Exception
     */
    @Override
    public void close() throws Exception
    {
        super.close();

        try
        {
            if( conn.get() != null )
                conn.get().commit();
        }
        catch( Throwable t )
        {
        }

        conn.set(null);
        transformer = null;
    }

    ////////////////////////////////////////////////////////////////////////////
    //  Protected methods
    //

    /**
     * Extracts and returns ru.biosoft.access.core.DataElement with specified name from the data collection.
     *
     * @param name Name of the data element (PK).
     * @return ru.biosoft.access.core.DataElement with specified name or null, if data element not found.
     * @see ru.biosoft.access.SqlTransformer#getElementQuery(String)
     *
     * @throws java.sql.SQLException if cannot execute SELECT statement.
     * @throws java.lang.Exception if transformer failed to create ru.biosoft.access.core.DataElement instance.
     */
    @Override
    protected T doGet(String name) throws Exception
    {
        String elementQuery = transformer.getElementQuery(name);
        if( elementQuery == null )
            return null;

        T de = null;
        try( Statement statement = getConnection().createStatement(); ResultSet resultSet = statement.executeQuery( elementQuery ) )
        {
            de = resultSet.next() ? transformer.create(resultSet, getConnection()) : null;
        }
        catch( SQLException e )
        {
            throw sqlError(e, elementQuery);
        }
        return de;
    }

    /**
     * Remove ru.biosoft.access.core.DataElement from data collection.
     * @param de ru.biosoft.access.core.DataElement which should be removed (cannot be null).
     * @throws SQLException If cannot execute DELETE statement generated by transformer.<br>
     *                      If cannot create Statement.
     * @see ru.biosoft.access.SqlTransformer#getDeleteQuery(String)
     */
    @Override
    protected void doRemove(String name) throws Exception
    {
        try( Statement statement = getConnection().createStatement() )
        {
            transformer.addDeleteCommands( statement, name );

            executeStatementTransaction( statement );
        }
        nameListRef = null;
        count = -1;
    }


    /**
     * Adds the specified data element to the collection.
     *
     * If element is new then Insert query will be executed
     * otherwise Update query from SqlTransformer will be used.
     *
     * While put action can include many SQL statements it is executed as one transaction.
     * If some exception will occur then method rollback transaction.
     *
     * @throws SQLException If cannot execute Insert or Update statement generated by transformer.<br>
     *                      If cannot create Statement.
     * @see ru.biosoft.access.SqlTransformer#getInsertQuery(DataElement)
     * @see ru.biosoft.access.SqlTransformer#getUpdateQuery(DataElement)
     */
    @Override
    protected void doPut(T de, boolean isNew) throws Exception
    {
        try( Statement statement = getConnection().createStatement() )
        {
            if( isNew )
                transformer.addInsertCommands( statement, de );
            else
                transformer.addUpdateCommands( statement, de );

            executeStatementTransaction( statement );
        }
        nameListRef = null;
        count = -1;
    }

    /**
     * Safely executes statement using transaction.
     */
    protected void executeStatementTransaction(Statement statement) throws BiosoftSQLException
    {
        Connection connection = null;
        boolean autoCommit = false;

        try
        {
            connection = statement.getConnection();

            autoCommit = connection.getAutoCommit();
            if( autoCommit == true )
                connection.setAutoCommit(false);

            try
            {
                statement.executeBatch();
                connection.commit();
            }
            catch( SQLException e )
            {
                connection.rollback();
                throw e;
            }
        }
        catch(SQLException e)
        {
            throw sqlError(e, null);
        }
        finally
        {
            SqlUtil.close(statement, null);
            try
            {
                if( connection != null && connection.getAutoCommit() != autoCommit )
                    connection.setAutoCommit(autoCommit);
            }
            catch( SQLException e )
            {
                throw sqlError(e, "SET autocommit = "+(autoCommit?1:0));
            }
        }
    }

    @Override
    public boolean isSortingSupported()
    {
        return getTransformer().isSortingSupported();
    }

    @Override
    public String[] getSortableFields()
    {
        return getTransformer().getSortableFields();
    }

    @Override
    public List<String> getSortedNameList(String field, boolean direction)
    {
        try
        {
            return SqlUtil.queryStrings(getConnection(), getTransformer().getSortedNameListQuery(field, direction));
        }
        catch( BiosoftSQLException e )
        {
            return getNameList();
        }
    }

    @Override
    public Iterator<T> getSortedIterator(String field, boolean direction, int from, int to)
    {
        List<String> sortedNameList = getSortedNameList(field, direction);
        List<String> subList = sortedNameList.subList( from, to );
        if( subList.isEmpty() )
            return java.util.Collections.<T>emptyIterator();

        // Batch-fetch all rows in one query instead of N+1 individual lookups.
        // The old path (createDataCollectionIterator) called doGet(name) per row,
        // issuing one SELECT per element. Profiling on strange.genexplain.com
        // showed this was 43% of WebTablesProvider CPU time.
        try
        {
            String selectQuery = transformer.getSelectQuery();
            StringJoiner inClause = new StringJoiner( "," );
            for( String name : subList )
                inClause.add( SqlUtil.quoteString( name ) );

            String idFilter = transformer.getIdField() + " IN (" + inClause + ")";
            String batchQuery = buildBatchQuery( selectQuery, idFilter );

            Map<String, T> byName = new HashMap<>();
            try( Statement statement = getConnection().createStatement(); ResultSet rs = statement.executeQuery( batchQuery ) )
            {
                while( rs.next() )
                {
                    T element = transformer.create( rs, getConnection() );
                    byName.put( element.getName(), element );
                }
            }

            if( byName.size() < subList.size() )
            {
                log.log( Level.FINE,
                        "getSortedIterator: batch query returned {0}/{1} rows — some will fall back to per-row doGet",
                        new Object[]{ byName.size(), subList.size() } );
            }

            final Map<String, T> result = byName;
            Iterator<String> nameIter = subList.iterator();
            return new Iterator<T>()
            {
                @Override
                public boolean hasNext()
                {
                    return nameIter.hasNext();
                }
                @Override
                public T next()
                {
                    String name = nameIter.next();
                    T element = result.get( name );
                    if( element == null )
                    {
                        // Fallback for rows the batch query didn't return
                        // (e.g. filtered out by a transformer-specific condition)
                        try
                        {
                            element = doGet( name );
                        }
                        catch( Exception e )
                        {
                            throw new RuntimeException( e );
                        }
                    }
                    return element;
                }
            };
        }
        catch( Exception e )
        {
            // Fall back to the per-row iterator if the batch query fails
            log.log( Level.SEVERE, "Batch query in getSortedIterator failed, falling back to per-row", e );
            return AbstractDataCollection.createDataCollectionIterator( this, subList.iterator() );
        }
    }

    /**
     * Case-insensitive whole-word search: returns the index of the first
     * occurrence of {@code word} as a standalone token in {@code sql},
     * or -1 if not found.  A match requires:
     * <ul>
     *   <li>whole-word boundaries on both sides (the preceding and
     *       following characters, if any, must not be letters or digits),</li>
     *   <li>the match must not be inside a single-quoted string literal
     *       or a backtick-quoted identifier,</li>
     *   <li>the match must be followed by a non-identifier character or
     *       end-of-string (so "WHEREX" doesn't match "WHERE").</li>
     * </ul>
     * Mutual guards (!inBacktick / !inSingleQuote) prevent a backtick
     * inside a single-quoted literal (e.g. {@code 'can`stop'}) from
     * toggling backtick-state, and vice versa.  Intentional — do not
     * simplify.
     *
     * Known limitation: no parenthesis-depth tracking, so a subquery
     * containing the target word earlier in the string than the outer
     * occurrence will match first (leftmost wins).  Callers that depend
     * on finding the <em>outermost</em> occurrence should not use this
     * method on queries with nested subqueries.  None of the current
     * SqlTransformer.getSelectQuery() implementations have such subqueries.
     */
    private static int indexOfWholeWord( String sql, String word )
    {
        String upper = sql.toUpperCase();
        String wordUpper = word.toUpperCase();
        boolean inSingleQuote = false;
        boolean inBacktick = false;
        for( int i = 0; i < upper.length(); i++ )
        {
            char c = sql.charAt( i );
            if( c == '\'' && !inBacktick )
                inSingleQuote = !inSingleQuote;
            else if( c == '`' && !inSingleQuote )
                inBacktick = !inBacktick;
            if( inSingleQuote || inBacktick )
                continue;
            if( upper.startsWith( wordUpper, i ) )
            {
                if( i > 0 && Character.isLetterOrDigit( sql.charAt( i - 1 ) ) )
                    continue;
                int end = i + wordUpper.length();
                if( end >= upper.length() || !Character.isLetterOrDigit( upper.charAt( end ) ) )
                    return i;
            }
        }
        return -1;
    }

    /**
     * Case-insensitive whole-word check: returns true if the given word
     * appears as a standalone token in the string (not as a substring of an
     * identifier, and not inside single-quoted or backtick-quoted literals).
     */
    private static boolean hasWholeWord( String sql, String word )
    {
        return indexOfWholeWord( sql, word ) >= 0;
    }

    /**
     * Find the index of a trailing SQL clause (ORDER BY, GROUP BY, LIMIT) in
     * a query string, ignoring occurrences inside string literals and
     * backtick-quoted identifiers.  Delegates to {@link #indexOfWholeWord}
     * for the tokenization scan; the only difference is that the right
     * boundary must be whitespace (not just a non-identifier character)
     * to handle multi-clause keywords like "ORDER BY" where the space
     * between the two words is part of the token.
     * Returns -1 if the clause is not found.
     */
    private static int findTrailingClauseIndex( String sql, String clause )
    {
        // indexOfWholeWord treats the right boundary as "not a letter/digit",
        // which is too permissive for multi-word clauses: "ORDER BYX" would
        // match "ORDER BY" (space after BY is a non-identifier char).  For
        // trailing-clause detection we need the right boundary to be actual
        // whitespace.  Re-scan with the stricter check.
        String upper = sql.toUpperCase();
        String clauseUpper = clause.toUpperCase();
        boolean inSingleQuote = false;
        boolean inBacktick = false;
        for( int i = 0; i < upper.length(); i++ )
        {
            char c = sql.charAt( i );
            if( c == '\'' && !inBacktick )
                inSingleQuote = !inSingleQuote;
            else if( c == '`' && !inSingleQuote )
                inBacktick = !inBacktick;
            if( inSingleQuote || inBacktick )
                continue;
            if( upper.startsWith( clauseUpper, i ) )
            {
                if( i > 0 && Character.isLetterOrDigit( sql.charAt( i - 1 ) ) )
                    continue;
                int end = i + clauseUpper.length();
                if( end >= upper.length() || Character.isWhitespace( upper.charAt( end ) ) )
                    return i;
            }
        }
        return -1;
    }

    /**
     * Build a batch-fetch query by inserting an id filter into a SELECT
     * query, before any trailing ORDER BY / GROUP BY / LIMIT clause.
     *
     * If the query already has a WHERE clause, the pre-existing condition
     * is wrapped in parentheses before the AND is appended.  This is
     * necessary because AND binds tighter than OR in SQL: without
     * parenthesization, "... WHERE a=1 OR b=2 AND id IN (...)" would parse
     * as "a=1 OR (b=2 AND id IN (...))", silently returning rows matching
     * a=1 that are NOT in the requested batch.
     *
     * Known limitation: no parenthesis-depth tracking, so a subquery
     * containing its own ORDER BY / LIMIT / WHERE earlier in the string
     * than the outer occurrence will be matched first, causing the
     * insertion point or parenthesization to target the subquery rather
     * than the outer clause.  None of the current SqlTransformer
     * implementations have such subqueries in getSelectQuery().
     */
    private static String buildBatchQuery( String selectQuery, String idFilter )
    {
        int orderIdx = findTrailingClauseIndex( selectQuery, "ORDER BY" );
        int groupIdx = findTrailingClauseIndex( selectQuery, "GROUP BY" );
        int limitIdx = findTrailingClauseIndex( selectQuery, "LIMIT" );
        int insertIdx = Math.min(
                Math.min( orderIdx == -1 ? selectQuery.length() : orderIdx,
                          groupIdx == -1 ? selectQuery.length() : groupIdx ),
                limitIdx == -1 ? selectQuery.length() : limitIdx );

        String before = selectQuery.substring( 0, insertIdx ).trim();
        String after  = selectQuery.substring( insertIdx );
        String afterPart = after.isEmpty() ? "" : " " + after.trim();

        int wherePos = indexOfWholeWord( before, "WHERE" );
        if( wherePos == -1 )
            return before + " WHERE " + idFilter + afterPart;

        // Wrap the existing condition in parentheses so the appended AND
        // doesn't change the semantics of any top-level OR.  The original
        // case of the WHERE keyword is preserved.
        String preWhere  = before.substring( 0, wherePos );
        String whereKw   = before.substring( wherePos, wherePos + "WHERE".length() );
        String condition = before.substring( wherePos + "WHERE".length() ).trim();
        return preWhere + whereKw + " (" + condition + ") AND " + idFilter + afterPart;
    }

    @Override
    public void reinitialize() throws LoggedException
    {
        if(isValid())
            return;
        valid = true;
        init();
        if(isValid())
        {
            DataCollection<?> origin = getOrigin();
            if( origin != null && origin.isPropagationEnabled()
                    && !CollectionFactory.isDataElementCreating( getCompletePath().toString() ) )
            {
                origin.propagateElementChanged(this, null);
            }
        }
    }
}
