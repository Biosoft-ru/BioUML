package biouml.model.util;

import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import javax.annotation.Nonnull;
import one.util.streamex.StreamEx;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.Element;
import biouml.model.Module;
import biouml.standard.StandardModuleType;
import biouml.standard.type.DatabaseInfo;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.Referrer;

//moved from biouml.plugins.microarray.ReferencesHandler
public class ReferencesHandler
{
    public static Stream<String> idsForDataElement(DataElement kernel, Map<String, DatabaseInfo> databaseInfoMap, BioHub bioHub)
    {
        Builder<String> builder = Stream.builder();
        if( kernel instanceof Referrer )
        {
            Element[] references = convertToElements( ( (Referrer)kernel ).getDatabaseReferences());
            Map<Element, Element[]> results = null;
            if( null != bioHub )
            {
                results = bioHub.getReferences(references, null, null, 1, -1);
            }
            for( Element reference : references )
            {
                if( null != results && results.containsKey(reference) )
                {
                    Element[] resultReferences = results.get(reference);
                    for( DatabaseReference resultReference : convertToDatabaseReferences(resultReferences) )
                    {
                        builder.add(resultReference.getAc());
                    }
                }
                builder.add(convertToDatabaseReference(reference).getId());
            }
            if( null != databaseInfoMap && 0 < databaseInfoMap.size() )
            {
                if( null != bioHub )
                {
                    results = bioHub.getReferences(references, null, null, 1, -1);
                }
                if( null != results )
                {
                    for( Element reference : references )
                    {
                        if( results.containsKey(reference) )
                        {
                            Element[] resultReferences = results.get(reference);
                            for( DatabaseReference resultReference : convertToDatabaseReferences(resultReferences) )
                            {
                                builder.add(resultReference.getAc());
                            }
                        }
                    }
                }
            }
        }
        builder.add(kernel.getName());
        return builder.build();
    }

    public static void initDatabaseInfoMap(Module module, Map<String, DatabaseInfo> databaseInfoMap) throws Exception
    {
        databaseInfoMap.clear();
        DataCollection<DatabaseInfo> databaseInfo;
        DataCollection<?> metadata = (DataCollection<?>)module.get(Module.METADATA);
        if( metadata != null )
        {
            databaseInfo = (DataCollection<DatabaseInfo>)metadata.get(StandardModuleType.DATABASE_INFO);
            if( databaseInfo != null )
            {
                for( DatabaseInfo dbInfo : databaseInfo )
                {
                    databaseInfoMap.put( dbInfo.getTitle(), dbInfo );
                }
            }
        }
    }

    public static @Nonnull Element[] convertToElements(DatabaseReference[] reference)
    {
        if(reference == null)
            return new Element[0];
        return StreamEx.of( reference ).map( DatabaseReference::convertToElement ).toArray( Element[]::new );
    }

    public static DatabaseReference[] convertToDatabaseReferences(Element[] elements)
    {
        return StreamEx.of( elements ).map( ReferencesHandler::convertToDatabaseReference ).nonNull().toArray( DatabaseReference[]::new );
    }

    public static DatabaseReference convertToDatabaseReference(Element element)
    {
        try
        {
            return new DatabaseReference( element );
        }
        catch( IllegalArgumentException e )
        {
            return null;
        }
    }
}
