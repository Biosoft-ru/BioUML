package biouml.plugins.reactome.access;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.developmentontheedge.beans.DynamicPropertySet;

import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.sql.SqlDynamicProperty;
import biouml.standard.type.Molecule;
import biouml.standard.type.Substance;

public class PolymerSqlTransformer extends PhysicalEntitySqlTransformer<Molecule>
{
    @Override
    public boolean init(SqlDataCollection<Molecule> owner)
    {
        table = "StableIdentifier";
        idField = "identifier";
        this.owner = owner;
        return true;
    }

    @Override
    public Molecule create(ResultSet resultSet, Connection connection) throws Exception
    {
        Molecule mol = super.create(resultSet, connection);
        DynamicPropertySet dps = mol.getAttributes();
        dps.add(new SqlDynamicProperty(ORGANISM_PD, getConnectionHolder(), getEntitySpeciesQuery( getReactomeId(mol), "Polymer_2_species"), true));
        return mol;
    }
    
    @Override
    public String getNameListQuery()
    {
        return "SELECT " + idField + " FROM " + table + " si INNER JOIN " + databaseObjectTable
        + " dbt ON(dbt.stableIdentifier=si.DB_ID) WHERE dbt._class='Polymer' ORDER BY " + idField;

    }
    
    @Override
    public String getCountQuery()
    {
        return "SELECT count(" + idField + ") FROM " + table + " si INNER JOIN " + databaseObjectTable
        + " dbt ON(dbt.stableIdentifier=si.DB_ID) WHERE dbt._class='Polymer'";
    }

    @Override
    protected Molecule createElement(ResultSet resultSet, Connection connection) throws SQLException
    {
        return new Substance(owner, resultSet.getString(1));
    }

    @Override
    public Class<Molecule> getTemplateClass()
    {
        return Molecule.class;
    }
    
    @Override
    protected String getReactomeObjectClass()
    {
        return "Polymer";
    }
    
}
