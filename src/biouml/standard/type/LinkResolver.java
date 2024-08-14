package biouml.standard.type;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import biouml.model.Module;
import biouml.standard.StandardModuleType;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.util.LazyValue;


public class LinkResolver
{
    public static final DataElementPath MIRIAM_RESOURCE = DataElementPath.create("databases/Utils/MIRIAM");

    private static Logger log = Logger.getLogger(LinkResolver.class.getName());
    private DataCollection<DatabaseInfo> miriamCollection;
    private final LazyValue<Map<String, String>> uriList = new LazyValue<Map<String,String>>("MIRIAM uriList")
    {
        @Override
        protected Map<String, String> doGet() throws Exception
        {
            if(miriamCollection != null)
            {
                return StreamEx.of( miriamCollection.stream() )
                        .mapToEntry( info -> info.getAttributes().getValue( "uris" ), DatabaseInfo::getName )
                    .selectKeys( String[].class ).flatMapKeys( Arrays::stream ).toMap();
            }
            return Collections.emptyMap();
        }
    };


    public LinkResolver()
    {
        try
        {
            miriamCollection = MIRIAM_RESOURCE.getDataCollection(DatabaseInfo.class);
        }
        catch( RepositoryException e )
        {
        }
    }

    public String getQueryById(DataElement de, DatabaseReference ref)
    {
        String result = "";
        String databaseName = ref.getDatabaseName();

        try
        {
            DatabaseInfo info = null;
            DataCollection<DatabaseInfo> databaseInfos = getDatabaseInfoCollection(de);
            if( databaseInfos != null )
            {
                info = databaseInfos.get(databaseName);
                if(info == null)
                {
                    info = databaseInfos.stream().filter( dbInfo -> dbInfo.getTitle().equals( databaseName ) ).findFirst().orElse( null );
                }
                if( miriamCollection != null && info != null && info.getDatabaseReferences() != null )
                {
                    for( DatabaseReference reference : info.getDatabaseReferences() )
                    {
                        if( miriamCollection.contains(reference.getId()) )
                        {
                            info = miriamCollection.get(reference.getId());
                            break;
                        }
                    }
                }
            }

            if( info == null && miriamCollection != null )
            {
                info = miriamCollection.get(databaseName);
                if( info == null )
                    info = getDatabaseInfoByUri(databaseName);
            }

            if( info != null )
            {
                result = info.getQueryById();
                String id = ref.getId();
                result = result.replace("$id$", id);
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "LinkResolver: can not get query by id, " + e);
        }
        return result;
    }

    public String getDatabaseTitle(DataElement de, DatabaseReference ref)
    {
        String databaseName = ref.getDatabaseName();
        DatabaseInfo info = null;
        try
        {
            DataCollection databaseInfos = getDatabaseInfoCollection(de);
            if( databaseInfos != null && databaseInfos.contains(databaseName) )
            {
                info = (DatabaseInfo)databaseInfos.get(databaseName);
            }
            else if( miriamCollection != null )
            {
                info = miriamCollection.get(databaseName);
                if( info == null )
                    info = getDatabaseInfoByUri(databaseName);
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can't process database reference: " + ExceptionRegistry.log( e ));
        }

        if( info != null )
            return info.getTitle();
        else
            return databaseName;
    }

    public String getQueryById(DataElement de, String publicationID)
    {
        String result = "";
        try
        {
            if( miriamCollection != null )
            {
                DataCollection literature = getLiteratureCollection(de);
                String pmid = null;

                if( literature != null )
                    pmid = ( (Publication)literature.get(publicationID) ).getPubMedId();
                else
                    pmid = publicationID;

                if( pmid != null && !pmid.equals("") && !pmid.equals("0") )
                {
                    DatabaseInfo dbInfo = miriamCollection.get("MIR:00000015");
                    if( dbInfo != null )
                    {
                        result = dbInfo.getQueryById();
                        result = result.replace("$id$", pmid);
                    }
                }
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not process publication " + publicationID);
        }
        return result;
    }

    public String getReference(DataElement de, String publicationID)
    {
        String result = publicationID;
        try
        {
            DataCollection literature = getLiteratureCollection(de);
            if( literature != null )
                result = ( (Publication)literature.get(publicationID) ).getReference();
            else
                result = publicationID;
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not process publication " + publicationID);
        }

        return result;
    }

    protected DataCollection<DatabaseInfo> getDatabaseInfoCollection(DataElement de) throws Exception
    {
        DataElementPath modulePath = Module.optModulePath( de );
        if( modulePath == null )
            return null;
        return modulePath.getChildPath( Module.METADATA, StandardModuleType.DATABASE_INFO ).optDataCollection( DatabaseInfo.class );
    }

    protected DataCollection getLiteratureCollection(DataElement de) throws Exception
    {
        DataCollection literature = null;
        Module module = Module.optModule(de);
        if( module != null && module.get(Module.DATA) != null )
        {
            DataCollection data = (DataCollection)module.get(Module.DATA);
            literature = (DataCollection)data.get("literature");
        }
        return literature;
    }

    protected DatabaseInfo getDatabaseInfoByUri(String uri) throws Exception
    {
        if( miriamCollection != null && uriList.get().containsKey(uri) )
            return miriamCollection.get(uriList.get().get(uri));
        return null;
    }

    //Workaround to get element by complete name in getDBRefList method
    public DataElement getDataElement(String completeName)
    {
        return CollectionFactory.getDataElement(completeName);
    }

    public Module getModule(DataElement de)
    {
        return Module.optModule(de);
    }
}
