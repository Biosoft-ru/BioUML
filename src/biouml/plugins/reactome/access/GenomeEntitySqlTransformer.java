package biouml.plugins.reactome.access;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import biouml.standard.type.Substance;

import com.developmentontheedge.beans.DynamicPropertySet;

import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.sql.SqlDynamicProperty;

public class GenomeEntitySqlTransformer extends PhysicalEntitySqlTransformer<Substance>
{
    @Override
    public boolean init(SqlDataCollection<Substance> owner)
    {
        //table = "GenomeEncodedEntity";
        //idField = "DB_ID";
        table = "StableIdentifier";
        idField = "identifier";
        this.owner = owner;
        return true;
    }

    @Override
    public String getNameListQuery()
    {
        //return "SELECT " + idField + " FROM " + databaseObjectTable + " WHERE _class='GenomeEncodedEntity' ORDER BY " + idField;
        return "SELECT " + idField + " FROM " + table + " si INNER JOIN " + databaseObjectTable
        + " dbt ON(dbt.stableIdentifier=si.DB_ID) WHERE dbt._class='GenomeEncodedEntity' ORDER BY " + idField;

    }
    
    @Override
    public String getCountQuery()
    {
        return "SELECT count(" + idField + ") FROM " + table + " si INNER JOIN " + databaseObjectTable
        + " dbt ON(dbt.stableIdentifier=si.DB_ID) WHERE dbt._class='GenomeEncodedEntity'";
    }

    @Override
    public Substance create(ResultSet resultSet, Connection connection) throws Exception
    {
        Substance substance = super.create(resultSet, connection);
        DynamicPropertySet dps = substance.getAttributes();
        dps.add(new SqlDynamicProperty(ORGANISM_PD, getConnectionHolder(), getEntitySpeciesQuery( getReactomeId(substance), "GenomeEncodedEntity"), true));
        return substance;
    }

    @Override
    protected Substance createElement(ResultSet resultSet, Connection connection) throws SQLException
    {
        return new Substance(owner, resultSet.getString(1));
    }

    @Override
    public Class<Substance> getTemplateClass()
    {
        return Substance.class;
    }
    
    @Override
    protected String getReactomeObjectClass()
    {
        return "GenomeEncodedEntity";
    }
}
