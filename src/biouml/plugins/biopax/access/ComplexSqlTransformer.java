
package biouml.plugins.biopax.access;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.sql.SqlUtil;
import biouml.standard.type.Complex;
import biouml.standard.type.access.ConceptSqlTransformer;

public class ComplexSqlTransformer extends ConceptSqlTransformer<Complex>
{
    @Override
    public boolean init(SqlDataCollection<Complex> owner)
    {
        table = "complexes";
        this.owner = owner;
        checkAttributesColumn(owner);
        return true;
    }
    
    @Override
    public String[] getUsedTables()
    {
        return new String[] {"dbReferences", "publicationReferences", "publications", "synonyms", "complex2component", table};
    }
    
    @Override
    public Class<Complex> getTemplateClass()
    {
        return Complex.class;
    }

    @Override
    public String getSelectQuery()
    {
        return "SELECT ID, type, title, description, comment, completeName  " + "FROM " + table;
    }

    @Override
    protected Complex createElement(ResultSet resultSet, Connection connection) throws SQLException
    {
        Complex complex = new Complex(owner, resultSet.getString(1));
        complex.setCompleteName(resultSet.getString(6));
        String[] components = SqlUtil.stringStream(connection, "SELECT componentPath FROM complex2component " + "WHERE complexID="
                + validateValue(resultSet.getString(1))).toArray( String[]::new );
        complex.setComponents( components );
        return complex;
    }

    @Override
    protected String getSpecificFields(Complex complex)
    {
        return ", completeName";
    }

    @Override
    protected String[] getSpecificValues(Complex complex)
    {
        return new String[] { complex.getCompleteName()};
    }
    
    @Override
    public void addInsertCommands(Statement statement, Complex complex) throws Exception
    {
        super.addInsertCommands(statement, complex);
        addComponentsCommand(statement, complex);
    }
    
    protected void addComponentsCommand(Statement statement, Complex complex) throws Exception
    {
        String[] components = complex.getComponents();
        if( components != null)
        {
            for( String component : components )
            {
                statement.addBatch(
                    "INSERT INTO complex2component (complexID, componentPath)" + "VALUES(" +
                    validateValue(complex.getName()) + ", " + validateValue(component.trim()) + ")");
            }
        }
    }
    
    @Override
    public void addDeleteCommands(Statement statement, String name) throws Exception
    {
        super.addDeleteCommands(statement, name);
        addRemoveComponents(statement, name);
    }
    
    private void addRemoveComponents(Statement statement, String name) throws SQLException
    {
        statement.addBatch("DELETE FROM complex2component WHERE complexID=" + validateValue(name));
    }

    @Override
    public String getCreateTableQuery(String tableName)
    {
        if( tableName.equals(table) )
        {
            return "CREATE TABLE `complexes` (" +
                    getIDFieldFormat() + "," +
                    "  `type` enum('unknown','semantic-concept','semantic-concept-function','semantic-concept-process','semantic-concept-state','molecule','molecule-gene','molecule-RNA','molecule-protein','molecule-substance','compartment','compartment-cell','reaction','relation','relation-semantic','relation-chemical','info-database','info-diagram','info-relation-type','info-species','info-unit','constant') NOT NULL default 'compartment-cell'," +
                    getTitleFieldFormat()+ "," +
                    "  `completeName` varchar(200) default NULL," +
                    "  `description` text," +
                    "  `comment` text," +
                    "  `attributes` text,"+
                    "  UNIQUE KEY `IDX_UNIQUE_complexes_ID` (`ID`)" +
                    ") ENGINE=MyISAM";
        }
        else if(tableName.equals("complex2component"))
        {
            return "CREATE TABLE `complex2component` (" +
                        getIDFieldFormat("complexID") + "," +
                        "  `componentPath` varchar(200) NOT NULL default ''," +
                        "  KEY `IDX_COMPLEX_2_COMPONENT_ID` (`complexID`)" +
                        ") ENGINE=MyISAM";
        }
        return super.getCreateTableQuery(tableName);
    }
}
