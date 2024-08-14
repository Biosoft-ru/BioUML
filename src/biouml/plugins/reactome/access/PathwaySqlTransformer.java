package biouml.plugins.reactome.access;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.plugins.reactome.ReactomeDiagramReference;
import ru.biosoft.access.SqlTransformerSupport;

/**
 * @author lan
 *
 */
public class PathwaySqlTransformer extends SqlTransformerSupport<ReactomeDiagramReference>
{

    @Override
    public Class<ReactomeDiagramReference> getTemplateClass()
    {
        return ReactomeDiagramReference.class;
    }

    @Override
    public ReactomeDiagramReference create(ResultSet resultSet, Connection connection) throws Exception
    {
        ReactomeDiagramReference reference = new ReactomeDiagramReference( owner, resultSet.getString( 1 ) );
        reference.setTitle(resultSet.getString(2));
        reference.setComment("Diagram ID: "+resultSet.getString(3));
        reference.getAttributes().add(new DynamicProperty("InnerID", String.class, reference.getName()));
        return reference;
    }

    @Override
    public String getCountQuery()
    {
        return "SELECT COUNT(1) FROM DatabaseObject do JOIN PathwayDiagram_2_representedPathway p2r ON do.DB_ID=p2r.representedPathway"
                + " where _class IN ('Pathway')";
    }

    @Override
    public String getSelectQuery()
    {
        return "SELECT do.DB_ID,_displayName,p2r.DB_ID FROM DatabaseObject do"
                + " JOIN PathwayDiagram_2_representedPathway p2r ON do.DB_ID=p2r.representedPathway where _class IN ('Pathway')";
    }

    @Override
    public String getNameListQuery()
    {
        return "SELECT do.DB_ID FROM DatabaseObject do JOIN PathwayDiagram_2_representedPathway p2r ON do.DB_ID=p2r.representedPathway"
                + " where _class IN ('Pathway')";
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
        return getSelectQuery()+" AND do.DB_ID="+id;
    }

    @Override
    public void addInsertCommands(Statement statement, ReactomeDiagramReference de) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addUpdateCommands(Statement statement, ReactomeDiagramReference de) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addDeleteCommands(Statement statement, String name) throws Exception
    {
        throw new UnsupportedOperationException();
    }
}
