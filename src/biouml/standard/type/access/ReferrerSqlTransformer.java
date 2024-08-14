package biouml.standard.type.access;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.SqlTransformerSupport;
import biouml.model.Module;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.Publication;
import biouml.standard.type.Referrer;

abstract class ReferrerSqlTransformer<T extends Referrer> extends SqlTransformerSupport<T>
{
    /** Creates the data element and fills it specific fields. */
    protected abstract T createElement(ResultSet resultSet, Connection connection) throws SQLException;

    @Override
    public T create(ResultSet resultSet, Connection connection) throws Exception
    {
        T referrer = createElement(resultSet, connection);

        // common fields
        referrer.setTitle(resultSet.getString(3));
        referrer.setDescription(resultSet.getString(4));
        referrer.setComment(resultSet.getString(5));
        getAttributes(connection, referrer.getName(), referrer.getAttributes());

        // retrieve database references
        try (Statement statement = connection.createStatement();
                ResultSet dbRefSet = statement
                        .executeQuery( "SELECT ref.dbInfoId, ref.dbId, ref.dbAc, ref.comment, ref.relation FROM dbReferences ref "
                                + "WHERE ref.entityID=" + validateValue( referrer.getName() ) + " ORDER BY ref.dbInfoId" ))
        {
            List<DatabaseReference> dbRefs = new ArrayList<>();
            while( dbRefSet.next() )
            {
                DatabaseReference ref = new DatabaseReference( dbRefSet.getString( 1 ), dbRefSet.getString( 2 ), dbRefSet.getString( 3 ) );
                ref.setParent( referrer );
                ref.setComment( dbRefSet.getString( 4 ) );
                ref.setRelationshipType( dbRefSet.getString( 5 ) );

                dbRefs.add( ref );
            }
            if( dbRefs.size() > 0 )
                referrer.setDatabaseReferences( dbRefs.toArray( new DatabaseReference[dbRefs.size()] ) );
        }

        // retrieve literature references
        try (Statement statement = connection.createStatement();
                ResultSet literSet = statement.executeQuery( "SELECT publicationId FROM publicationReferences ref "
                        + "WHERE ref.entityID = " + validateValue( referrer.getName() ) + " ORDER BY publicationId" ))
        {
            DataCollection<Publication> literature = null;
            try
            {
                literature = Module.getModulePath( owner ).getChildPath( "Data", "literature" ).getDataCollection( Publication.class );
            }
            catch( Exception e1 )
            {
            }
            List<String> liter = new ArrayList<>();
            while( literSet.next() )
            {
                Publication de = null;
                try
                {
                    if( literature != null )
                        de = literature.get( literSet.getString( 1 ) );
                }
                catch( Exception e )
                {
                }
                if( de != null )
                    liter.add( de.getName() );
                else
                    liter.add( literSet.getString( 1 ) );
            }
            if( liter.size() > 0 )
                referrer.setLiteratureReferences( liter.toArray( new String[liter.size()] ) );
        }

        return referrer;
    }

    /**
     * Returns names of specific fields for the data element.
     * This names will be used in INSERT clause.
     * @see #getSpecificValues
     */
    abstract protected String getSpecificFields(T de);

    /**
     * Returns values of specific fields for the data element.
     * This values will be used in VALUES clause.
     * @see #getSpecificFields
     */
    abstract protected String[] getSpecificValues(T de);

    /**
     * Adds set of SQL commands to the statement to insert data element into the table.
     * @param statement - statement to which SQL commands should be added.
     * @param de - object for which INSERT statements will be generated.
     */
    @Override
    public void addInsertCommands(Statement statement, T referrer) throws Exception
    {
        StringBuffer result = new StringBuffer("INSERT INTO " + table + " (id, type, title, description, comment" + getSpecificFields(referrer)
                + ") " + "VALUES(");

        result.append(validateValue(referrer.getName()));
        result.append(", " + validateValue(referrer.getType()));
        result.append(", " + validateValue(referrer.getTitle()));
        result.append(", " + validateValue(referrer.getDescription()));
        result.append(", " + validateValue(referrer.getComment()));

        String[] values = getSpecificValues(referrer);
        if( values != null )
        {
            for( String value : values )
                result.append(", " + validateValue(value));
        }

        result.append(")");
        //System.out.println("Insert: " + result);
        statement.addBatch(result.toString());

        addInsertDBRefAndPublicationsCommands(statement, referrer);
        addInsertAttributesCommand(statement, referrer.getName(), getAttributesString(referrer.getAttributes()));
        //    System.out.println("Finish");
    }

