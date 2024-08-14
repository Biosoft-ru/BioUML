package biouml.plugins.biopax.access;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.SqlTransformerSupport;
import biouml.standard.type.SpecieReference;

/**
 * BioPAX SpecieReferences are stored in ../Data/Participants collection and are not only a reaction participants,
 * so separate transformer is required to store SpecieReferences in sql
 *
 */
public class SpecieReferenceSqlTransformer extends SqlTransformerSupport<SpecieReference>
{
    @Override
    public boolean init(SqlDataCollection<SpecieReference> owner)
    {
        super.init(owner);
        table = "specieReferences";
        checkAttributesColumn(owner);
        return true;
    }
    
    @Override
    public String[] getUsedTables()
    {
        return new String[] {table};
    }
    
    @Override
    public void addInsertCommands(Statement statement, SpecieReference referrer) throws Exception
    {
        StringBuffer result = new StringBuffer("INSERT INTO " + table + " (ID, role, title, comment, specie, stoichiometry, modifierAction) VALUES(");

        result.append(validateValue(referrer.getName()));
        result.append(", " + validateValue(referrer.getRole()));
        result.append(", " + validateValue(referrer.getTitle()));
        result.append(", " + validateValue(referrer.getComment()));
        if(referrer.getSpecie() == null)
            result.append(", ''");
        else
            result.append(", " + validateValue(referrer.getSpecie()));
        result.append(", " + validateValue(referrer.getStoichiometry()));
        result.append(", " + validateValue(referrer.getModifierAction()));
        result.append(")");
        statement.addBatch(result.toString());
        addInsertAttributesCommand(statement, referrer.getName(), getAttributesString(referrer.getAttributes()));
    }

    @Override
    public SpecieReference create(ResultSet resultSet, Connection connection) throws Exception
    {
        SpecieReference sr = new SpecieReference(owner, resultSet.getString(1));
        sr.setRole(resultSet.getString(2));
        sr.setTitle(resultSet.getString(3));
        sr.setComment(resultSet.getString(4));
        sr.setSpecie(resultSet.getString(5));
        sr.setStoichiometry(resultSet.getString(6));
        if(sr.getRole().equals(SpecieReference.MODIFIER))
            sr.setModifierAction(resultSet.getString(7));
        getAttributes(connection, sr.getName(), sr.getAttributes());
        return sr;
    }

    @Override
    public String getSelectQuery()
    {
        return "SELECT ID, role, title, comment, specie, stoichiometry, modifierAction " + "FROM " + table;
    }

    @Override
    public Class<SpecieReference> getTemplateClass()
    {
        return SpecieReference.class;
    }
    
    @Override
    public String getCreateTableQuery(String tableName)
    {
        return "CREATE TABLE `specieReferences` (" +
                    "  `ID` VARCHAR(100) NOT NULL," +
                    "  `role` enum('reactant','product','modifier','other') NOT NULL default 'other'," +
                    "  `title` TEXT NOT NULL," +
                    "  `specie` varchar(100) NOT NULL default ''," +
                    "  `stoichiometry` varchar(20) NOT NULL default '1'," +
                    "  `modifierAction` enum('catalyst','inhibitor','switch on','switch off') default NULL," +
                    "  `comment` text," +
                    "  `attributes` text,"+
                    "  UNIQUE KEY `IDX_UNIQUE_specieReferences_ID` (`ID`)" +
                    ") ENGINE=MyISAM";
    }
}
