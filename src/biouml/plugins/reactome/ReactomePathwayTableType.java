package biouml.plugins.reactome;

import biouml.plugins.reactome.access.ReactomeDiagramRepository;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.security.NetworkDataCollection;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.analysis.type.PathwayType;
import ru.biosoft.journal.ProjectUtils;

public class ReactomePathwayTableType extends PathwayType
{
    @Override
    public String getSource()
    {
        return "Reactome";
    }

    @Override
    public int getIdScore(String id)
    {
        return SCORE_NOT_THIS_TYPE;
    }

    @Override
    public DataElementPath getPath(String id)
    {
        DataElementPath diagramsPath = ProjectUtils.getPreferredDatabasePath( "Reactome" ).getChildPath( "Diagrams" );
        DataCollection<?> primaryDiagrams = getPrimaryCollection( diagramsPath );
        String[] pathParts = getAdditionalPathParts( id, primaryDiagrams );
        return diagramsPath.getChildPath( pathParts );
    }

    private static DataCollection<?> getPrimaryCollection(DataElementPath collectionPath)
    {
        DataCollection<?> dc = collectionPath.optDataCollection();
        try
        {
            if( dc != null && dc instanceof NetworkDataCollection )
            {
                return (DataCollection<?>)SecurityManager.runPrivileged( () -> {
                    return DataCollectionUtils.fetchPrimaryCollectionPrivileged( dc );
                } );
            }
        }
        catch( Exception e )
        {
        }
        return dc;
    }

    private static String[] getAdditionalPathParts(String id, DataCollection<?> dc)
    {
        if( dc != null && dc instanceof ReactomeDiagramRepository )
            return ( (ReactomeDiagramRepository)dc ).getInnerDiagramPathParts( id );
        else
            return new String[] {id};
    }
}