    protected void addInsertDBRefAndPublicationsCommands(Statement statement, Referrer referrer) throws Exception
    {
        //      insert into DB refs
        if( referrer.getDatabaseReferences() != null )
        {
            for( int i = 0; i < referrer.getDatabaseReferences().length; i++ )
            {
                DatabaseReference ref = referrer.getDatabaseReferences()[i];

                StringBuilder buf = new StringBuilder("INSERT INTO dbReferences (entityId, dbInfoId, dbId, dbAc, comment, relation) VALUES(");
                buf.append(validateValue(referrer.getName()));
                buf.append(", " + validateValue(ref.getDatabaseName()));
                buf.append(", " + validateValue(ref.getId()));
                buf.append(", " + validateValue(ref.getAc()));
                buf.append(", " + validateValue(ref.getComment()));
                buf.append(", " + validateValue(ref.getRelationshipType()));
                buf.append(")");

                //        System.out.println("Insert: " + result);

                statement.addBatch(buf.toString());
            }
        }

        // insert liter
        if( referrer.getLiteratureReferences() != null )
        {
            for( String ref : referrer.getLiteratureReferences() )
            {
                statement.addBatch("INSERT INTO publicationReferences (entityId, publicationId) VALUES("
                        + validateValue(referrer.getName()) + ", " + validateValue(ref) + ")");
            }
        }
    }

    /**
     * Adds set of SQL commands to the statement to update data element in the table.
     * @param statement - statement to which SQL commands should be added.
     * @param de - object for which UPDATE statements will be generated.
     */
    @Override
    public void addUpdateCommands(Statement statement, T de) throws Exception
    {
        addDeleteCommands(statement, de.getName());
        addInsertCommands(statement, de);
    }

    /**
     * Adds set of SQL commands to the statement to remove data element from the table.
     * @param statement - statement to which SQL commands should be added.
     * @param de - object for which DELETE statements will be generated.
     */
    @Override
    public void addDeleteCommands(Statement statement, String name) throws Exception
    {
        statement.addBatch("DELETE FROM " + table + " WHERE id='" + name + "'");
        statement.addBatch("DELETE FROM dbReferences WHERE entityId='" + name + "'");
        statement.addBatch("DELETE FROM publicationReferences WHERE entityId='" + name + "'");
    }

    @Override
    public String getCreateTableQuery(String tableName)
    {
        if( tableName.equals("dbReferences") )
        {
            return "CREATE TABLE `dbReferences` ("
                    + "  `ID` bigint(20) unsigned NOT NULL auto_increment,"
                    + getIDFieldFormat("entityID") + ","
                    + "  `dbInfoId` char(20) NOT NULL default '',"
                    + "  `dbId` char(50) NOT NULL default '',"
                    + "  `dbAc` char(50) default NULL,"
                    + "  `comment` char(250) default NULL,"
                    + "  `relation` char(30) default NULL,"
                    + "  PRIMARY KEY  (`ID`),"
                    + "  KEY `IDX_DBREFS_entityID` (`entityID`)"
                    + ") ENGINE=MyISAM";
        }
        if( tableName.equals("publicationReferences") )
        {
            return "CREATE TABLE `publicationReferences` ("
                    + "  `ID` bigint(20) unsigned NOT NULL auto_increment,"
                    + getIDFieldFormat("entityID") + ","
                    + getIDFieldFormat("publicationId") + ","
                    + "  `comment` char(250) default NULL,"
                    + "  PRIMARY KEY  (`ID`),"
                    + "  KEY `IDX_PUBREFS_entityID` (`entityID`),"
                    + "  KEY `IDX_PUBREFS_publicationID` (`publicationId`)"
                    + ") ENGINE=MyISAM";
        }
        if( tableName.equals("publications") )
        {
            return "CREATE TABLE `publications` ("
                    + getIDFieldFormat() + ","
                    + "  `ref` text,"
                    + "  `PMID` bigint(20) unsigned default NULL,"
                    + "  `authors` text,"
                    + getTitleFieldFormat()+ ","
                    + "  `source` varchar(100) default NULL,"
                    + " `journalTitle` varchar(100) default NULL,"
                    + "  `year` int(10) unsigned default NULL,"
                    + "  `month` varchar(10) default NULL,"
                    + "  `volume` int(10) unsigned default NULL,"
                    + "  `issue` int(10) unsigned default NULL,"
                    + "  `pageFrom` varchar(10) default NULL,"
                    + "  `pageTo` varchar(10) default NULL,"
                    + "  `language` char(3) default NULL,"
                    + "  `publicationType` varchar(50) default NULL,"
                    + "  `abstract` text,"
                    + "  `url` varchar(512) default NULL,"
                    + "  `importance` int(11) default '3',"
                    + "  `keyWords` text,"
                    + "  `comment` text,"
                    + "  `affiliation` text,"
                    + "  `status` varchar(30) default NULL,"
                    + "  PRIMARY KEY  (`ID`),"
                    + "  UNIQUE KEY `IDX_UNIQUE_PUBLICATIONS_ID` (`ID`)"
                    + ") ENGINE=MyISAM";
        }
        return super.getCreateTableQuery(tableName);
    }
    
    protected String getIDFieldFormat()
    {
        return getIDFieldFormat("ID");
    }
    
    protected String getIDFieldFormat(String idName)
    {
        return "  `" + idName + "` VARCHAR(100) NOT NULL";
    }
    
    protected String getTitleFieldFormat()
    {
        return "  `title` TEXT NOT NULL";
    }
}
