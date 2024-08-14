package biouml.plugins.chebi;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.SqlTransformerSupport;
import biouml.standard.type.Structure;

public class StructureSqlTransformer extends SqlTransformerSupport<Structure>
{
    @Override
    public Structure create(ResultSet resultSet, Connection connection) throws Exception
    {
        Structure structure = new Structure(owner, resultSet.getString(1));
        structure.setFormat(resultSet.getString(3));
        String data = resultSet.getString(2);
        // Fix SDF headers
        int line2pos = data.indexOf( '\n' );
        if(line2pos >= 0 && line2pos < data.length()-1 && data.charAt( line2pos+1 ) == '\n')
            data = data.substring( 0, line2pos+1 )+"  "+structure.getName()+data.substring( line2pos+1 );
        structure.setData(data);

        return structure;
    }

    @Override
    public boolean init(SqlDataCollection<Structure> owner)
    {
        table = "structures";
        this.owner = owner;
        return true;
    }

    @Override
    public Class<Structure> getTemplateClass()
    {
        return Structure.class;
    }

    @Override
    public String getNameListQuery()
    {
        return "SELECT id FROM " + table + " ORDER BY id";
    }

    @Override
    public boolean isNameListSorted()
    {
        return true;
    }

    @Override
    public String getSelectQuery()
    {
        return "SELECT id, structure, type " + "FROM " + table;
    }

    @Override
    public String getElementQuery(String name)
    {
        if(!name.matches( "\\d+" ))
            return null;
        return super.getElementQuery( name );
    }
    
    @Override
    public void addInsertCommands(Statement statement, Structure de) throws Exception
    {
        throw new Exception("You can't add or remove elements from this module");
    }

    @Override
    public void addDeleteCommands(Statement statement, String name) throws Exception
    {
        throw new Exception("You can't add or remove elements from this module");
    }
}
