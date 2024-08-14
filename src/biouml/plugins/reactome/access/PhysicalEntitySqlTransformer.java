package biouml.plugins.reactome.access;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;

import com.developmentontheedge.beans.DynamicPropertySet;
import biouml.standard.type.Concept;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.sql.Query;
import ru.biosoft.access.sql.SqlDynamicProperty;
import ru.biosoft.access.sql.SqlUtil;

public abstract class PhysicalEntitySqlTransformer<T extends Concept> extends ReactomeObjectSqlTransformer<T>
{
    private static final Query SYNONYMS_QUERY = new Query("SELECT DISTINCT name,name_rank FROM PhysicalEntity_2_name "
            + "WHERE DB_ID=$id$ ORDER BY name_rank");
    
    @Override
    public boolean init(SqlDataCollection<T> owner)
    {
        //table = "PhysicalEntity";
        //idField = "DB_ID";
        table = "StableIdentifier";
        idField = "identifier";
        this.owner = owner;
        return true;
    }

    @Override
    public T create(ResultSet resultSet, Connection connection) throws Exception
    {
        T concept = super.create(resultSet, connection);
        concept.setType(resultSet.getString(2));

        String reactomeId = getReactomeId(concept);
        
        // retrieve names references
        
        List<String> names = SqlUtil.queryStrings( connection, SYNONYMS_QUERY.str( reactomeId ) );

        concept.setComment(getSummation(reactomeId, "PhysicalEntity_2_summation", connection));

        if( names.size() > 0 )
            concept.setSynonyms(String.join("; ", names));

        DynamicPropertySet dps = concept.getAttributes();
        dps.add(new SqlDynamicProperty(COMPARTMENT_PD, getConnectionHolder(), getCompartmentQuery(reactomeId, "PhysicalEntity_2_compartment"), true));

        //Literature references
        concept.setLiteratureReferences(getLiteratureReferences(reactomeId, "PhysicalEntity_2_literatureReference", connection));

        return concept;
    }

    protected String getEntitySpeciesQuery(String name, String specDB)
    {
        return "SELECT DISTINCT _displayName FROM " + databaseObjectTable + " dbt INNER JOIN " + specDB
                + " sdb ON(dbt.DB_ID=sdb.species) WHERE sdb.DB_ID='" + name + "'";
    }
}
