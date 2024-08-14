
package biouml.standard.type.access;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.StringTokenizer;

import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.sql.SqlUtil;
import biouml.standard.type.Concept;

public class ConceptSqlTransformer<T extends Concept> extends ReferrerSqlTransformer<T>
{
    @Override
    public boolean init(SqlDataCollection<T> owner)
    {
        super.init(owner);
        table = "concepts";
        checkAttributesColumn(owner);
        return true;
    }

    @Override
    public Class getTemplateClass()
    {
        return Concept.class;
    }

    @Override
    public String getSelectQuery()
    {
        return "SELECT id, type, title, description, comment, completeName  " + "FROM " + table;
    }

    @Override
    protected T createElement(ResultSet resultSet, Connection connection) throws SQLException
    {
        T concept = (T)new Concept(owner, resultSet.getString(1));
        concept.setType(resultSet.getString(2));
        concept.setCompleteName(resultSet.getString(6));

        return concept;
    }

    @Override
    public T create(ResultSet resultSet, Connection connection) throws Exception
    {
        T concept = super.create(resultSet, connection);

        // retrieve synonyms
        String synonyms = SqlUtil.stringStream( connection,
                "SELECT synonym FROM synonyms s WHERE s.entityID=" + validateValue( concept.getName() ) + " ORDER BY synonym" )
                .joining( "; " );

        concept.setSynonyms(synonyms);

        return concept;
    }

    @Override
    protected String getSpecificFields(T de)
    {
        return ", completeName";
    }

    @Override
    protected String[] getSpecificValues(T de)
    {
        return new String[] { de.getCompleteName() };
    }

    @Override
    public void addInsertCommands(Statement statement, T concept) throws Exception
    {
        super.addInsertCommands(statement, concept);

        // add synonyms
        if(concept.getSynonyms() != null)
        {
            StringTokenizer tokens = new StringTokenizer(concept.getSynonyms(), ";");
            while(tokens.hasMoreTokens())
            {
                statement.addBatch(
                    "INSERT INTO synonyms (entityId, synonym)" + "VALUES('" +
                    concept.getName() + "', '" + tokens.nextToken().trim().replaceAll("'", "`") + "')");
            }
        }
    }

    /**
     * Adds set of SQL commands to the statement to remove data element from the table.
     * @param statement - statement to which SQL commands should be added.
     * @param de - object for which DELETE statements will be generated.
     */
    @Override
    public void addDeleteCommands(Statement statement, String name) throws Exception
    {
        super.addDeleteCommands(statement, name);
        statement.addBatch("DELETE FROM synonyms WHERE entityId='" + name + "'");
    }
    
    @Override
    public String[] getUsedTables()
    {
        return new String[] {"dbReferences", "publicationReferences", "publications", "synonyms", table};
    }
    
    @Override
    public String getCreateTableQuery(String tableName)
    {
        if( tableName.equals(table) )
        {
            return "CREATE TABLE `concepts` (" +
                    getIDFieldFormat() + "," +
                    "  `type` enum('unknown','semantic-concept','semantic-concept-function','semantic-concept-process','semantic-concept-state','molecule','molecule-gene','molecule-RNA','molecule-protein','molecule-substance','compartment','compartment-cell','reaction','relation','relation-semantic','relation-chemical','info-database','info-diagram','info-relation-type','info-species','info-unit','constant') NOT NULL default 'semantic-concept'," +
                    getTitleFieldFormat()+ "," +
                    "  `completeName` varchar(200) default NULL," +
                    "  `description` text," +
                    "  `comment` text," +
                    "  `attributes` text,"+
                    "  UNIQUE KEY `IDX_UNIQUE_concepts_ID` (`ID`)" +
                    ") ENGINE=MyISAM";
        }
        if( tableName.equals("synonyms") )
        {
            return "CREATE TABLE `synonyms` (" +
                    "  `ID` bigint(20) unsigned NOT NULL auto_increment," +
                    getIDFieldFormat("entityID") + "," +
                    "  `synonym` text," +
                    "  `comment` varchar(250) default NULL," +
                    "  PRIMARY KEY  (`ID`)," +
                    "  KEY `IDX_SYNONYMS_entityID` (`entityID`)" +
                    ") ENGINE=MyISAM";
        }
        return super.getCreateTableQuery(tableName);
    }
}
