package ru.biosoft.server.servlets.webservices.providers;

import java.io.IOException;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.generic.GenericDataCollection;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.server.servlets.webservices.imports.OmicsType;
import ru.biosoft.server.servlets.webservices.imports.OmicsTypeHelper;
import ru.biosoft.table.TableDataCollection;

public class OmicsTypeProvider extends WebJSONProviderSupport
{
    @Override
    public void process(BiosoftWebRequest arguments, JSONResponse response) throws Exception
    {
        String action = arguments.getAction();
        switch( action )
        {
            case "detect":
                processDetectOmicsType( arguments, response );
                break;
            case "set":
                processSetOmicsType( arguments, response );
                break;
            default:
                throw new WebException( "EX_QUERY_PARAM_INVALID_VALUE", "action" );
        }
    }

    private void processDetectOmicsType(BiosoftWebRequest arguments, JSONResponse resp) throws WebException, IOException
    {
        DataElementPath dePath = arguments.getDataElementPath();
        DataElement de = dePath.optDataElement();
        if( de == null )
            throw new WebException( "EX_QUERY_NO_ELEMENT", dePath );
        OmicsType omicsType = detectOmicsType( de );
        resp.sendString( omicsType == null ? null : omicsType.toString() );
    }

    private OmicsType detectOmicsType(@Nonnull ru.biosoft.access.core.DataElement de)
    {
        String omicsTypeStr = OmicsTypeHelper.getOmicsTypeString( de.getCompletePath() );
        if( omicsTypeStr != null )
            return OmicsType.valueOf( omicsTypeStr );

        OmicsType omicsType = null;
        if( de instanceof DataCollection )
        {
            ReferenceType referenceType = ReferenceTypeRegistry.getReferenceType( (DataCollection<?>)de );
            if( referenceType != null )
                omicsType = OmicsType.getByReferenceType( referenceType );
        }
        if( omicsType == null )
        {
            if( de instanceof TableDataCollection )
                omicsType = OmicsType.Transcriptomics;
            //Dirty code to remove dependency of server.servlets on bsa
            else
            {
                try
                {
                    Class<?> trackClass = Class.forName( "ru.biosoft.bsa.Track" );
                    if( trackClass.isAssignableFrom( de.getClass() ) )
                        omicsType = OmicsType.Genomics;
                }
                catch( ClassNotFoundException e )
                {
                }
            }
        }
        return omicsType;
    }

    private void processSetOmicsType(BiosoftWebRequest args, JSONResponse resp) throws WebException, IOException
    {
        DataElementPath dePath = args.getDataElementPath();
        DataElement de = dePath.optDataElement();
        if( de == null )
            throw new WebException( "EX_QUERY_NO_ELEMENT", dePath );
        DataCollection<?> parentDC = dePath.getParentCollection();
        String omicsTypeStr = args.get( "omicsType" );
        OmicsType omicsType = omicsTypeStr == null ? null : OmicsType.valueOf( omicsTypeStr );
        if( omicsType != null )
        {
            try
            {
                DataCollection<?> primaryParent = (DataCollection<?>)SecurityManager.runPrivileged( () -> {
                    return DataCollectionUtils.fetchPrimaryCollectionPrivileged( parentDC );
                } );
                if( primaryParent instanceof GenericDataCollection )
                {
                    OmicsTypeHelper.setChildOmicsType( (GenericDataCollection)primaryParent, de, omicsType );
                }
            }
            catch( Exception e )
            {
            }
        }
        resp.sendString( "ok" );
    }
}
