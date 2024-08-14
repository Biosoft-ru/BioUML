package biouml.plugins.reactome.access;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.sql.SqlDynamicProperty;
import ru.biosoft.access.sql.SqlUtil;
import biouml.model.Module;
import biouml.standard.type.Complex;

import com.developmentontheedge.beans.DynamicPropertySet;

public class EntitySetSqlTransformer extends PhysicalEntitySqlTransformer<Complex>
{
    @Override
    public boolean init(SqlDataCollection<Complex> owner)
    {
        //table = "EntitySet";
        //idField = "DB_ID";
        table = "StableIdentifier";
        idField = "identifier";
        this.owner = owner;
        return true;
    }

    @Override
    public Complex create(ResultSet resultSet, Connection connection) throws Exception
    {
        Complex complex = super.create(resultSet, connection);
        DynamicPropertySet dps = complex.getAttributes();
        dps.add(new SqlDynamicProperty(ORGANISM_PD, getConnectionHolder(), getEntitySpeciesQuery( getReactomeId(complex), "EntitySet_2_species"), true));
        complex.setComponents(getComponents(complex, connection));
        return complex;
    }

    @Override
    protected Complex createElement(ResultSet resultSet, Connection connection) throws SQLException
    {
        return new Complex(owner, resultSet.getString(1));
    }

    @Override
    public Class<Complex> getTemplateClass()
    {
        return Complex.class;
    }

    @Override
    public String getNameListQuery()
    {
        return "SELECT " + idField + " FROM " + table + " si INNER JOIN " + databaseObjectTable
                + " dbt ON(dbt.stableIdentifier=si.DB_ID) WHERE dbt._class='EntitySet' ORDER BY " + idField;
    }

    @Override
    public String getCountQuery()
    {
        return "SELECT count(" + idField + ") FROM " + table + " si INNER JOIN " + databaseObjectTable
                + " dbt ON(dbt.stableIdentifier=si.DB_ID) WHERE dbt._class='EntitySet'";
    }

    protected String[] getComponents(@Nonnull Complex parent, Connection connection)
    {
        String memberQuery = "SELECT DISTINCT identifier, _displayName,_class FROM " + databaseObjectTable
                + " dbt INNER JOIN EntitySet_2_hasMember es ON(es.hasMember=dbt.DB_ID) JOIN StableIdentifier si ON (si.DB_ID=dbt.stableIdentifier) WHERE es.DB_ID='" + getReactomeId(parent)
                + "' ORDER BY hasMember_rank";
        DataElementPath modulePath = Module.getModulePath(parent);
        DataElementPath dataPath = modulePath.getChildPath(Module.DATA);
        return SqlUtil
                .stream( connection, memberQuery,
                        rs -> dataPath.getChildPath( getCollectionNameByClass( rs.getString( 3 ) ), rs.getString( 1 ) ) )
                .filter( DataElementPath::exists ).map( path -> path.getPathDifference( modulePath ) ).toArray( String[]::new );
    }
    
    @Override
    protected String getReactomeObjectClass()
    {
        return "EntitySet";
    }
}
