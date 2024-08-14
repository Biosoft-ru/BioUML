package ru.biosoft.access;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import ru.biosoft.access.core.DataElement;


/**
 *  Adapter for storing/extracting data elements to/from SQL DBMS.
 *
 *  @see ru.biosoft.access.SqlDataCollection
 */
public interface SqlTransformer<T extends DataElement>
{
    /**
     * Initialize transformer.
     * Must be called by client after SqlTransformer's constructor, and before using instance of SqlTransformer.
     * @return <code>true</code> - if succeeded, <code>false</code> - otherwise.
     */
    boolean init( SqlDataCollection<T> owner );

    /**
     * Returns class of object.
     * @return Class of transformed object.
     * @see #create(java.sql.ResultSet)
     */
    Class<T> getTemplateClass();

    /**
     * Extracts all needed fields from resultSet, and create instance of ru.biosoft.access.core.DataElement.
     * If necessary the method can use additional queries to get needed data
     * using the specified connection.
     *
     * @param resultSet ResultSet from DBMS.
     * @param connection to be used if additional query is needed.
     *
     * @return created data element.
     *
     * @see #getTemplateClass()
     */
    T create(ResultSet resultSet, Connection connection) throws Exception;

    /**
     * Creates SQL query for extracting all data elements from table.
     * @return SQL query for extracting all data elements from table.
     */
    String getSelectQuery();

    /**
     * Creates SQL query for extracting count of data elements in the table.
     * @return SQL query for extracting count of data elements in the table.
     */
    String getCountQuery();

    /**
     * Creates SQL query for extracting names for all data elements.
     * Data element names should be sorted alphabetically.
     *
     * @return SQL query for extracting count of data elements in the table.
     */
    String getNameListQuery();

    boolean isNameListSorted();

    /**
     * Creates SQL query for extracting data element with specified name from table.
     * @param name Name (PK) of needed data element.
     * @return SQL query for extracting data element with specified name from table.
     */
    String getElementQuery( String name );

    /**
     * Creates SQL query for test is data element with specified name exists in the table.
     * @param name Name (PK) of needed data element.
     * @return SQL query for testing data element with specified name in the table.
     */
    String getElementExistsQuery( String name );
    
    /**
     * Get names for all used tables sorted by priority.
     * 
     * @return Array of names sorted by priority.
     */
    String[] getUsedTables( );
    
    /**
     * Creates SQL query for table creation.
     * @param name of table.
     * @return SQL query for table creation.
     */
    String getCreateTableQuery(String tableName);

    /**
     * Adds set of SQL commands to the statement to insert data element into the table.
     *
     * @param statement - statement to which SQL commands should be added.
     * @param de - object for which INSERT statements will be generated.
     */
    void addInsertCommands(Statement statement, T de) throws Exception;

    /**
     * Adds set of SQL commands to the statement to update data element in the table.
     *
     * @param statement - statement to which SQL commands should be added.
     * @param de - object for which UPDATE statements will be generated.
     */
    void addUpdateCommands(Statement statement, T de) throws Exception;

    /**
     * Adds set of SQL commands to the statement to remove data element from the table.
     *
     * @param statement - statement to which SQL commands should be added.
     * @param de - object for which DELETE statements will be generated.
     */
    void addDeleteCommands(Statement statement, String name) throws Exception;

    /**
     * @return false if sorting is not supported
     */
    public boolean isSortingSupported();
    
    /**
     * @return list of field names for which sorting is supported
     */
    public String[] getSortableFields();
    
    /**
     * @param field one of fields previously returned by getSortableFields
     * @param direction sorting direction (true = ascending)
     * @return Query to get sorted name list
     */
    public String getSortedNameListQuery(String field, boolean direction);
}