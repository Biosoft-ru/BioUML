package biouml.plugins.proteinmodel;

import one.util.streamex.StreamEx;

import biouml.standard.type.DatabaseReference;

public class ProteinModelUtils
{
    static StreamEx<DatabaseReference> createReferences(String idsStr, String databaseName)
    {
        if( !idsStr.isEmpty() )
        {
            return StreamEx.split( idsStr, ";" ).distinct().map( id -> new DatabaseReference( databaseName, id ) );
        }
        return StreamEx.empty();
    }

    static String getRNAExistsCondition()
    {
        return "refseq_mrna_ids!=\"\"";
    }
}
