package biouml.plugins.ensembl.biohub;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExternalDBRestriction
{
    private Map<String, Item> restrictionsBySpecies = new HashMap<>();
    private Item defaultRestriction = new Item();
    
    public ExternalDBRestriction setSpeciesDBName(String species, String dbName)
    {
        restrictionsBySpecies.computeIfAbsent( species, k->new Item() ).dbName = dbName;
        return this;
    }
    
    public ExternalDBRestriction setSpeciesDBType(String species, String type)
    {
        restrictionsBySpecies.computeIfAbsent( species, k->new Item() ).type = type;
        return this;
    }
    
    public ExternalDBRestriction setDefaultDBName(String dbName)
    {
        defaultRestriction.dbName = dbName;
        return this;
    }
    public ExternalDBRestriction setDefaultDBType(String type)
    {
        defaultRestriction.type = type;
        return this;
    }
    
    private static final String DB_ID_QUERY = "SELECT external_db_id FROM external_db";
    public String getExternalDBQuery(String species)
    {
        Item item = restrictionsBySpecies.getOrDefault( species, defaultRestriction );
        String restriction = item.getRestriction();
        if(restriction.isEmpty())
            return DB_ID_QUERY;
        return DB_ID_QUERY + " WHERE " + restriction;
    }
    
    private static class Item
    {
        private String dbName;
        private String type;
        
        public String getRestriction()
        {
            List<String> l = new ArrayList<>();
            if(dbName != null)
            {
                String e;
                if(dbName.contains( "%" ))
                    l.add("db_name LIKE '" + dbName + "'");
                else
                    l.add( "db_name IN ('" + dbName.replaceAll( ",", "','" ) + "')" );
            }
            if(type != null)
                l.add( "type='" + type + "'" );
            return String.join( " AND ", l );
        }
    }
    
    @Override
    public String toString()
    {
        String res = defaultRestriction.dbName;
        if(res != null)
            return res;
        res = defaultRestriction.type;
        if(res != null)
            return res;
        return "";
    }
}
