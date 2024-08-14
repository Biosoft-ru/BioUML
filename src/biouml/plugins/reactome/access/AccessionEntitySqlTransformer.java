package biouml.plugins.reactome.access;

import biouml.standard.type.Substance;
import ru.biosoft.access.SqlDataCollection;

public class AccessionEntitySqlTransformer extends GenomeEntitySqlTransformer
{
    @Override
    public boolean init(SqlDataCollection<Substance> owner)
    {
        //table = "EntityWithAccessionedSequence";
        //idField = "DB_ID";
        table = "StableIdentifier";
        idField = "identifier";
        this.owner = owner;
        return true;
    }

    @Override
    public String getNameListQuery()
    {
        return "SELECT " + idField + " FROM " + table + " si INNER JOIN " + databaseObjectTable
                + " dbt ON(dbt.stableIdentifier=si.DB_ID) WHERE dbt._class='EntityWithAccessionedSequence' ORDER BY " + idField;
    }
    
    @Override
    public String getCountQuery()
    {
        return "SELECT count(" + idField + ") FROM " + table + " si INNER JOIN " + databaseObjectTable
        + " dbt ON(dbt.stableIdentifier=si.DB_ID) WHERE dbt._class='EntityWithAccessionedSequence'";
    }
    
    @Override
    protected String getReactomeObjectClass()
    {
        return "EntityWithAccessionedSequence";
    }
}
