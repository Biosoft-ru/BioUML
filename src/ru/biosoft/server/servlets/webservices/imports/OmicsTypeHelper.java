package ru.biosoft.server.servlets.webservices.imports;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.generic.GenericDataCollection;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.util.CustomImageLoader;

public class OmicsTypeHelper
{
    private static final Logger log = Logger.getLogger( OmicsTypeHelper.class.getName() );
    public static String getOmicsTypeString(DataElementPath dePath)
    {
        if( dePath == null )
            return null;
        try
        {
            DataCollection<?> parent = dePath.getParentCollection();
            DataCollection<?> primaryParent = (DataCollection<?>)SecurityManager.runPrivileged( () -> {
                return DataCollectionUtils.fetchPrimaryCollectionPrivileged( parent );
            } );
            if( primaryParent instanceof GenericDataCollection )
            {
                GenericDataCollection genericParent = (GenericDataCollection)primaryParent;
                String omicsTypeStr = genericParent.getChildProperty( dePath.getName(), "omicsType" );
                return omicsTypeStr;
            }
        }
        catch( Exception e )
        {
            log.log( Level.SEVERE, "Can not get omicsType for " + dePath, e );
        }
        return null;
    }

    public static void setChildOmicsType(GenericDataCollection genericParent, DataElement element, OmicsType omicsType) throws Exception
    {
        boolean setTypeToElement = true;
        if( element instanceof DataCollection )
        {
            DataCollection<?> primaryElement = (DataCollection<?>)SecurityManager.runPrivileged( () -> {
                return DataCollectionUtils.fetchPrimaryCollectionPrivileged( (DataCollection<?>)element );
            } );
            if( primaryElement instanceof GenericDataCollection )
            {
                //we do not need to set omics type for folder
                setTypeToElement = false;
                for( ru.biosoft.access.core.DataElement childElement : primaryElement )
                    setChildOmicsType( (GenericDataCollection)primaryElement, childElement, omicsType );
            }
        }

        if( setTypeToElement )
        {
            genericParent.setChildProperty( element.getName(), "omicsType", omicsType.toString() );
            genericParent.setChildProperty( element.getName(), CustomImageLoader.DATA_COLLECTION_PROPERTY,
                    OmicsImageLoader.getImageLoaderForType( omicsType ).getName() );
        }
    }
}
