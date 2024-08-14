package biouml.plugins.reactome.access;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.standard.type.Compartment;
import ru.biosoft.access.SqlTransformerSupport;

/**
 * @author lan
 *
 */
public class CompartmentSqlTransformer extends SqlTransformerSupport<Compartment>
{

    @Override
    public Class<Compartment> getTemplateClass()
    {
        return Compartment.class;
    }

    @Override
    public Compartment create(ResultSet resultSet, Connection connection) throws Exception
    {
        Compartment compartment = new Compartment(owner, resultSet.getString("DB_ID"));
        compartment.setTitle(resultSet.getString("_displayName"));
        compartment.setComment("Type: "+resultSet.getString("_class"));
        compartment.getAttributes().add(new DynamicProperty("InnerID", String.class, compartment.getName()));
        return compartment;
    }

    @Override
    public String getCountQuery()
    {
        return "SELECT COUNT(1) FROM DatabaseObject where _class IN ('EntityCompartment', 'GO_CellularComponent')";
    }

    @Override
    public String getSelectQuery()
    {
        return "SELECT * FROM DatabaseObject where _class IN ('EntityCompartment', 'GO_CellularComponent')";
    }

    @Override
    public String getNameListQuery()
    {
        return "SELECT DB_ID FROM DatabaseObject where _class IN ('EntityCompartment', 'GO_CellularComponent')";
    }

    @Override
    public String getElementExistsQuery(String name)
    {
        return getElementQuery(name);
    }

    @Override
    public String getElementQuery(String name)
    {
        int id;
        try
        {
            id = Integer.parseInt(name);
        }
        catch( NumberFormatException e )
        {
            return null;
        }
        return getSelectQuery()+" AND DB_ID="+id;
    }

    @Override
    public void addInsertCommands(Statement statement, Compartment de) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addUpdateCommands(Statement statement, Compartment de) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addDeleteCommands(Statement statement, String name) throws Exception
    {
        throw new UnsupportedOperationException();
    }
}
