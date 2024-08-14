package biouml.plugins.reactome;

/**
 * @author anna
 *
 */
public class ReactomeSqlUtils
{
    public final static String databaseObjectTable = "DatabaseObject";
    
    public static String getCollectionNameByClass(String className)
    {
        if( "SimpleEntity".equals( className ) )
        {
            return "SmallMolecule";
        }
        else if( "EntityWithAccessionedSequence".equals( className ) )
        {
            return "EntityWithAccession";
        }
        else
        {
            return className;
        }

    }
}
