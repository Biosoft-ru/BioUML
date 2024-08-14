package biouml.plugins.reactome.access;


public class DefinedSetSqlTransformer extends EntitySetSqlTransformer
{
    @Override
    public String getNameListQuery()
    {
        return "SELECT " + idField + " FROM " + table + " si INNER JOIN " + databaseObjectTable
        + " dbt ON(dbt.stableIdentifier=si.DB_ID) WHERE dbt._class='DefinedSet' ORDER BY " + idField;
    }
    
    @Override
    public String getCountQuery()
    {
        return "SELECT count(" + idField + ") FROM " + table + " si INNER JOIN " + databaseObjectTable
        + " dbt ON(dbt.stableIdentifier=si.DB_ID) WHERE dbt._class='DefinedSet'";
    }
    
    @Override
    protected String getReactomeObjectClass()
    {
        return "DefinedSet";
    }
}
